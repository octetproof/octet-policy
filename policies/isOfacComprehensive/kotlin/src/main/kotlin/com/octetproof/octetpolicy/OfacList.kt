// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ─────────────────────────────────────────────────────────────────
// OFAC list data types and loader
// ─────────────────────────────────────────────────────────────────
//
// Internal to the :is-ofac-comprehensive module. The predicate
// reads from OfacList.entries. The JSON file (countries.json) is
// the authoritative source — these types are just the in-memory
// shape for it. Same file the Swift IsOfacComprehensive target
// reads via Bundle.module.

/**
 * One entry in the OFAC comprehensive list. Either country-level
 * ([subdivision] is null) or subdivision-level ([subdivision] is
 * non-null).
 */
@Serializable
internal data class OfacEntry(
    /** ISO 3166-1 alpha-2 country code (e.g. `"CU"`, `"UA"`). */
    @SerialName("iso_3166_1") val iso3166_1: String,
    /** Human-readable name. Audit-only; the predicate does not
     *  match on this field. */
    val name: String,
    /** ISO 3166-2 subdivision code WITHOUT the country prefix
     *  (e.g. `"43"` for Crimea, not `"UA-43"`). Null for
     *  country-level entries. */
    val subdivision: String? = null,
    /** Primary-source URL for this entry. Audit-only. */
    @SerialName("source_url") val sourceUrl: String,
    /** ISO 8601 date this entry was added to the list. Audit-only. */
    @SerialName("added_on") val addedOn: String,
    /** Name of the compliance reviewer who signed off. Audit-only. */
    @SerialName("reviewed_by") val reviewedBy: String,
)

/**
 * The whole `countries.json` document. The top-level fields
 * ([listId], [policyVersion], [effectiveDate]) are metadata about
 * the list itself; they are loaded but not used at runtime by the
 * predicate. Tests pin them.
 */
@Serializable
internal data class OfacListFile(
    @SerialName("list_id") val listId: String,
    @SerialName("policy_version") val policyVersion: String,
    @SerialName("effective_date") val effectiveDate: String,
    val entries: List<OfacEntry>,
)

/**
 * Loads `countries.json` once at first access and caches it.
 *
 * Decoding failure is a programmer error (the file ships *with*
 * the SDK), so we throw [IllegalStateException]. Silently falling
 * back to an empty list would cause every OFAC check to return
 * `match=false` — exactly the wrong direction to fail in.
 */
internal object OfacList {
    val file: OfacListFile by lazy { loadFromClasspath() }
    val entries: List<OfacEntry> get() = file.entries

    private fun loadFromClasspath(): OfacListFile {
        val resourceStream = OfacList::class.java.classLoader
            .getResourceAsStream("countries.json")
            ?: error("IsOfacComprehensive: countries.json missing from classpath. This is a build error.")
        val text = resourceStream.bufferedReader().use { it.readText() }
        return try {
            Json.decodeFromString<OfacListFile>(text)
        } catch (e: Exception) {
            error("IsOfacComprehensive: countries.json failed to decode: $e. This is a build error.")
        }
    }
}
