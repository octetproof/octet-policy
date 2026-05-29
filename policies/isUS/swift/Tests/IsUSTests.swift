// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import XCTest
import OctetSDK
@testable import IsUS
@testable import OctetPolicyCore

final class IsUSTests: XCTestCase {

    private let fixedTime = Date(timeIntervalSince1970: 1_716_000_000)

    // 0.0.1-alpha LocationProof has no default init; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private func fakeProof() -> LocationProof {
        LocationProof(
            id: "test", claimedRegion: .country(isoCode: "US"),
            level: .country, timestampMs: 0,
            confidence: ConfidenceSummary(),
            positionCommitment: Data(), sdkVersion: "test",
            platform: "test", proofBytes: Data()
        )
    }

    private func verdict(
        _ result: OctetVerdict.Result,
        reason: OctetVerdict.ReasonCode = .ok
    ) -> OctetVerdict {
        let proof: LocationProof? = (result == .indeterminate) ? nil : fakeProof()
        return OctetVerdict(
            result: result, reason: reason, message: "test",
            proof: proof, validity: nil, queriedAt: fixedTime,
            confidence: ConfidenceSummary()
        )
    }

    func test_device_in_us_matches() async {
        let result = await isUS { region in
            XCTAssertEqual(region.shape, .country(isoCode: "US"),
                           "isUS must query the US country region")
            return self.verdict(.yes)
        }
        XCTAssertTrue(result.match)
        XCTAssertEqual(result.country, "US")
        XCTAssertNil(result.state)
        XCTAssertEqual(result.reason, .ok)
        XCTAssertEqual(result.policyName, "isUS")
        XCTAssertEqual(result.policyVersion, PolicyPackage.version)
    }

    func test_device_outside_us_is_clean_negative() async {
        // A device in a territory (PR, GU, …) or any other country
        // yields .no from .country("US") — the SDK handles the
        // territory exclusion. Clean negative, reason .ok.
        let result = await isUS { _ in self.verdict(.no) }
        XCTAssertFalse(result.match)
        XCTAssertNil(result.country)
        XCTAssertEqual(result.reason, .ok)
    }

    func test_indeterminate_is_unknown_not_negative() async {
        let result = await isUS { _ in self.verdict(.indeterminate, reason: .noFix) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .verdictIndeterminate)
    }
}
