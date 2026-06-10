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
 * Tests for [isUSState] against the multi-region containment model.
 * The testable core takes a `suspend (OctetRegion) -> OctetVerdict`
 * closure; tests inject canned verdicts keyed off the region the
 * predicate queries. Mirrors the Swift IsUSStateTests.
 */
class IsUSStateTests {

    private val fixedTime: Instant = Instant.ofEpochSecond(1_716_000_000)

    // 0.0.1-alpha LocationProof has no default instance; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private fun fakeProof(): LocationProof = LocationProof(
        "test", ProofRegion.Subdivision("US-CA"), ProofLevel.SUBDIVISION, 0L,
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

    /** A device that is in exactly [matchState] (US-XX) and nowhere else. */
    private fun inState(matchState: String): suspend (OctetRegion) -> OctetVerdict = { region ->
        if (region == OctetRegion.usState(matchState)) verdict(OctetVerdict.Result.YES)
        else verdict(OctetVerdict.Result.NO)
    }

    // ── Positive matches ───────────────────────────────────────

    @Test
    fun `device in california with CA in list matches`() = runTest {
        val result = isUSState(listOf("CA"), evaluate = inState("CA"))
        assertTrue(result.match)
        assertEquals("US", result.country)
        assertEquals("CA", result.state)
        assertEquals(PolicyReasonCode.OK, result.reason)
        assertEquals("isUSState", result.policyName)
        assertEquals(fixedTime, result.evaluatedAt)
    }

    @Test
    fun `device in california with CA in multi state list matches`() = runTest {
        val result = isUSState(listOf("NY", "CA", "TX"), evaluate = inState("CA"))
        assertTrue(result.match)
        assertEquals("CA", result.state)
    }

    @Test
    fun `dc is a valid subdivision code`() = runTest {
        val result = isUSState(listOf("DC"), evaluate = inState("DC"))
        assertTrue(result.match)
        assertEquals("DC", result.state)
    }

    @Test
    fun `all five territories are valid subdivision codes`() = runTest {
        for (code in listOf("PR", "GU", "VI", "AS", "MP")) {
            val result = isUSState(listOf(code), evaluate = inState(code))
            assertTrue(result.match, "expected match for territory $code")
            assertEquals(code, result.state)
        }
    }

    // ── Clean negatives (match=false, reason=OK) ────────────────

    @Test
    fun `device in new york with only CA in list is clean negative`() = runTest {
        val result = isUSState(listOf("CA"), evaluate = inState("NY"))
        assertFalse(result.match)
        assertNull(result.state)
        assertEquals(PolicyReasonCode.OK, result.reason,
            "all regions returned a determinate NO — clean negative")
    }

    @Test
    fun `device outside US is clean negative`() = runTest {
        // Every us-state query returns NO.
        val result = isUSState(listOf("CA", "NY")) { verdict(OctetVerdict.Result.NO) }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.OK, result.reason)
    }

    // ── Indeterminate → unknown, not negative ───────────────────

    @Test
    fun `any indeterminate region with no match folds to unknown`() = runTest {
        // CA indeterminate, NY no → no YES, saw indeterminate → unknown.
        val result = isUSState(listOf("CA", "NY")) { region ->
            if (region == OctetRegion.usState("CA"))
                verdict(OctetVerdict.Result.INDETERMINATE, OctetVerdict.ReasonCode.NO_FIX)
            else verdict(OctetVerdict.Result.NO)
        }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.VERDICT_INDETERMINATE, result.reason,
            "match=false here means 'we cannot tell', not 'device is not in these states'")
    }

    @Test
    fun `a YES wins even if another region was indeterminate`() = runTest {
        // First CA indeterminate, then NY yes → match (YES short-circuits).
        val result = isUSState(listOf("CA", "NY")) { region ->
            if (region == OctetRegion.usState("CA"))
                verdict(OctetVerdict.Result.INDETERMINATE, OctetVerdict.ReasonCode.NO_FIX)
            else verdict(OctetVerdict.Result.YES)
        }
        assertTrue(result.match)
        assertEquals("NY", result.state)
    }

    // ── Input validation (programmer bugs → INPUT_INVALID) ──────

    @Test
    fun `empty states list returns input invalid`() = runTest {
        val result = isUSState(emptyList()) { error("must not query the SDK") }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.INPUT_INVALID, result.reason)
    }

    @Test
    fun `unknown state code returns input invalid`() = runTest {
        val result = isUSState(listOf("ZZ")) { error("must not query the SDK") }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.INPUT_INVALID, result.reason)
    }

    @Test
    fun `list with one invalid code returns input invalid`() = runTest {
        val result = isUSState(listOf("CA", "ZZ", "NY")) { error("must not query the SDK") }
        assertFalse(result.match)
        assertEquals(PolicyReasonCode.INPUT_INVALID, result.reason)
    }

    // ── Data file integrity ─────────────────────────────────────

    @Test
    fun `list has exactly 56 entries`() {
        assertEquals(56, UsStateList.entries.size)
    }

    @Test
    fun `list contains dc and all five territories`() {
        val codes = UsStateList.entries.map { it.code }.toSet()
        assertTrue(setOf("DC", "PR", "GU", "VI", "AS", "MP").all { it in codes },
            "states.json must include DC and all five territories")
    }

    @Test
    fun `list policy version matches package version`() {
        assertEquals(PolicyPackage.VERSION, UsStateList.file.policyVersion)
    }

    // ── Loader leniency parity with Swift ──────────────────────
    //
    // Mirrors `IsUSStateTests.swift`. states.json already carries a
    // top-level `source_note` audit field that the loader silently
    // ignores; this test fences that contract on both platforms.
    // Companion to the IsOfacComprehensiveTests version-bump. (Part
    // of the octet-sdks audit M13 lockstep fix.)

    @Test
    fun `decoder ignores unknown top-level keys including source_note`() {
        val withExtraTopLevel = """
            {
              "list_id": "us_iso_3166_2_subdivisions",
              "policy_version": "0.0.0-test",
              "effective_date": "2026-01-01",
              "source_note": "real top-level audit key already in states.json",
              "audit_note": "second unknown key, also ignored",
              "entries": []
            }
        """.trimIndent()
        val parsed = UsStateList.decode(withExtraTopLevel)
        assertEquals("us_iso_3166_2_subdivisions", parsed.listId)
        assertTrue(parsed.entries.isEmpty())
    }

    @Test
    fun `decoder ignores unknown per-entry keys`() {
        val withExtraInEntry = """
            {
              "list_id": "us_iso_3166_2_subdivisions",
              "policy_version": "0.0.0-test",
              "effective_date": "2026-01-01",
              "entries": [
                { "code": "CA", "name": "California", "kind": "state", "audit_internal": "ignored" }
              ]
            }
        """.trimIndent()
        val parsed = UsStateList.decode(withExtraInEntry)
        assertEquals(1, parsed.entries.size)
        assertEquals("CA", parsed.entries[0].code)
    }
}
