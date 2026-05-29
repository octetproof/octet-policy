// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import XCTest
@testable import OctetPolicyCore

/// These tests pin the **audit-record contract** on `PolicyResult`.
/// The result struct is what compliance officers look at when
/// reconstructing "what did our predicate match against on date X" —
/// its defaults and invariants matter for that flow, not just for
/// type-safety.
final class PolicyResultTests: XCTestCase {

    func test_default_result_for_yes_match_carries_audit_fields() {
        let when = Date(timeIntervalSince1970: 1_716_000_000)
        let result = PolicyResult(
            match: true,
            country: "US",
            state: "CA",
            policyVersion: "0.0.1",
            policyName: "isUSState",
            evaluatedAt: when
        )

        XCTAssertTrue(result.match)
        XCTAssertEqual(result.country, "US")
        XCTAssertEqual(result.state, "CA")
        XCTAssertEqual(result.confidence, .high,
            "v1 predicates always emit .high")
        XCTAssertEqual(result.reason, .ok)
        XCTAssertEqual(result.policyVersion, "0.0.1")
        XCTAssertEqual(result.policyName, "isUSState")
        XCTAssertEqual(result.evaluatedAt, when)
    }

    func test_match_false_with_state_missing_means_unknown_not_negative() {
        // The audit-trail invariant. A caller that reads
        // `result.match` alone and treats `false` as "device is not
        // in CA" is misusing the API.
        let result = PolicyResult(
            match: false,
            policyVersion: "0.0.1",
            policyName: "isUSState",
            reason: .verdictIndeterminate
        )

        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .verdictIndeterminate,
            "match=false paired with .verdictIndeterminate means 'unknown', not 'negative'")
    }

    func test_confidence_round_trips_through_raw_value() {
        XCTAssertEqual(Confidence.high.rawValue, "HIGH")
        XCTAssertEqual(Confidence(rawValue: "MEDIUM"), .medium)
        XCTAssertEqual(Confidence(rawValue: "LOW"), .low)
    }

    func test_policy_reason_code_round_trips() {
        XCTAssertEqual(PolicyReasonCode.ok.rawValue, "OK")
        XCTAssertEqual(PolicyReasonCode(rawValue: "VERDICT_INDETERMINATE"),
                       .verdictIndeterminate)
        XCTAssertEqual(PolicyReasonCode(rawValue: "INPUT_INVALID"),
                       .inputInvalid)
    }

    func test_policy_package_version_is_pinned() {
        // Brittle on purpose: if VERSION is bumped, this test fails
        // until PolicyPackage.version is bumped too. The CI
        // version-sync lint provides a stricter parity check across
        // every place the version is hardcoded.
        XCTAssertEqual(PolicyPackage.version, "0.0.1-alpha")
    }

    func test_equality_when_all_fields_match() {
        let when = Date(timeIntervalSince1970: 1_716_000_000)
        let a = PolicyResult(
            match: true,
            country: "SG",
            policyVersion: "0.0.1",
            policyName: "isSingapore",
            evaluatedAt: when
        )
        let b = PolicyResult(
            match: true,
            country: "SG",
            policyVersion: "0.0.1",
            policyName: "isSingapore",
            evaluatedAt: when
        )
        XCTAssertEqual(a, b)
    }

    func test_inequality_when_match_differs() {
        let when = Date(timeIntervalSince1970: 1_716_000_000)
        let a = PolicyResult(match: true,  country: "SG",
                             policyVersion: "0.0.1", policyName: "isSingapore",
                             evaluatedAt: when)
        let b = PolicyResult(match: false, country: "SG",
                             policyVersion: "0.0.1", policyName: "isSingapore",
                             evaluatedAt: when)
        XCTAssertNotEqual(a, b)
    }
}
