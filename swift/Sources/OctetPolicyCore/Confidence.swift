// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation

/// Confidence level on a `PolicyResult`. All v1 predicates emit
/// `.high`; `.medium` and `.low` are reserved for future use.
public enum Confidence: String, Codable, Equatable {
    case high   = "HIGH"
    case medium = "MEDIUM"
    case low    = "LOW"
}
