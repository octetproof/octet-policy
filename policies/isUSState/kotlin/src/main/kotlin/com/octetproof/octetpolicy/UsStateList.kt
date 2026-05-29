// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ─────────────────────────────────────────────────────────────────
// US subdivision list data types and loader
// ─────────────────────────────────────────────────────────────────
//
// Internal to the :is-us-state module. The predicate uses
// UsStateList.validCodes to validate caller input, and reads from
// states.json for the audit-friendly entries.

/**
 * One entry in the ISO 3166-2:US subdivision list. [kind] is
 * informational — the predicate treats states, the district, and
 * territories identically for matching purposes (the caller's
 * list is the source of truth for "what counts").
 */
@Serializable
internal data class UsStateEntry(
    /** ISO 3166-2 subdivision code WITHOUT the country prefix
     *  (e.g. `"CA"`, not `"US-CA"`). */
    val code: String,
    /** Human-readable name. Audit-only; not used for matching. */
    val name: String,
    /** `"state"` | `"district"` | `"territory"`. Audit-only. */
    val kind: String,
)

/** The whole `states.json` document. */
@Serializable
internal data class UsStateListFile(
    @SerialName("list_id") val listId: String,
    @SerialName("policy_version") val policyVersion: String,
    @SerialName("effective_date") val effectiveDate: String,
    val entries: List<UsStateEntry>,
)

/**
 * Loads `states.json` once at first access and caches it. Decoding
 * failure is a programmer error — see the equivalent comment in
 * [OfacList].
 */
internal object UsStateList {
    val file: UsStateListFile by lazy { loadFromClasspath() }
    val entries: List<UsStateEntry> get() = file.entries

    /**
     * Set of valid ISO 3166-2:US subdivision codes, without the
     * `"US-"` prefix. Used by the predicate to validate caller
     * input.
     */
    val validCodes: Set<String> by lazy { entries.map { it.code }.toSet() }

    // Lenient JSON: the on-disk states.json carries a `source_note`
    // field that we don't decode into a typed property. Mirrors
    // Swift's JSONDecoder behaviour (unknown keys silently ignored).
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadFromClasspath(): UsStateListFile {
        val resourceStream = UsStateList::class.java.classLoader
            .getResourceAsStream("states.json")
            ?: error("IsUSState: states.json missing from classpath. This is a build error.")
        val text = resourceStream.bufferedReader().use { it.readText() }
        return try {
            json.decodeFromString<UsStateListFile>(text)
        } catch (e: Exception) {
            error("IsUSState: states.json failed to decode: $e. This is a build error.")
        }
    }
}
