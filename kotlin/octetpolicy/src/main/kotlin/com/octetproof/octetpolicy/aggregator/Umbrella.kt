// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy.aggregator

/**
 * Marker for the `:octetpolicy` aggregator module. The module's
 * purpose is exposing the other predicate modules via transitive
 * `api(...)` dependencies — see the module's `build.gradle.kts`.
 *
 * Consumers depend on `com.octetproof:octetpolicy` and get access to:
 *
 * - `com.octetproof.octetpolicy.PolicyResult`, `Confidence`,
 *   `PolicyReasonCode`, `PolicyPackage`
 * - `com.octetproof.octetpolicy.isSingapore`, `isUS`, `isUSState`,
 *   `isOfacComprehensive` (all `suspend`, taking an `OctetLoc`)
 * - the octet-sdk API types (`com.octetproof.sdk.api.OctetLoc`,
 *   `OctetRegion`, `OctetVerdict`), transitively
 *
 * The library's semantic version is exposed at
 * [com.octetproof.octetpolicy.PolicyPackage.VERSION] — the single
 * source of truth, derived at build time from the root VERSION file.
 */
public object OctetPolicy
