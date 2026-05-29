// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import com.octetproof.sdk.api.OctetLoc
import com.octetproof.sdk.api.OctetRegion
import com.octetproof.sdk.api.OctetVerdict
import java.time.Instant

// ─────────────────────────────────────────────────────────────────
// isUS — country-level predicate. Single containment query against
// OctetRegion.country("US").
//
// "US" at the country level is the 50 states + DC. The unincorporated
// territories (PR, GU, VI, AS, MP) have their own ISO 3166-1 codes, so
// a device there yields a NO from country("US") — the territory
// exclusion is now the SDK's job, not ours. A caller who wants "US
// plus territories" composes: `isUS(loc).match || isInTerritory(loc)`.
//
// Mirrors Swift's IsUS.swift.

/** Answers: *is the device located in the United States (50 states + DC)?* */
suspend fun isUS(loc: OctetLoc, atTime: Instant = Instant.now()): PolicyResult =
    isUS(atTime) { region -> loc.isWithin(region, atTime) }

/** Testable core — see `isSingapore` for the closure-injection rationale. */
internal suspend fun isUS(
    atTime: Instant = Instant.now(),
    evaluate: suspend (OctetRegion) -> OctetVerdict,
): PolicyResult {
    val policyName = "isUS"
    val policyVersion = PolicyPackage.VERSION

    val verdict = evaluate(OctetRegion.country("US"))

    return when (verdict.result) {
        OctetVerdict.Result.YES -> PolicyResult(
            match = true, country = "US", state = null,
            policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = verdict.queriedAt, reason = PolicyReasonCode.OK,
        )
        OctetVerdict.Result.NO -> PolicyResult(
            match = false, country = null, state = null,
            policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = verdict.queriedAt, reason = PolicyReasonCode.OK,
        )
        OctetVerdict.Result.INDETERMINATE -> PolicyResult(
            match = false, country = null, state = null,
            policyVersion = policyVersion, policyName = policyName,
            evaluatedAt = verdict.queriedAt, reason = PolicyReasonCode.VERDICT_INDETERMINATE,
        )
    }
}
