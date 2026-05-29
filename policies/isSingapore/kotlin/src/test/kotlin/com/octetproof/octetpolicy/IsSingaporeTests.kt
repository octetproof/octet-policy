// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import com.octetproof.sdk.api.ConfidenceSummary
import com.octetproof.sdk.api.LocationProof
import com.octetproof.sdk.api.OctetRegion
import com.octetproof.sdk.api.OctetVerdict
import com.octetproof.sdk.api.ProofLevel
import com.octetproof.sdk.api.ProofRegion
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tests for `isSingapore` against the real region-containment model.
 * The testable core takes a `suspend (OctetRegion) -> OctetVerdict`
 * closure, so each test injects a canned verdict — no live SDK or
 * license key needed. Mirrors the Swift IsSingaporeTests.
 */
class IsSingaporeTests {

    private val fixedTime: Instant = Instant.ofEpochSecond(1_716_000_000)

    // 0.0.1-alpha LocationProof has no default instance; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private fun fakeProof(): LocationProof = LocationProof(
        "test", ProofRegion.Country("SG"), ProofLevel.COUNTRY, 0L,
        ConfidenceSummary(), ByteArray(0), "test", "test", ByteArray(0),
    )

    /** Build a verdict honoring the SDK invariant: proof is null iff
     *  result == INDETERMINATE. */
    private fun verdict(
        result: OctetVerdict.Result,
        reason: OctetVerdict.ReasonCode = OctetVerdict.ReasonCode.OK,
    ): OctetVerdict {
        val proof: LocationProof? =
            if (result == OctetVerdict.Result.INDETERMINATE) null
            else fakeProof()
        return OctetVerdict(
            result = result,
            reason = reason,
            message = "test",
            proof = proof,
            validity = null,
            queriedAt = fixedTime,
            confidence = ConfidenceSummary(),
        )
    }

    @Test
    fun `device in singapore matches`() = runTest {
        val result = isSingapore { region ->
            assertEquals(OctetRegion.country("SG"), region,
                "isSingapore must query the SG country region")
            verdict(OctetVerdict.Result.YES)
        }
        assertTrue(result.match)
        assertEquals("SG", result.country)
        assertNull(result.state)
        assertEquals(PolicyReasonCode.OK, result.reason)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals("isSingapore", result.policyName)
        assertEquals(PolicyPackage.VERSION, result.policyVersion)
        assertEquals(fixedTime, result.evaluatedAt)
    }

    @Test
    fun `device not in singapore is clean negative`() = runTest {
        val result = isSingapore { verdict(OctetVerdict.Result.NO) }
        assertFalse(result.match)
        assertNull(result.country,
            "a NO verdict doesn't reveal where the device is — country stays null")
        assertEquals(PolicyReasonCode.OK, result.reason)
    }

    @Test
    fun `indeterminate verdict is unknown not negative`() = runTest {
        val result = isSingapore {
            verdict(OctetVerdict.Result.INDETERMINATE, OctetVerdict.ReasonCode.NO_FIX)
        }
        assertFalse(result.match)
        assertNull(result.country)
        assertEquals(PolicyReasonCode.VERDICT_INDETERMINATE, result.reason)
    }

    @Test
    fun `not yet released geometry folds to indeterminate`() = runTest {
        val result = isSingapore {
            verdict(OctetVerdict.Result.INDETERMINATE, OctetVerdict.ReasonCode.NOT_YET_RELEASED)
        }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.VERDICT_INDETERMINATE, result.reason)
    }
}
