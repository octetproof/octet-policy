// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation
import OctetPolicyCore
import OctetSDK

// ─────────────────────────────────────────────────────────────────
// isUS — country-level predicate. Single containment query against
// OctetRegion.country("US").
//
// "US" at the country level is the 50 states + DC. The unincorporated
// territories (PR, GU, VI, AS, MP) have their own ISO 3166-1 codes, so
// a device there yields a `.no` from `.country("US")` — the territory
// exclusion is now the SDK's job, not ours. A caller who wants "US
// plus territories" composes: `isUS(loc).match || isInTerritory(loc)`.

/// Answers: *is the device located in the United States (50 states +
/// DC)?*
public func isUS(_ loc: OctetLoc, atTime: Date = Date()) async -> PolicyResult {
    await isUS(atTime: atTime) { region in
        await loc.isWithin(region: region, atTime: atTime)
    }
}

/// Testable core — see `isSingapore` for the closure-injection rationale.
func isUS(
    atTime: Date = Date(),
    evaluate: (OctetRegion) async -> OctetVerdict
) async -> PolicyResult {

    let policyName = "isUS"
    let policyVersion = PolicyPackage.version

    let verdict = await evaluate(.country(isoCode: "US"))

    switch verdict.result {
    case .yes:
        return PolicyResult(
            match: true, country: "US", state: nil,
            policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: verdict.queriedAt, reason: .ok
        )
    case .no:
        return PolicyResult(
            match: false, country: nil, state: nil,
            policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: verdict.queriedAt, reason: .ok
        )
    case .indeterminate:
        return PolicyResult(
            match: false, country: nil, state: nil,
            policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: verdict.queriedAt, reason: .verdictIndeterminate
        )
    }
}
