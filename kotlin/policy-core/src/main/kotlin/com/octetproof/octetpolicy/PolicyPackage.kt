// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

/**
 * Single source of truth for the policy package's version. Every
 * [PolicyResult.policyVersion] reads from [PolicyPackage.VERSION].
 *
 * **Must be kept in sync with the root `VERSION` file** at every
 * release. The CI version-sync lint enforces this (see
 * `scripts/check-version-sync.sh`).
 *
 * Kotlin's [VERSION] is uppercase per the `const val` convention;
 * the Swift equivalent is `PolicyPackage.version` (lowercase per
 * Swift's `static let` convention). Different accessor names; same
 * string value flows into every result's `policyVersion` field.
 */
object PolicyPackage {
    /** Semantic version of this policy package. Matches the root
     *  `VERSION` file. */
    const val VERSION: String = "0.0.1-alpha"
}
