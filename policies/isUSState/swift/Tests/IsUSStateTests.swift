// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import XCTest
import OctetSDK
@testable import IsUSState
@testable import OctetPolicyCore

final class IsUSStateTests: XCTestCase {

    private let fixedTime = Date(timeIntervalSince1970: 1_716_000_000)

    // 0.0.1-alpha LocationProof has no default init; tests don't read
    // proof bytes, so synthesize a placeholder with arbitrary fields.
    private func fakeProof() -> LocationProof {
        LocationProof(
            id: "test", claimedRegion: .subdivision(isoCode: "US-CA"),
            level: .subdivision, timestampMs: 0,
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

    func test_device_in_listed_state_matches() async {
        let result = await isUSState(["CA"]) { region in
            if region.shape == .subdivision(isoCode: "US-CA") { return self.verdict(.yes) }
            return self.verdict(.no)
        }
        XCTAssertTrue(result.match)
        XCTAssertEqual(result.country, "US")
        XCTAssertEqual(result.state, "CA")
        XCTAssertEqual(result.reason, .ok)
        XCTAssertEqual(result.policyName, "isUSState")
    }

    func test_multi_state_list_matches_one() async {
        let result = await isUSState(["NY", "CA", "TX"]) { region in
            if region.shape == .subdivision(isoCode: "US-CA") { return self.verdict(.yes) }
            return self.verdict(.no)
        }
        XCTAssertTrue(result.match)
        XCTAssertEqual(result.state, "CA")
    }

    func test_device_in_no_listed_state_is_clean_negative() async {
        let result = await isUSState(["CA", "NY"]) { _ in self.verdict(.no) }
        XCTAssertFalse(result.match)
        XCTAssertNil(result.state)
        XCTAssertEqual(result.reason, .ok)
    }

    func test_empty_list_is_input_invalid() async {
        let result = await isUSState([]) { _ in self.verdict(.no) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .inputInvalid)
    }

    func test_unknown_code_is_input_invalid() async {
        let result = await isUSState(["ZZ"]) { _ in self.verdict(.no) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .inputInvalid)
    }

    func test_one_bad_code_poisons_the_whole_call() async {
        let result = await isUSState(["CA", "ZZ", "NY"]) { _ in self.verdict(.no) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .inputInvalid)
    }

    func test_indeterminate_fails_closed() async {
        let result = await isUSState(["CA"]) { _ in self.verdict(.indeterminate, reason: .noFix) }
        XCTAssertFalse(result.match)
        XCTAssertEqual(result.reason, .verdictIndeterminate,
                       "couldn't determine the subdivision — unknown, never a false negative")
    }

    func test_dc_and_territories_are_valid_codes() async {
        for code in ["DC", "PR", "GU", "VI", "AS", "MP"] {
            let result = await isUSState([code]) { region in
                if region.shape == .subdivision(isoCode: "US-\(code)") { return self.verdict(.yes) }
                return self.verdict(.no)
            }
            XCTAssertTrue(result.match, "expected match for US-\(code)")
            XCTAssertEqual(result.state, code)
        }
    }

    func test_list_has_exactly_56_entries() {
        XCTAssertEqual(UsStateList.entries.count, 56)
    }

    func test_list_policy_version_matches_package_version() {
        XCTAssertEqual(UsStateList.file.policyVersion, PolicyPackage.version)
    }

    // MARK: - Loader leniency parity with Kotlin
    //
    // Mirrors `IsUSStateTests.kt`. states.json already carries a
    // top-level `source_note` audit field that the loader silently
    // ignores; this test fences that contract on both platforms.
    // Companion to the IsOfacComprehensiveTests parity test. (Part
    // of the octet-sdks audit M13 lockstep fix.)

    func test_decoder_ignores_unknown_top_level_keys_including_source_note() throws {
        let json = """
        {
          "list_id": "us_iso_3166_2_subdivisions",
          "policy_version": "0.0.0-test",
          "effective_date": "2026-01-01",
          "source_note": "real top-level audit key already in states.json",
          "audit_note": "second unknown key, also ignored",
          "entries": []
        }
        """.data(using: .utf8)!
        let parsed = try UsStateList.decode(json)
        XCTAssertEqual(parsed.listId, "us_iso_3166_2_subdivisions")
        XCTAssertTrue(parsed.entries.isEmpty)
    }

    func test_decoder_ignores_unknown_per_entry_keys() throws {
        let json = """
        {
          "list_id": "us_iso_3166_2_subdivisions",
          "policy_version": "0.0.0-test",
          "effective_date": "2026-01-01",
          "entries": [
            { "code": "CA", "name": "California", "kind": "state", "audit_internal": "ignored" }
          ]
        }
        """.data(using: .utf8)!
        let parsed = try UsStateList.decode(json)
        XCTAssertEqual(parsed.entries.count, 1)
        XCTAssertEqual(parsed.entries.first?.code, "CA")
    }
}
