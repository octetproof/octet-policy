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
 * Tests for [isUS] against the region-containment model. The
 * territory exclusion is now the SDK's responsibility (PR/GU/etc.
 * yield NO from country("US")), so it's no longer pinned here.
 * Mirrors the Swift IsUSTests.
 */
class IsUSTests {

    private val fixedTime: Instant = Instant.ofEpochSecond(1_716_000_000)

    // 0.0.1-alpha LocationProof has no default instance; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private fun fakeProof(): LocationProof = LocationProof(
        "test", ProofRegion.Country("US"), ProofLevel.COUNTRY, 0L,
        ConfidenceSummary(), ByteArray(0), "test", "test", ByteArray(0),
    )

    private fun verdict(
        result: OctetVerdict.Result,
        reason: OctetVerdict.ReasonCode = OctetVerdict.ReasonCode.OK,
    ): OctetVerdict {
        val proof: LocationProof? =
            if (result == OctetVerdict.Result.INDETERMINATE) null
            else fakeProof()
        return OctetVerdict(
            result = result, reason = reason, message = "test",
            proof = proof, validity = null, queriedAt = fixedTime,
            confidence = ConfidenceSummary(),
        )
    }

    @Test
    fun `device in US matches`() = runTest {
        val result = isUS { region ->
            assertEquals(OctetRegion.country("US"), region)
            verdict(OctetVerdict.Result.YES)
        }
        assertTrue(result.match)
        assertEquals("US", result.country)
        assertNull(result.state)
        assertEquals(PolicyReasonCode.OK, result.reason)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals("isUS", result.policyName)
        assertEquals(PolicyPackage.VERSION, result.policyVersion)
        assertEquals(fixedTime, result.evaluatedAt)
    }

    @Test
    fun `device outside US is clean negative`() = runTest {
        val result = isUS { verdict(OctetVerdict.Result.NO) }
        assertFalse(result.match)
        assertNull(result.country,
            "a NO verdict doesn't reveal where the device is — country stays null")
        assertEquals(PolicyReasonCode.OK, result.reason)
    }

    @Test
    fun `indeterminate verdict is unknown not negative`() = runTest {
        val result = isUS {
            verdict(OctetVerdict.Result.INDETERMINATE, OctetVerdict.ReasonCode.NO_FIX)
        }
        assertFalse(result.match)
        assertNull(result.country)
        assertEquals(PolicyReasonCode.VERDICT_INDETERMINATE, result.reason,
            "match=false here means 'we cannot tell', not 'device is not in the US'")
    }
}
