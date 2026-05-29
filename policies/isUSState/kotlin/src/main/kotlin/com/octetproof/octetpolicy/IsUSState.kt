// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import com.octetproof.sdk.api.OctetLoc
import com.octetproof.sdk.api.OctetRegion
import com.octetproof.sdk.api.OctetVerdict
import java.time.Instant

// ─────────────────────────────────────────────────────────────────
// isUSState — subdivision-level predicate. Multi-region: queries
// OctetRegion.usState(code) once per requested state and aggregates.
//
// Aggregation (fail-closed):
//   - any region returns YES        → match
//   - else any region indeterminate → unknown (VERDICT_INDETERMINATE)
//   - else (all NO)                 → clean negative (OK)
//
// Input validation runs first: an empty list or any code not in the
// ISO 3166-2:US set (states.json) returns INPUT_INVALID before any
// SDK call. Programmer bugs fail loud and closed.
//
// Mirrors Swift's IsUSState.swift.

/** Single-state convenience overload. Delegates to the list form. */
suspend fun isUSState(loc: OctetLoc, state: String, atTime: Instant = Instant.now()): PolicyResult =
    isUSState(loc, listOf(state), atTime)

/**
 * Answers: *is the device in one of these US states / territories?*
 * [states] are ISO 3166-2:US codes without the `US-` prefix
 * (`["CA", "NY"]`).
 */
suspend fun isUSState(loc: OctetLoc, states: List<String>, atTime: Instant = Instant.now()): PolicyResult =
    isUSState(states, atTime) { region -> loc.isWithin(region, atTime) }

/** Testable core. */
internal suspend fun isUSState(
    states: List<String>,
    atTime: Instant = Instant.now(),
    evaluate: suspend (OctetRegion) -> OctetVerdict,
): PolicyResult {
    val policyName = "isUSState"
    val policyVersion = PolicyPackage.VERSION

    // ── Input validation (fail closed before any SDK call) ──────
    if (states.isEmpty()) {
        return PolicyResult(
            match = false, policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = atTime, reason = PolicyReasonCode.INPUT_INVALID,
        )
    }
    if (!states.all { it in UsStateList.validCodes }) {
        return PolicyResult(
            match = false, policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = atTime, reason = PolicyReasonCode.INPUT_INVALID,
        )
    }

    // ── Query each requested state; first YES wins ──────────────
    var sawIndeterminate = false
    var lastEvaluatedAt = atTime
    for (code in states) {
        val verdict = evaluate(OctetRegion.usState(code))
        lastEvaluatedAt = verdict.queriedAt
        when (verdict.result) {
            OctetVerdict.Result.YES -> return PolicyResult(
                match = true, country = "US", state = code,
                policyVersion = policyVersion, policyName = policyName,
                evaluatedAt = verdict.queriedAt, reason = PolicyReasonCode.OK,
            )
            OctetVerdict.Result.NO -> continue
            OctetVerdict.Result.INDETERMINATE -> sawIndeterminate = true
        }
    }

    // ── No match. Fail closed if any region was indeterminate ───
    return if (sawIndeterminate) {
        PolicyResult(
            match = false, policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = lastEvaluatedAt, reason = PolicyReasonCode.VERDICT_INDETERMINATE,
        )
    } else {
        PolicyResult(
            match = false, country = null, state = null,
            policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = lastEvaluatedAt, reason = PolicyReasonCode.OK,
        )
    }
}
