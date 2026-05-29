#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Understone, Inc.
#
# Verifies the octetpolicy Swift package works from a clean consumer:
# creates a throwaway SPM package at $TMPDIR, depends on this repo via
# `.package(path: ...)`, and compiles a target that imports OctetPolicy
# and references every predicate.
#
# This is a COMPILE + RESOLVE check, not a behavioural one. The
# predicates take a live `OctetLoc` from a running SDK (device sensors +
# license key), which a CI/headless consumer can't provide — so we prove
# the artifact resolves and the public API (predicates + the re-exported
# SDK types needed to call them) is reachable from a single
# `import OctetPolicy`. Behaviour is covered by the unit tests.
#
# Requires xcodebuild (i.e. a macOS host) — the octet-sdk xcframework is
# iOS-only, so the consumer can only build for iOS. We use a generic iOS
# destination so no simulator needs to boot.
#
# Re-runnable; the throwaway consumer is NOT committed.
#
# Exit codes:
#   0  consumer resolved + compiled
#   1  resolve/build failure

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# A path dependency's package identity is the directory basename, not
# the manifest `name`. Derive it so this works regardless of what the
# checkout directory is called.
PKG_NAME="$(basename "$REPO_ROOT")"
WORK_DIR="$(mktemp -d -t octetpolicy-spm-consumer.XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "==> Creating throwaway consumer at: $WORK_DIR"
echo "    Depending on package at:        $REPO_ROOT"

mkdir -p "$WORK_DIR/Sources/SmokeTest"

cat > "$WORK_DIR/Package.swift" <<EOF
// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "ConsumerSmokeTest",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "SmokeTest", targets: ["SmokeTest"]),
    ],
    dependencies: [
        .package(path: "$REPO_ROOT"),
    ],
    targets: [
        // Depend on the umbrella product ONLY. A single
        // \`import OctetPolicy\` then exposes every predicate plus the
        // shared types AND the re-exported SDK API. This is the
        // canonical consumer setup — see README.md.
        .target(
            name: "SmokeTest",
            dependencies: [
                .product(name: "OctetPolicy", package: "$PKG_NAME"),
            ]
        ),
    ]
)
EOF

cat > "$WORK_DIR/Sources/SmokeTest/SmokeTest.swift" <<'EOF'
import OctetPolicy

// Compile-only proof that a clean consumer can, from one import:
//   • see the shared types (PolicyResult, PolicyPackage)
//   • see every predicate
//   • see the SDK types needed to call them (OctetLoc, re-exported
//     from OctetSDK by the umbrella)
//
// We can't run the predicates here — they need a live OctetLoc (device
// sensors + license key) — so this references them in an unused async
// function purely to type-check the public surface. The unit tests in
// policies/*/swift cover behaviour.
@discardableResult
public func _apiSurfaceCheck(_ loc: OctetLoc) async -> [PolicyResult] {
    [
        await isSingapore(loc),
        await isUS(loc),
        await isUSState(loc, ["CA", "NY"]),
        await isOfacComprehensive(loc),
    ]
}

public let _smokeTestVersion = PolicyPackage.version
EOF

echo "==> Resolving and building consumer (xcodebuild, generic iOS)..."
cd "$WORK_DIR"
# SwiftPM auto-generates a scheme matching the package name.
xcodebuild build \
    -scheme ConsumerSmokeTest \
    -destination 'generic/platform=iOS Simulator' \
    -skipPackagePluginValidation \
    CODE_SIGNING_ALLOWED=NO

echo ""
echo "==> Consumer SPM verification: PASSED (resolved + compiled)"
