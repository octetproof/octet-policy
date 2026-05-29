// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicysample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.octetproof.octetpolicy.PolicyPackage
import com.octetproof.octetpolicy.PolicyReasonCode
import com.octetproof.octetpolicy.PolicyResult
import com.octetproof.octetpolicy.isOfacComprehensive
import com.octetproof.octetpolicy.isSingapore
import com.octetproof.octetpolicy.isUS
import com.octetproof.octetpolicy.isUSState
import com.octetproof.sdk.api.Octet
import com.octetproof.sdk.api.OctetConfig
import com.octetproof.sdk.api.OctetLoc
import com.octetproof.sdk.api.OctetRegion
import com.octetproof.sdk.api.OctetVerdict

import kotlinx.coroutines.launch

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// OctetPolicy Android sample — interactive demo on a real device.
//
//   • a map of where the OS thinks the device is (osmdroid + the OS
//     fused location — the OctetSDK is a containment oracle and does
//     NOT expose raw coordinates)
//   • an "Evaluate" button that mints a FRESH location proof and runs
//     every v1 predicate (tap it after the sensors warm up, otherwise
//     a cold start reads back VERDICT_INDETERMINATE)
//   • a country picker that asks the SDK directly,
//     loc.isWithin(OctetRegion.country(code)), for any ISO country
//
// The license key is read from local.properties via BuildConfig (see
// app/build.gradle.kts) and is never committed.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // osmdroid needs a user-agent before it fetches tiles.
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PolicySampleScreen()
                }
            }
        }
    }
}

private sealed interface SdkState {
    data object Initializing : SdkState
    data class Ready(val loc: OctetLoc) : SdkState
    data class Failed(val message: String) : SdkState
}

@Composable
private fun PolicySampleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissionResolved by remember { mutableStateOf(false) }
    var sdkState by remember { mutableStateOf<SdkState>(SdkState.Initializing) }
    var deviceLatLon by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var results by remember { mutableStateOf<List<Pair<String, PolicyResult>>?>(null) }
    var evaluating by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionResolved = true }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    // Once permission is resolved: start the SDK (once) and fetch the OS
    // location for the map.
    LaunchedEffect(permissionResolved) {
        if (!permissionResolved) return@LaunchedEffect
        fetchDeviceLocation(context) { lat, lon -> deviceLatLon = lat to lon }
        sdkState = try {
            val key = BuildConfig.OCTET_LICENSE_KEY
            require(key.isNotBlank()) {
                "No license key. Add `octet.licenseKey=<your key>` to " +
                    "samples/android-sample/local.properties and rebuild."
            }
            SdkState.Ready(Octet.start(context, OctetConfig(licenseKey = key)).loc)
        } catch (e: Exception) {
            SdkState.Failed(e.message ?: e.toString())
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("OctetPolicy", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Sample — v${PolicyPackage.VERSION}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))
        MapPanel(deviceLatLon)
        Text(
            deviceLatLon?.let { (lat, lon) -> "OS location: %.4f, %.4f".format(lat, lon) }
                ?: "Locating… (OS fused location)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Predicate evaluation ────────────────────────────────────
        Text("Predicates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Each tap mints a fresh proof and re-runs every predicate. " +
                "Give the sensors a few seconds before the first tap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (val s = sdkState) {
            is SdkState.Initializing -> LoadingRow("Starting SDK…")
            is SdkState.Failed -> Text(
                "⚠️ ${s.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828),
            )
            is SdkState.Ready -> {
                Button(
                    onClick = {
                        if (evaluating) return@Button
                        scope.launch {
                            evaluating = true
                            results = listOf(
                                "isSingapore(loc)" to isSingapore(s.loc),
                                "isUS(loc)" to isUS(s.loc),
                                "isUSState(loc, [CA, NY, TX])" to isUSState(s.loc, listOf("CA", "NY", "TX")),
                                "isOfacComprehensive(loc)" to isOfacComprehensive(s.loc),
                            )
                            evaluating = false
                        }
                    },
                    enabled = !evaluating,
                ) {
                    Text(if (evaluating) "Evaluating…" else "Generate proof & evaluate")
                }

                Spacer(modifier = Modifier.height(8.dp))
                when {
                    evaluating && results == null -> LoadingRow("Minting proof…")
                    results == null -> Text(
                        "No results yet — tap the button above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> results!!.forEach { (label, r) -> PredicateCase(label, r) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                CountryProbe(loc = s.loc)
            }
        }
    }
}

@Composable
private fun MapPanel(latLon: Pair<Double, Double>?) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(2.0)
                onResume()
            }
        },
        update = { map ->
            latLon?.let { (lat, lon) ->
                val point = GeoPoint(lat, lon)
                map.controller.setZoom(13.0)
                map.controller.setCenter(point)
                map.overlays.removeAll { it is Marker }
                val marker = Marker(map).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Device (OS location)"
                }
                map.overlays.add(marker)
                map.invalidate()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryProbe(loc: OctetLoc) {
    val scope = rememberCoroutineScope()
    val countries = remember {
        Locale.getISOCountries()
            .map { code -> code to Locale("", code).displayCountry }
            .sortedBy { it.second }
    }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<String, String>?>(null) }
    var verdict by remember { mutableStateOf<OctetVerdict.Result?>(null) }
    var probing by remember { mutableStateOf(false) }

    Text("Check any country", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        "Asks the SDK directly: loc.isWithin(country). Not a policy predicate — just the raw containment answer.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected?.let { "${it.second} (${it.first})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Country") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            countries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text("${entry.second} (${entry.first})") },
                    onClick = {
                        expanded = false
                        selected = entry
                        verdict = null
                        scope.launch {
                            probing = true
                            verdict = try {
                                loc.isWithin(OctetRegion.country(entry.first)).result
                            } catch (e: Exception) {
                                null
                            }
                            probing = false
                        }
                    },
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    when {
        selected == null -> Text(
            "Pick a country to see whether the device is inside it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        probing -> LoadingRow("Checking ${selected!!.second}…")
        else -> {
            val (label, color) = when (verdict) {
                OctetVerdict.Result.YES -> "✓ inside ${selected!!.second}" to Color(0xFF2E7D32)
                OctetVerdict.Result.NO -> "✗ outside ${selected!!.second}" to Color(0xFFC62828)
                else -> "? can't tell (indeterminate)" to Color(0xFF1976D2)
            }
            Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PredicateCase(label: String, result: PolicyResult) {
    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row {
                Text(
                    if (result.match) "✓ match" else "✗ no match",
                    color = if (result.match) Color(0xFF2E7D32) else Color(0xFFC62828),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "reason: ${result.reason.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = reasonColor(result.reason),
                )
            }
            Text(
                "country=${result.country ?: "null"}  state=${result.state ?: "null"}  confidence=${result.confidence.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun reasonColor(reason: PolicyReasonCode): Color = when (reason) {
    PolicyReasonCode.OK -> Color(0xFF455A64)
    PolicyReasonCode.INPUT_INVALID -> Color(0xFFF57C00)
    else -> Color(0xFF1976D2)
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, onResult: (Double, Double) -> Unit) {
    val fused = LocationServices.getFusedLocationProviderClient(context)
    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) onResult(location.latitude, location.longitude)
        }
}
