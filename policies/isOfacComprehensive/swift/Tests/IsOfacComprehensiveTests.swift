// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import XCTest
import OctetSDK
@testable import IsOfacComprehensive
@testable import OctetPolicyCore

final class IsOfacComprehensiveTests: XCTestCase {

    private let fixedTime = Date(timeIntervalSince1970: 1_716_000_000)

    // 0.0.1-alpha LocationProof has no default init; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private func fakeProof() -> LocationProof {
        LocationProof(
            id: "test", claimedRegion: .country(isoCode: "IR"),
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

    func test_device_in_iran_matches_country_level() async {
        let result = await isOfacComprehensive { region in
            if region.shape == .country(isoCode: "IR") { return self.verdict(.yes) }
            return self.verdict(.no)
        }
        XCTAssertTrue(result.match)
        XCTAssertEqual(result.country, "IR")
        XCTAssertNil(result.state)
        XCTAssertEqual(result.reason, .ok)
        XCTAssertEqual(result.policyName, "isOfacComprehensive")
    }

    func test_device_in_crimea_matches_subdivision() async {
        let result = await isOfacComprehensive { region in
            if region.shape == .subdivision(isoCode: "UA-43") { return self.verdict(.yes) }
            return self.verdict(.no)
        }
        XCTAssertTrue(result.match)
        XCTAssertEqual(result.country, "UA")
        XCTAssertEqual(result.state, "43")
    }

    func test_device_in_non_sanctioned_region_is_clean_negative() async {
        let result = await isOfacComprehensive { _ in self.verdict(.no) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .ok)
    }

    func test_indeterminate_fails_closed() async {
        // SDK couldn't determine containment for the queried regions.
        // Sanctions predicate fails closed: unknown, never "not sanctioned".
        let result = await isOfacComprehensive { _ in self.verdict(.indeterminate, reason: .noFix) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .verdictIndeterminate)
    }

    func test_queries_all_six_sanctioned_regions_when_no_match() async {
        var queried: [OctetRegion.Shape] = []
        _ = await isOfacComprehensive { region in
            queried.append(region.shape)
            return self.verdict(.no)
        }
        XCTAssertEqual(queried.count, 6)
        XCTAssertTrue(queried.contains(.country(isoCode: "CU")))
        XCTAssertTrue(queried.contains(.country(isoCode: "IR")))
        XCTAssertTrue(queried.contains(.country(isoCode: "KP")))
        XCTAssertTrue(queried.contains(.subdivision(isoCode: "UA-43")))
        XCTAssertTrue(queried.contains(.subdivision(isoCode: "UA-14")))
        XCTAssertTrue(queried.contains(.subdivision(isoCode: "UA-09")))
    }

    func test_list_has_exactly_six_entries() {
        XCTAssertEqual(OfacList.entries.count, 6)
    }

    func test_list_policy_version_matches_package_version() {
        XCTAssertEqual(OfacList.file.policyVersion, PolicyPackage.version)
    }
}
