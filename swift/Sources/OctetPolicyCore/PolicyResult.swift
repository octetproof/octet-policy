// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation

/// The structured result returned by every policy predicate in this
/// repository.
///
/// `match` is the headline answer, but **a caller cannot make a
/// compliance decision from `match` alone**: `match == false` paired
/// with `reason != .ok` means *we couldn't tell*, which is not the
/// same as *the answer is no*.
public struct PolicyResult: Equatable {

    /// The answer to the predicate's question. Always inspect
    /// `reason` alongside this — see the type doc.
    public let match: Bool

    /// ISO 3166-1 alpha-2 country code derived from the verdict.
    /// `nil` when the verdict carried no country (typically because
    /// the verdict was `.indeterminate`).
    public let country: String?

    /// ISO 3166-2 subdivision code *without* country prefix
    /// (e.g. `"CA"`, not `"US-CA"`). `nil` when the predicate
    /// doesn't care about subdivisions or the verdict didn't surface
    /// one.
    public let state: String?

    /// All v1 predicates emit `.high`. The field exists from day one
    /// so introducing `.medium` / `.low` later is not a breaking API
    /// change.
    public let confidence: Confidence

    /// Semantic version of the policy package that produced this
    /// result. The audit-trail anchor: pull the matching tagged
    /// release to see the exact data file used.
    public let policyVersion: String

    /// The predicate's name, e.g. `"isOfacComprehensive"`.
    public let policyName: String

    /// Copy-through from `verdict.queriedAt`. The *when* of the
    /// verdict that drove this result.
    public let evaluatedAt: Date

    /// Structured reason. `.ok` paired with `match` is an answer;
    /// any other reason is an "unknown" signal.
    public let reason: PolicyReasonCode

    public init(
        match: Bool,
        country: String? = nil,
        state: String? = nil,
        confidence: Confidence = .high,
        policyVersion: String,
        policyName: String,
        evaluatedAt: Date = Date(),
        reason: PolicyReasonCode = .ok
    ) {
        self.match = match
        self.country = country
        self.state = state
        self.confidence = confidence
        self.policyVersion = policyVersion
        self.policyName = policyName
        self.evaluatedAt = evaluatedAt
        self.reason = reason
    }
}
