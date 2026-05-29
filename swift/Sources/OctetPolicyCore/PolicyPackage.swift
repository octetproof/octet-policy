// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation

/// Single source of truth for the policy package's version. Every
/// `PolicyResult.policyVersion` reads from `PolicyPackage.version`.
///
/// **Must be kept in sync with the root `VERSION` file** at every
/// release. The CI version-sync lint enforces this (see
/// `scripts/check-version-sync.sh`).
public enum PolicyPackage {
    /// Semantic version of this policy package. Matches the root
    /// `VERSION` file.
    public static let version = "0.0.1-alpha"
}
