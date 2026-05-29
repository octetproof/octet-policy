// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation
import OctetPolicyCore
import OctetSDK

// ─────────────────────────────────────────────────────────────────
// isUSState — subdivision-level predicate. Multi-region: queries
// OctetRegion.usState(code) once per requested state and aggregates.
//
// Aggregation (fail-closed):
//   - any region returns .yes        → match
//   - else any region indeterminate  → unknown (.verdictIndeterminate)
//   - else (all .no)                 → clean negative (.ok)
//
// Input validation runs first: an empty list or any code not in the
// ISO 3166-2:US set (states.json) returns .inputInvalid before any
// SDK call. Programmer bugs fail loud and closed.

/// Single-state convenience overload.
public func isUSState(_ loc: OctetLoc, _ state: String, atTime: Date = Date()) async -> PolicyResult {
    await isUSState(loc, [state], atTime: atTime)
}

/// Answers: *is the device in one of these US states / territories?*
/// `states` are ISO 3166-2:US codes without the `US-` prefix
/// (`["CA", "NY"]`).
public func isUSState(_ loc: OctetLoc, _ states: [String], atTime: Date = Date()) async -> PolicyResult {
    await isUSState(states, atTime: atTime) { region in
        await loc.isWithin(region: region, atTime: atTime)
    }
}

/// Testable core.
func isUSState(
    _ states: [String],
    atTime: Date = Date(),
    evaluate: (OctetRegion) async -> OctetVerdict
) async -> PolicyResult {

    let policyName = "isUSState"
    let policyVersion = PolicyPackage.version

    // ── Input validation (fail closed before any SDK call) ──────
    if states.isEmpty {
        return PolicyResult(
            match: false, policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: atTime, reason: .inputInvalid
        )
    }
    let everyCodeIsValid = states.allSatisfy { UsStateList.validCodes.contains($0) }
    if !everyCodeIsValid {
        return PolicyResult(
            match: false, policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: atTime, reason: .inputInvalid
        )
    }

    // ── Query each requested state; first YES wins ──────────────
    var sawIndeterminate = false
    var lastEvaluatedAt = atTime
    for code in states {
        let verdict = await evaluate(.usState(code))
        lastEvaluatedAt = verdict.queriedAt
        switch verdict.result {
        case .yes:
            return PolicyResult(
                match: true, country: "US", state: code,
                policyVersion: policyVersion, policyName: policyName,
                evaluatedAt: verdict.queriedAt, reason: .ok
            )
        case .no:
            continue
        case .indeterminate:
            sawIndeterminate = true
        }
    }

    // ── No match. Fail closed if any region was indeterminate ───
    if sawIndeterminate {
        return PolicyResult(
            match: false, policyVersion: policyVersion, policyName: policyName,
            evaluatedAt: lastEvaluatedAt, reason: .verdictIndeterminate
        )
    }
    return PolicyResult(
        match: false, country: nil, state: nil,
        policyVersion: policyVersion, policyName: policyName,
        evaluatedAt: lastEvaluatedAt, reason: .ok
    )
}
