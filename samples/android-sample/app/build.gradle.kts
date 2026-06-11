// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The octet-sdk needs a license key at runtime. Read it from
// local.properties (gitignored) so the live key never lands in source
// control. Add a line `octet.licenseKey=<your key>` to
// samples/android-sample/local.properties before running on a device.
val octetLicenseKey: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("octet.licenseKey", "")

android {
    namespace = "com.octetproof.octetpolicysample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.octetproof.octetpolicysample"
        minSdk = 30   // the octet-sdk AAR (via octetpolicy) requires minSdk 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "OCTET_LICENSE_KEY", "\"$octetLicenseKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // The umbrella artifact. Pulls in every predicate plus PolicyResult
    // and (transitively) the octet-sdk API (Octet, OctetLoc, OctetRegion).
    implementation("com.octetproof:octetpolicy:1.0.0")

    // Compose + AndroidX.
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // OpenStreetMap map view (no API key) for the "where am I" panel.
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // OS location for the map marker — the OctetSDK is a containment
    // oracle and does not expose raw coordinates.
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
