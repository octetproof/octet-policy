// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation
import OctetPolicyCore
import OctetSDK

// ─────────────────────────────────────────────────────────────────
// isSingapore
// ─────────────────────────────────────────────────────────────────
//
// Tutorial-style predicate built on the real octet-location API.
//
// The SDK is a region-containment oracle: `loc.isWithin(region:)`
// answers "is the device in *this* region — yes / no / can't-tell?".
// It does NOT tell you where the device is. So a predicate is a thin
// composition: ask `isWithin` for the region that defines the
// question, then interpret the verdict into a `PolicyResult`.
//
// `isSingapore` is the simplest case — a single country region.
// Multi-region predicates (isOfacComprehensive, isUSState) ask
// `isWithin` once per region and aggregate.

/// Answers: *is the device located in Singapore?*
///
/// Calls `loc.isWithin(.country("SG"))` and interprets the verdict.
/// `async` because the underlying SDK call is async. Returns a
/// structured `PolicyResult`; does **not** act on the answer — that's
/// the caller's call.
public func isSingapore(_ loc: OctetLoc, atTime: Date = Date()) async -> PolicyResult {
    await isSingapore(atTime: atTime) { region in
        await loc.isWithin(region: region, atTime: atTime)
    }
}

/// Testable core. `evaluate` maps a region to a verdict; production
/// passes `loc.isWithin`, tests pass a canned closure so they don't
/// need a live SDK + license key. `internal` — reached via
/// `@testable import`.
func isSingapore(
    atTime: Date = Date(),
    evaluate: (OctetRegion) async -> OctetVerdict
) async -> PolicyResult {

    let policyName = "isSingapore"
    let policyVersion = PolicyPackage.version

    // Singapore is ISO 3166-1 alpha-2 "SG". One containment query.
    let verdict = await evaluate(.country(isoCode: "SG"))

    switch verdict.result {
    case .yes:
        // Device is provably in Singapore. `country` records the
        // region that matched (not "where the device is" — the SDK
        // never reveals that).
        return PolicyResult(
            match: true,
            country: "SG",
            state: nil,
            policyVersion: policyVersion,
            policyName: policyName,
            evaluatedAt: verdict.queriedAt,
            reason: .ok
        )

    case .no:
        // Provably not in Singapore. We don't know where it is, so
        // `country` stays nil. Clean negative — reason `.ok`.
        return PolicyResult(
            match: false,
            country: nil,
            state: nil,
            policyVersion: policyVersion,
            policyName: policyName,
            evaluatedAt: verdict.queriedAt,
            reason: .ok
        )

    case .indeterminate:
        // The SDK couldn't answer (no fix, stale, attestation failed,
        // not-yet-released geometry, …). NOT the same as "not in SG".
        // Callers must inspect reason, not just match.
        return PolicyResult(
            match: false,
            country: nil,
            state: nil,
            policyVersion: policyVersion,
            policyName: policyName,
            evaluatedAt: verdict.queriedAt,
            reason: .verdictIndeterminate
        )
    }
}
