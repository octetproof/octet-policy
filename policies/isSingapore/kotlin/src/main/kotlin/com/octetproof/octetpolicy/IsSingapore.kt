// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import com.octetproof.sdk.api.OctetLoc
import com.octetproof.sdk.api.OctetRegion
import com.octetproof.sdk.api.OctetVerdict
import java.time.Instant

// ─────────────────────────────────────────────────────────────────
// isSingapore
// ─────────────────────────────────────────────────────────────────
//
// Tutorial-style predicate built on the real octet-location API.
// The SDK is a region-containment oracle: `loc.isWithin(region)`
// answers "is the device in this region — yes / no / can't-tell?".
// A predicate composes one (or more) such queries and interprets
// the verdict(s) into a PolicyResult. `suspend` because the SDK call
// is suspending. Mirrors the Swift IsSingapore.swift.

/**
 * Answers: *is the device located in Singapore?* Queries
 * `loc.isWithin(OctetRegion.country("SG"))` and interprets the
 * verdict. Does **not** act on the answer — that's the caller's call.
 */
suspend fun isSingapore(loc: OctetLoc, atTime: Instant = Instant.now()): PolicyResult =
    isSingapore(atTime) { region -> loc.isWithin(region, atTime) }

/**
 * Testable core. `evaluate` maps a region to a verdict; production
 * passes `loc::isWithin`, tests pass a canned closure so they don't
 * need a live SDK + license key. `internal` — reached from the
 * module's own tests.
 */
internal suspend fun isSingapore(
    atTime: Instant = Instant.now(),
    evaluate: suspend (OctetRegion) -> OctetVerdict,
): PolicyResult {
    val policyName = "isSingapore"
    val policyVersion = PolicyPackage.VERSION

    val verdict = evaluate(OctetRegion.country("SG"))

    return when (verdict.result) {
        OctetVerdict.Result.YES -> PolicyResult(
            match = true, country = "SG", state = null,
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
