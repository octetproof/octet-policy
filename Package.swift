// swift-tools-version:5.9
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
import PackageDescription

// Predicates are built on the octet-sdk region-containment API:
// `loc.isWithin(region:) async -> OctetVerdict`. Each predicate is a
// thin composition — query the region(s) that define its question,
// interpret the verdict(s) into a `PolicyResult`.
//
// Consumer story (one product, one import, async calls):
//
//   .package(url: "...octetpolicy", from: "...")
//   .product(name: "OctetPolicy", package: "octetpolicy")
//
//   import OctetPolicy
//   let r = await isSingapore(sdk.loc)
//   let r = await isOfacComprehensive(sdk.loc)
//
// SDK dependency: the public binary xcframework from octet-sdk-ios.
// Pin to an exact tag — the alpha line may break compatibility across
// 0.0.x. iOS-only: the SDK ships no macOS slice, so neither do we.
let package = Package(
    name: "octetpolicy",
    platforms: [
        .iOS(.v16),     // OctetSDK requires iOS 16; no macOS slice exists
    ],
    products: [
        // Umbrella. The product's `targets:` lists every constituent
        // so the consumer's linker pulls them all in.
        .library(name: "OctetPolicy", targets: [
            "OctetPolicy",
            "OctetPolicyCore",
            "IsSingapore",
            "IsUS",
            "IsUSState",
            "IsOfacComprehensive",
        ]),

        // Granular products.
        .library(name: "OctetPolicyCore",     targets: ["OctetPolicyCore"]),
        .library(name: "IsSingapore",         targets: ["IsSingapore"]),
        .library(name: "IsUS",                targets: ["IsUS"]),
        .library(name: "IsUSState",           targets: ["IsUSState"]),
        .library(name: "IsOfacComprehensive", targets: ["IsOfacComprehensive"]),
    ],
    dependencies: [
        // Public SwiftPM URL — resolves the binary xcframework target.
        // Package identity is the repo basename, "octet-sdk-ios".
        .package(url: "https://github.com/octetproof/octet-sdk-ios", exact: "1.1.0"),
    ],
    targets: [
        .target(
            name: "OctetPolicyCore",
            path: "swift/Sources/OctetPolicyCore"
        ),
        .testTarget(
            name: "OctetPolicyCoreTests",
            dependencies: ["OctetPolicyCore"],
            path: "swift/Tests/OctetPolicyCoreTests"
        ),

        // ── Per-predicate targets ───────────────────────────────────
        // Each depends on OctetPolicyCore (PolicyResult etc.) and the
        // SDK's OctetSDK module (OctetLoc / OctetRegion / OctetVerdict).
        .target(
            name: "IsSingapore",
            dependencies: ["OctetPolicyCore", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isSingapore/swift",
            exclude: ["Tests"]
        ),
        .testTarget(
            name: "IsSingaporeTests",
            dependencies: ["IsSingapore", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isSingapore/swift/Tests"
        ),

        .target(
            name: "IsUS",
            dependencies: ["OctetPolicyCore", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isUS/swift",
            exclude: ["Tests"]
        ),
        .testTarget(
            name: "IsUSTests",
            dependencies: ["IsUS", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isUS/swift/Tests"
        ),

        .target(
            name: "IsOfacComprehensive",
            dependencies: ["OctetPolicyCore", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isOfacComprehensive",
            exclude: ["README.md", "CHANGELOG.md", "sources.md", "swift/Tests"],
            sources: ["swift"],
            resources: [.process("countries.json")]
        ),
        .testTarget(
            name: "IsOfacComprehensiveTests",
            dependencies: ["IsOfacComprehensive", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isOfacComprehensive/swift/Tests"
        ),

        .target(
            name: "IsUSState",
            dependencies: ["OctetPolicyCore", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isUSState",
            exclude: ["README.md", "CHANGELOG.md", "sources.md", "swift/Tests"],
            sources: ["swift"],
            resources: [.process("states.json")]
        ),
        .testTarget(
            name: "IsUSStateTests",
            dependencies: ["IsUSState", .product(name: "OctetSDK", package: "octet-sdk-ios")],
            path: "policies/isUSState/swift/Tests"
        ),

        // ── Umbrella ────────────────────────────────────────────────
        .target(
            name: "OctetPolicy",
            dependencies: [
                "OctetPolicyCore",
                "IsSingapore",
                "IsUS",
                "IsUSState",
                "IsOfacComprehensive",
                // Re-exported so a single `import OctetPolicy` also
                // surfaces the SDK API (Octet, OctetLoc, OctetRegion,
                // OctetVerdict) — mirrors the Kotlin aggregator's
                // `api(sdk)`.
                .product(name: "OctetSDK", package: "octet-sdk-ios"),
            ],
            path: "swift/Sources/OctetPolicy"
        ),
    ]
)
