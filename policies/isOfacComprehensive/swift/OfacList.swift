// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation

// MARK: - OFAC list data types and loader
//
// Internal to the `IsOfacComprehensive` module. The predicate reads
// from `OfacList.entries`. The JSON file (`countries.json`) is the
// authoritative source — these types are just the in-memory shape
// for it.

/// One entry in the OFAC comprehensive list. Either country-level
/// (`subdivision == nil`) or subdivision-level (`subdivision != nil`).
struct OfacEntry: Decodable, Equatable {
    /// ISO 3166-1 alpha-2 country code (e.g. `"CU"`, `"UA"`).
    let iso3166_1: String
    /// Human-readable name. Audit-only; the predicate does not match
    /// on this field.
    let name: String
    /// ISO 3166-2 subdivision code **without** the country prefix
    /// (e.g. `"43"` for Crimea, not `"UA-43"`). `nil` for
    /// country-level entries.
    let subdivision: String?
    /// Primary-source URL for this entry. Audit-only.
    let sourceUrl: String
    /// ISO 8601 date this entry was added to the list. Audit-only.
    let addedOn: String
    /// Name of the compliance reviewer who signed off. Audit-only.
    let reviewedBy: String

    private enum CodingKeys: String, CodingKey {
        case iso3166_1     = "iso_3166_1"
        case name
        case subdivision
        case sourceUrl     = "source_url"
        case addedOn       = "added_on"
        case reviewedBy    = "reviewed_by"
    }
}

/// The whole `countries.json` document. The top-level fields
/// (`listId`, `policyVersion`, `effectiveDate`) are metadata about
/// the list itself; they are loaded but not used at runtime by the
/// predicate. Tests pin them.
struct OfacListFile: Decodable {
    let listId: String
    let policyVersion: String
    let effectiveDate: String
    let entries: [OfacEntry]

    private enum CodingKeys: String, CodingKey {
        case listId        = "list_id"
        case policyVersion = "policy_version"
        case effectiveDate = "effective_date"
        case entries
    }
}

/// Loads `countries.json` once at first access and caches it.
///
/// Decoding failure is a programmer error (the file ships *with* the
/// SDK), so we `fatalError`. Silently falling back to an empty list
/// would cause every OFAC check to return `match=false` — exactly
/// the wrong direction to fail in.
enum OfacList {
    static let file: OfacListFile = loadFromBundle()
    static var entries: [OfacEntry] { file.entries }

    /// Test seam exposing the loader's exact JSON decoder. Lets the
    /// parity tests pin lockstep with the Kotlin side — a regression
    /// to strict decoding here fails those tests. `JSONDecoder` is
    /// lenient by default (unknown keys silently ignored), and that
    /// behaviour is load-bearing: `countries.json` may grow audit
    /// metadata fields that we don't decode into typed properties.
    static func decode(_ data: Data) throws -> OfacListFile {
        try JSONDecoder().decode(OfacListFile.self, from: data)
    }

    private static func loadFromBundle() -> OfacListFile {
        guard let url = Bundle.module.url(forResource: "countries", withExtension: "json") else {
            fatalError("IsOfacComprehensive: countries.json missing from module bundle. This is a build error.")
        }
        do {
            let data = try Data(contentsOf: url)
            return try decode(data)
        } catch {
            fatalError("IsOfacComprehensive: countries.json failed to decode: \(error). This is a build error.")
        }
    }
}
