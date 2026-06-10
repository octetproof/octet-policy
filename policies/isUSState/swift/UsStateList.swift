// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation

// MARK: - US subdivision list data types and loader
//
// Internal to the `IsUSState` module. The predicate uses
// `UsStateList.validCodes` to validate caller input, and reads from
// `states.json` for the audit-friendly entries.

/// One entry in the ISO 3166-2:US subdivision list. `kind` is
/// informational — the predicate treats states, the district, and
/// territories identically for matching purposes (the caller's
/// list is the source of truth for "what counts").
struct UsStateEntry: Decodable, Equatable {
    /// ISO 3166-2 subdivision code WITHOUT the country prefix
    /// (e.g. `"CA"`, not `"US-CA"`).
    let code: String
    /// Human-readable name. Audit-only; not used for matching.
    let name: String
    /// `"state"` | `"district"` | `"territory"`. Audit-only.
    let kind: String
}

/// The whole `states.json` document.
struct UsStateListFile: Decodable {
    let listId: String
    let policyVersion: String
    let effectiveDate: String
    let entries: [UsStateEntry]

    private enum CodingKeys: String, CodingKey {
        case listId        = "list_id"
        case policyVersion = "policy_version"
        case effectiveDate = "effective_date"
        case entries
    }
}

/// Loads `states.json` once at first access and caches it. Decoding
/// failure is a programmer error — see the equivalent comment in
/// `IsOfacComprehensive/swift/OfacList.swift`.
enum UsStateList {
    static let file: UsStateListFile = loadFromBundle()
    static var entries: [UsStateEntry] { file.entries }

    /// Set of valid ISO 3166-2:US subdivision codes, without the
    /// `"US-"` prefix. Used by the predicate to validate caller
    /// input.
    static let validCodes: Set<String> = Set(file.entries.map { $0.code })

    /// Test seam exposing the loader's exact JSON decoder. Lets the
    /// parity tests pin lockstep with the Kotlin side — a regression
    /// to strict decoding here fails those tests. `JSONDecoder` is
    /// lenient by default (unknown keys silently ignored), and that
    /// behaviour is load-bearing: `states.json` carries a top-level
    /// `source_note` audit field today.
    static func decode(_ data: Data) throws -> UsStateListFile {
        try JSONDecoder().decode(UsStateListFile.self, from: data)
    }

    private static func loadFromBundle() -> UsStateListFile {
        guard let url = Bundle.module.url(forResource: "states", withExtension: "json") else {
            fatalError("IsUSState: states.json missing from module bundle. This is a build error.")
        }
        do {
            let data = try Data(contentsOf: url)
            return try decode(data)
        } catch {
            fatalError("IsUSState: states.json failed to decode: \(error). This is a build error.")
        }
    }
}
