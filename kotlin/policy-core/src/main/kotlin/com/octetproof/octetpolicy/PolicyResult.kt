// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

import java.time.Instant

/**
 * The structured result returned by every policy predicate in this
 * repository.
 *
 * [match] is the headline answer, but **a caller cannot make a
 * compliance decision from [match] alone**: `match == false` paired
 * with `reason != OK` means *we couldn't tell*, which is not the
 * same as *the answer is no*.
 *
 * @property match The answer to the predicate's question. Always
 *   inspect [reason] alongside this.
 * @property country ISO 3166-1 alpha-2 derived from the verdict.
 *   `null` when the verdict carried no country (typically
 *   indeterminate).
 * @property state ISO 3166-2 subdivision code WITHOUT country prefix
 *   (e.g. `"CA"`, not `"US-CA"`). `null` when the predicate doesn't
 *   care about subdivisions or the verdict didn't surface one.
 * @property confidence All v1 predicates emit [Confidence.HIGH];
 *   the field exists from day one so introducing `MEDIUM` / `LOW`
 *   later is not a breaking API change.
 * @property policyVersion Semantic version of the policy package
 *   that produced this result — the audit-trail anchor.
 * @property policyName The predicate's name, e.g. `"isOfacComprehensive"`.
 * @property evaluatedAt Copy-through from `verdict.queriedAt`.
 * @property reason Structured reason. `OK` paired with [match] is
 *   an answer; any other reason is an "unknown" signal. See design
 *   doc §3.4.
 */
data class PolicyResult(
    val match: Boolean,
    val country: String? = null,
    val state: String? = null,
    val confidence: Confidence = Confidence.HIGH,
    val policyVersion: String,
    val policyName: String,
    val evaluatedAt: Instant = Instant.now(),
    val reason: PolicyReasonCode = PolicyReasonCode.OK,
)
