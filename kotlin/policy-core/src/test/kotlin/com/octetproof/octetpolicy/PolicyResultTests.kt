// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Mirrors the Swift `PolicyResultTests`. These pin the
 * **audit-record contract** on [PolicyResult]: defaults and
 * invariants compliance officers depend on when reconstructing
 * "what did our predicate match against on date X."
 */
class PolicyResultTests {

    @Test
    fun `default result for yes match carries audit fields`() {
        val atTime = Instant.ofEpochSecond(1_716_000_000L)
        val result = PolicyResult(
            match = true,
            country = "US",
            state = "CA",
            policyVersion = "0.0.1",
            policyName = "isUSState",
            evaluatedAt = atTime,
        )

        assertTrue(result.match)
        assertEquals("US", result.country)
        assertEquals("CA", result.state)
        assertEquals(Confidence.HIGH, result.confidence,
            "v1 predicates always emit HIGH")
        assertEquals(PolicyReasonCode.OK, result.reason)
        assertEquals("0.0.1", result.policyVersion)
        assertEquals("isUSState", result.policyName)
        assertEquals(atTime, result.evaluatedAt)
    }

    @Test
    fun `match false with indeterminate means unknown not negative`() {
        // The audit-trail invariant. A caller that reads
        // `result.match` alone and treats `false` as "device is not
        // here" is misusing the API.
        val result = PolicyResult(
            match = false,
            policyVersion = "0.0.1",
            policyName = "isUSState",
            reason = PolicyReasonCode.VERDICT_INDETERMINATE,
        )

        assertFalse(result.match)
        assertEquals(PolicyReasonCode.VERDICT_INDETERMINATE, result.reason,
            "match=false + VERDICT_INDETERMINATE means 'unknown', not 'negative'")
    }

    @Test
    fun `confidence enum names match upstream convention`() {
        // The enum `name` property is the cross-language audit-chain
        // key. Swift's `Confidence.high.rawValue == "HIGH"` must
        // match Kotlin's `Confidence.HIGH.name == "HIGH"`.
        assertEquals("HIGH", Confidence.HIGH.name)
        assertEquals("MEDIUM", Confidence.MEDIUM.name)
        assertEquals("LOW", Confidence.LOW.name)
        assertEquals(Confidence.MEDIUM, Confidence.valueOf("MEDIUM"))
    }

    @Test
    fun `policy reason code enum names match upstream convention`() {
        assertEquals("OK", PolicyReasonCode.OK.name)
        assertEquals("VERDICT_INDETERMINATE", PolicyReasonCode.VERDICT_INDETERMINATE.name)
        assertEquals("INPUT_INVALID", PolicyReasonCode.INPUT_INVALID.name)
    }

    @Test
    fun `policy package version is pinned`() {
        // Brittle on purpose: if VERSION is bumped, this test fails
        // until PolicyPackage.VERSION is bumped too. The CI
        // version-sync lint provides a stricter parity check across
        // every place the version is hardcoded.
        assertEquals("0.0.2-alpha", PolicyPackage.VERSION)
    }

    @Test
    fun `equality when all fields match`() {
        val atTime = Instant.ofEpochSecond(1_716_000_000L)
        val a = PolicyResult(
            match = true,
            country = "SG",
            policyVersion = "0.0.1",
            policyName = "isSingapore",
            evaluatedAt = atTime,
        )
        val b = PolicyResult(
            match = true,
            country = "SG",
            policyVersion = "0.0.1",
            policyName = "isSingapore",
            evaluatedAt = atTime,
        )
        assertEquals(a, b)
    }

    @Test
    fun `inequality when match differs`() {
        val atTime = Instant.ofEpochSecond(1_716_000_000L)
        val a = PolicyResult(
            match = true, country = "SG",
            policyVersion = "0.0.1", policyName = "isSingapore",
            evaluatedAt = atTime,
        )
        val b = PolicyResult(
            match = false, country = "SG",
            policyVersion = "0.0.1", policyName = "isSingapore",
            evaluatedAt = atTime,
        )
        assertNotEquals(a, b)
    }
}
