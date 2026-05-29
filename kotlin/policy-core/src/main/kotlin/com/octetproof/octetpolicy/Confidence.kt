// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

package com.octetproof.octetpolicy

/**
 * Confidence level on a [PolicyResult]. All v1 predicates emit
 * [HIGH]; [MEDIUM] and [LOW] are reserved for future use.
 */
enum class Confidence {
    HIGH,
    MEDIUM,
    LOW,
}
