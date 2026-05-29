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
 * Tests for [isOfacComprehensive] against the multi-region
 * containment model. Pins both the match rules AND the data-file
 * integrity — for an OFAC predicate the data file *is* part of the
 * contract. Mirrors the Swift IsOfacComprehensiveTests.
 */
class IsOfacComprehensiveTests {

    private val fixedTime: Instant = Instant.ofEpochSecond(1_716_000_000)

    // 0.0.1-alpha LocationProof has no default instance; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private fun fakeProof(): LocationProof = LocationProof(
        "test", ProofRegion.Country("IR"), ProofLevel.COUNTRY, 0L,
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

    /** Device that is in exactly [target] and nowhere else on the list. */
    private fun inRegion(target: OctetRegion): suspend (OctetRegion) -> OctetVerdict = { region ->
        if (region == target) verdict(OctetVerdict.Result.YES) else verdict(OctetVerdict.Result.NO)
    }

    // ── Country-level matches (CU, IR, KP) ──────────────────────

    @Test
    fun `country level sanctioned countries all match`() = runTest {
        for (country in listOf("CU", "IR", "KP")) {
            val result = isOfacComprehensive(evaluate = inRegion(OctetRegion.country(country)))
            assertTrue(result.match, "expected match for country-level sanctioned $country")
            assertEquals(country, result.country)
            assertNull(result.state)
            assertEquals(PolicyReasonCode.OK, result.reason)
        }
    }

    // ── Subdivision matches (UA-43, UA-14, UA-09) ──────────────

    @Test
    fun `subdivision sanctioned oblasts all match`() = runTest {
        val oblasts = listOf("43" to "Crimea", "14" to "Donetsk", "09" to "Luhansk")
        for ((sub, name) in oblasts) {
            val result = isOfacComprehensive(evaluate = inRegion(OctetRegion.subdivision("UA-$sub")))
            assertTrue(result.match, "expected match for subdivision $name (UA-$sub)")
            assertEquals("UA", result.country)
            assertEquals(sub, result.state)
            assertEquals(PolicyReasonCode.OK, result.reason)
        }
    }

    // ── Clean negatives ─────────────────────────────────────────

    @Test
    fun `device in a non sanctioned location is clean negative`() = runTest {
        // The predicate only queries list regions; a device that is in
        // none of them gets NO for every query → clean negative.
        val result = isOfacComprehensive { verdict(OctetVerdict.Result.NO) }
        assertFalse(result.match)
        assertNull(result.country)
        assertEquals(PolicyReasonCode.OK, result.reason)
    }

    // ── Fail-closed on indeterminate ────────────────────────────

    @Test
    fun `any indeterminate region with no match folds to unknown`() = runTest {
        // No region matches, but one came back indeterminate → unknown,
        // never a false "not sanctioned". This is the fail-closed
        // contract that replaced the old STATE_MISSING special case.
        val result = isOfacComprehensive { region ->
            if (region == OctetRegion.subdivision("UA-43"))
                verdict(OctetVerdict.Result.INDETERMINATE, OctetVerdict.ReasonCode.NO_FIX)
            else verdict(OctetVerdict.Result.NO)
        }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.VERDICT_INDETERMINATE, result.reason,
            "match=false here means 'we cannot tell', not 'device is not sanctioned'")
    }

    // ── Data file integrity ─────────────────────────────────────

    @Test
    fun `list has exactly six entries`() {
        assertEquals(6, OfacList.entries.size,
            "v1 list should have 6 entries; if you changed the list, update this test and bump the version")
    }

    @Test
    fun `list policy version matches package version`() {
        assertEquals(PolicyPackage.VERSION, OfacList.file.policyVersion)
    }

    @Test
    fun `list contains expected entries`() {
        val expected = setOf("CU/-", "IR/-", "KP/-", "UA/43", "UA/14", "UA/09")
        val actual = OfacList.entries.map { "${it.iso3166_1}/${it.subdivision ?: "-"}" }.toSet()
        assertEquals(expected, actual)
    }

    // ── Audit trail ─────────────────────────────────────────────

    @Test
    fun `evaluated at copies through from verdict`() = runTest {
        val result = isOfacComprehensive(evaluate = inRegion(OctetRegion.country("CU")))
        assertEquals(fixedTime, result.evaluatedAt)
    }

    @Test
    fun `policy name and version are stamped`() = runTest {
        val result = isOfacComprehensive(evaluate = inRegion(OctetRegion.country("IR")))
        assertEquals("isOfacComprehensive", result.policyName)
        assertEquals(PolicyPackage.VERSION, result.policyVersion)
    }
}
