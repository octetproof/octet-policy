// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import SwiftUI
import MapKit
import CoreLocation
import OctetPolicy

// ─────────────────────────────────────────────────────────────────
// OctetPolicy iOS sample — interactive demo on a real device.
//
//   • a map of where the OS thinks the device is (MapKit + CoreLocation
//     — the OctetSDK is a containment oracle and does NOT expose raw
//     coordinates)
//   • a "Generate proof & evaluate" button that mints a FRESH proof and
//     runs every v1 predicate (tap after the sensors warm up, otherwise
//     a cold start reads back .verdictIndeterminate)
//   • a country picker that asks the SDK directly,
//     loc.isWithin(region: .country(isoCode:)), for any ISO country
//
// The license key comes from LocalConfig.swift (gitignored — copy
// LocalConfig.swift.example). Mirrors the Android sample.

/// Thin CoreLocation wrapper used only to center the map. The OctetSDK
/// deliberately does not surface coordinates, so the map relies on the
/// OS location instead.
final class LocationProvider: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var coordinate: CLLocationCoordinate2D?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    func start() {
        manager.requestWhenInUseAuthorization()
        manager.requestLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let last = locations.last { coordinate = last.coordinate }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways: manager.requestLocation()
        default: break
        }
    }
}

struct ContentView: View {
    @StateObject private var location = LocationProvider()
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 20, longitude: 0),
        span: MKCoordinateSpan(latitudeDelta: 120, longitudeDelta: 120))

    @State private var loc: OctetLoc?
    @State private var sdkError: String?
    @State private var results: [(label: String, result: PolicyResult)] = []
    @State private var didEvaluate = false
    @State private var evaluating = false

    @State private var selectedCountry = "US"
    @State private var countryVerdict: OctetVerdict.Result?
    @State private var probing = false

    private let countries = ContentView.isoCountries()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    header
                    mapSection
                    Divider()
                    predicatesSection
                    Divider()
                    countrySection
                }
                .padding()
            }
            .navigationTitle("OctetPolicy")
            .navigationBarTitleDisplayMode(.inline)
        }
        .onReceive(location.$coordinate.compactMap { $0 }) { coord in
            region = MKCoordinateRegion(
                center: coord,
                span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05))
        }
        .task { await startSdk() }
    }

    // MARK: - Sections

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("OctetPolicy").font(.title2).bold()
            Text("Sample — v\(PolicyPackage.version)")
                .font(.subheadline).foregroundStyle(.secondary)
            Text("Every v1 predicate against this device's live location.")
                .font(.footnote).foregroundStyle(.secondary)
        }
    }

    private var mapSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Map(coordinateRegion: $region, showsUserLocation: true)
                .frame(height: 240)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            if let c = location.coordinate {
                Text(String(format: "OS location: %.4f, %.4f", c.latitude, c.longitude))
                    .font(.caption).foregroundStyle(.secondary)
            } else {
                Text("Locating… (OS location)").font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private var predicatesSection: some View {
        Text("Predicates").font(.title3).bold()
        Text("Each tap mints a fresh proof and re-runs every predicate. Give the sensors a few seconds before the first tap.")
            .font(.caption).foregroundStyle(.secondary)

        if let err = sdkError {
            Text("⚠️ \(err)").foregroundStyle(.red).font(.callout)
        } else if loc == nil {
            ProgressView("Starting SDK…")
        } else {
            Button(action: evaluate) {
                Text(evaluating ? "Evaluating…" : "Generate proof & evaluate")
            }
            .buttonStyle(.borderedProminent)
            .disabled(evaluating)

            if evaluating && !didEvaluate {
                ProgressView("Minting proof…")
            } else if !didEvaluate {
                Text("No results yet — tap the button above.")
                    .font(.caption).foregroundStyle(.secondary)
            } else {
                ForEach(results, id: \.label) { item in
                    PredicateRow(label: item.label, result: item.result)
                }
            }
        }
    }

    @ViewBuilder
    private var countrySection: some View {
        Text("Check any country").font(.title3).bold()
        Text("Asks the SDK directly: loc.isWithin(country). Not a policy predicate — just the raw containment answer.")
            .font(.caption).foregroundStyle(.secondary)

        Picker("Country", selection: $selectedCountry) {
            ForEach(countries, id: \.code) { entry in
                Text("\(entry.name) (\(entry.code))").tag(entry.code)
            }
        }
        .pickerStyle(.menu)
        .disabled(loc == nil)
        .onChange(of: selectedCountry) { _ in probeCountry() }

        if loc != nil {
            if probing {
                ProgressView("Checking…")
            } else if let verdict = countryVerdict {
                let name = countries.first { $0.code == selectedCountry }?.name ?? selectedCountry
                let (text, color) = verdictLabel(verdict, country: name)
                Text(text).foregroundStyle(color).font(.headline)
            } else {
                Text("Pick a country to check containment.")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    // MARK: - Actions

    @MainActor
    private func startSdk() async {
        location.start()
        // The key lives in LocalConfig.swift (gitignored — copy
        // LocalConfig.swift.example).
        let key = LocalConfig.octetLicenseKey
        guard !key.isEmpty, key != "octet_live_REPLACE_ME" else {
            sdkError = "No license key. Copy LocalConfig.swift.example to LocalConfig.swift and add your key."
            return
        }
        do {
            let sdk = try await Octet.start(config: OctetConfig(licenseKey: key), startPosition: nil)
            loc = sdk.loc
        } catch {
            sdkError = "\(error)"
        }
    }

    private func evaluate() {
        guard let loc else { return }
        evaluating = true
        Task {
            let r: [(label: String, result: PolicyResult)] = [
                ("isSingapore(loc)", await isSingapore(loc)),
                ("isUS(loc)", await isUS(loc)),
                ("isUSState(loc, [CA, NY, TX])", await isUSState(loc, ["CA", "NY", "TX"])),
                ("isOfacComprehensive(loc)", await isOfacComprehensive(loc)),
            ]
            await MainActor.run {
                results = r
                didEvaluate = true
                evaluating = false
            }
        }
    }

    private func probeCountry() {
        guard let loc else { return }
        probing = true
        countryVerdict = nil
        let code = selectedCountry
        Task {
            let verdict = await loc.isWithin(region: .country(isoCode: code))
            await MainActor.run {
                countryVerdict = verdict.result
                probing = false
            }
        }
    }

    // MARK: - Helpers

    private func verdictLabel(_ verdict: OctetVerdict.Result, country: String) -> (String, Color) {
        switch verdict {
        case .yes: return ("✓ inside \(country)", .green)
        case .no: return ("✗ outside \(country)", .red)
        default: return ("? can't tell (indeterminate)", .blue)
        }
    }

    private static func isoCountries() -> [(code: String, name: String)] {
        let locale = Locale.current
        return Locale.Region.isoRegions
            .map { $0.identifier }
            .filter { $0.count == 2 }
            .map { (code: $0, name: locale.localizedString(forRegionCode: $0) ?? $0) }
            .sorted { $0.name < $1.name }
    }
}

private struct PredicateRow: View {
    let label: String
    let result: PolicyResult

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.subheadline).bold()
            HStack {
                Text(result.match ? "✓ match" : "✗ no match")
                    .foregroundStyle(result.match ? .green : .red).bold()
                Spacer()
                Text("reason: \(String(describing: result.reason))")
                    .foregroundStyle(.secondary)
            }
            .font(.callout)
            Text("country=\(result.country ?? "nil")  state=\(result.state ?? "nil")  confidence=\(String(describing: result.confidence))")
                .font(.caption).foregroundStyle(.secondary)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
