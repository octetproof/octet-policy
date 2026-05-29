// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import XCTest
import OctetSDK
@testable import IsSingapore
@testable import OctetPolicyCore

/// Tests for `isSingapore` against the real region-containment model.
/// The predicate's testable core takes an `evaluate` closure, so each
/// test injects a canned `OctetVerdict` for the region the predicate
/// queries — no live SDK or license key needed.
final class IsSingaporeTests: XCTestCase {

    private let fixedTime = Date(timeIntervalSince1970: 1_716_000_000)

    // 0.0.1-alpha LocationProof has no default init; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private func fakeProof() -> LocationProof {
        LocationProof(
            id: "test", claimedRegion: .country(isoCode: "SG"),
            level: .country, timestampMs: 0,
            confidence: ConfidenceSummary(),
            positionCommitment: Data(), sdkVersion: "test",
            platform: "test", proofBytes: Data()
        )
    }

    /// Build a verdict honoring the SDK's invariant: `proof` is nil
    /// iff `result == .indeterminate`.
    private func verdict(
        _ result: OctetVerdict.Result,
        reason: OctetVerdict.ReasonCode = .ok
    ) -> OctetVerdict {
        let proof: LocationProof? = (result == .indeterminate) ? nil : fakeProof()
        return OctetVerdict(
            result: result,
            reason: reason,
            message: "test",
            proof: proof,
            validity: nil,
            queriedAt: fixedTime,
            confidence: ConfidenceSummary()
        )
    }

    func test_device_in_singapore_matches() async {
        // The SDK confirms containment in SG → match.
        let result = await isSingapore { region in
            XCTAssertEqual(region.shape, .country(isoCode: "SG"),
                           "isSingapore must query exactly the SG country region")
            return self.verdict(.yes)
        }

        XCTAssertTrue(result.match)
        XCTAssertEqual(result.country, "SG")
        XCTAssertNil(result.state)
        XCTAssertEqual(result.reason, .ok)
        XCTAssertEqual(result.confidence, .high)
        XCTAssertEqual(result.policyName, "isSingapore")
        XCTAssertEqual(result.policyVersion, PolicyPackage.version)
        XCTAssertEqual(result.evaluatedAt, fixedTime)
    }

    func test_device_not_in_singapore_is_clean_negative() async {
        let result = await isSingapore { _ in self.verdict(.no) }

        XCTAssertFalse(result.match)
        XCTAssertNil(result.country,
                     "a NO verdict doesn't reveal where the device is — country stays nil")
        XCTAssertEqual(result.reason, .ok)
    }

    func test_indeterminate_verdict_is_unknown_not_negative() async {
        let result = await isSingapore { _ in self.verdict(.indeterminate, reason: .noFix) }

        XCTAssertFalse(result.match)
        XCTAssertNil(result.country)
        XCTAssertEqual(result.reason, .verdictIndeterminate,
                       "match=false here means 'cannot tell', not 'device is not in SG'")
    }

    func test_not_yet_released_geometry_is_indeterminate() async {
        // If the SDK build can't evaluate the region yet it returns
        // INDETERMINATE / notYetReleased. The predicate folds that into
        // .verdictIndeterminate — still "unknown", never a false negative.
        let result = await isSingapore { _ in
            self.verdict(.indeterminate, reason: .notYetReleased)
        }

        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .verdictIndeterminate)
    }

    func test_evaluated_at_copies_through_from_verdict() async {
        let result = await isSingapore { _ in self.verdict(.yes) }
        XCTAssertEqual(result.evaluatedAt, fixedTime)
    }
}
