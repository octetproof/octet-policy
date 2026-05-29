// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation
import OctetPolicyCore
import OctetSDK

// ─────────────────────────────────────────────────────────────────
// isOfacComprehensive — multi-region predicate over the OFAC
// comprehensive embargo list (countries.json). Each entry is queried
// as a region: country-level entries via OctetRegion.country(iso),
// subdivision entries (the Ukrainian oblasts) via
// OctetRegion.subdivision("UA-43") etc.
//
// Aggregation (fail-closed — the right direction for sanctions):
//   - any sanctioned region returns .yes → match
//   - else any region indeterminate      → unknown (.verdictIndeterminate)
//   - else (all .no)                      → clean negative (.ok)
//
// The fail-closed-on-indeterminate rule replaces the old
// "STATE_MISSING" special case: if the SDK can't determine whether
// the device is in a sanctioned oblast, we surface "unknown", never
// a false "not sanctioned". Callers blocking on OFAC compliance must
// inspect reason, not just match.

/// Answers: *is the device located in a country or region under OFAC
/// comprehensive embargo?*
public func isOfacComprehensive(_ loc: OctetLoc, atTime: Date = Date()) async -> PolicyResult {
    await isOfacComprehensive(atTime: atTime) { region in
        await loc.isWithin(region: region, atTime: atTime)
    }
}

/// Testable core.
func isOfacComprehensive(
    atTime: Date = Date(),
    evaluate: (OctetRegion) async -> OctetVerdict
) async -> PolicyResult {

    let policyName = "isOfacComprehensive"
    let policyVersion = PolicyPackage.version

    var sawIndeterminate = false
    var lastEvaluatedAt = atTime

    for entry in OfacList.entries {
        // Build the region for this entry: subdivision if it has one,
        // else country-level.
        let region: OctetRegion
        if let subdivision = entry.subdivision {
            region = .subdivision(isoCode: "\(entry.iso3166_1)-\(subdivision)")
        } else {
            region = .country(isoCode: entry.iso3166_1)
        }

        let verdict = await evaluate(region)
        lastEvaluatedAt = verdict.queriedAt
        switch verdict.result {
        case .yes:
            return PolicyResult(
                match: true, country: entry.iso3166_1, state: entry.subdivision,
                policyVersion: policyVersion, policyName: policyName,
                evaluatedAt: verdict.queriedAt, reason: .ok
            )
        case .no:
            continue
        case .indeterminate:
            sawIndeterminate = true
        }
    }

    if sawIndeterminate {
        return PolicyResult(
            match: false, policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: lastEvaluatedAt, reason: .verdictIndeterminate
        )
    }
    return PolicyResult(
        match: false, policyVersion: policyVersion, policyName: policyName,
        evaluatedAt: lastEvaluatedAt, reason: .ok
    )
}
