// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import com.octetproof.sdk.api.OctetLoc
import com.octetproof.sdk.api.OctetRegion
import com.octetproof.sdk.api.OctetVerdict
import java.time.Instant

// ─────────────────────────────────────────────────────────────────
// isOfacComprehensive — multi-region predicate over the OFAC
// comprehensive embargo list (countries.json). Each entry is queried
// as a region: country-level entries via OctetRegion.country(iso),
// subdivision entries (the Ukrainian oblasts) via
// OctetRegion.subdivision("UA-43") etc.
//
// Aggregation (fail-closed — the right direction for sanctions):
//   - any sanctioned region returns YES → match
//   - else any region indeterminate     → unknown (VERDICT_INDETERMINATE)
//   - else (all NO)                      → clean negative (OK)
//
// The fail-closed-on-indeterminate rule replaces the old
// "STATE_MISSING" special case: if the SDK can't determine whether
// the device is in a sanctioned oblast, we surface "unknown", never a
// false "not sanctioned". Callers blocking on OFAC compliance must
// inspect reason, not just match.
//
// Mirrors Swift's IsOfacComprehensive.swift.

/**
 * Answers: *is the device located in a country or region under OFAC
 * comprehensive embargo?*
 */
suspend fun isOfacComprehensive(loc: OctetLoc, atTime: Instant = Instant.now()): PolicyResult =
    isOfacComprehensive(atTime) { region -> loc.isWithin(region, atTime) }

/** Testable core. */
internal suspend fun isOfacComprehensive(
    atTime: Instant = Instant.now(),
    evaluate: suspend (OctetRegion) -> OctetVerdict,
): PolicyResult {
    val policyName = "isOfacComprehensive"
    val policyVersion = PolicyPackage.VERSION

    var sawIndeterminate = false
    var lastEvaluatedAt = atTime

    for (entry in OfacList.entries) {
        val region: OctetRegion =
            if (entry.subdivision != null) OctetRegion.subdivision("${entry.iso3166_1}-${entry.subdivision}")
            else OctetRegion.country(entry.iso3166_1)

        val verdict = evaluate(region)
        lastEvaluatedAt = verdict.queriedAt
        when (verdict.result) {
            OctetVerdict.Result.YES -> return PolicyResult(
                match = true, country = entry.iso3166_1, state = entry.subdivision,
                policyVersion = policyVersion, policyName = policyName,
                evaluatedAt = verdict.queriedAt, reason = PolicyReasonCode.OK,
            )
            OctetVerdict.Result.NO -> continue
            OctetVerdict.Result.INDETERMINATE -> sawIndeterminate = true
        }
    }

    return if (sawIndeterminate) {
        PolicyResult(
            match = false, policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = lastEvaluatedAt, reason = PolicyReasonCode.VERDICT_INDETERMINATE,
        )
    } else {
        PolicyResult(
            match = false, policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = lastEvaluatedAt, reason = PolicyReasonCode.OK,
        )
    }
}
