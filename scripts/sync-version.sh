#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Understone, Inc.
#
# Propagates the version in the repo-root VERSION file to every other
# place octetpolicy's own version is hardcoded. Run this after bumping
# VERSION; commit the resulting diff.
#
# Does NOT touch the octet-sdk dependency pin — that's an upstream
# coordinate, independent of our release line.
#
# Exit codes:
#   0  ok (whether or not anything actually changed)
#   1  VERSION file unreadable or empty
#
# Usage:
#   bash scripts/sync-version.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION_FILE="$REPO_ROOT/VERSION"

if [ ! -f "$VERSION_FILE" ]; then
    echo "ERROR: $VERSION_FILE not found." >&2
    exit 1
fi

VERSION="$(tr -d '[:space:]' < "$VERSION_FILE")"
if [ -z "$VERSION" ]; then
    echo "ERROR: $VERSION_FILE is empty." >&2
    exit 1
fi

# Portable in-place sed (BSD on macOS needs the -i '' empty-suffix).
sed_i() {
    if sed --version >/dev/null 2>&1; then
        sed -i -E "$@"
    else
        sed -i '' -E "$@"
    fi
}

# 1. swift/Sources/OctetPolicyCore/PolicyPackage.swift
sed_i "s/(public static let version = ).*/\1\"${VERSION}\"/" \
    "$REPO_ROOT/swift/Sources/OctetPolicyCore/PolicyPackage.swift"

# 2. kotlin/policy-core/.../PolicyPackage.kt
sed_i "s/(const val VERSION: String = ).*/\1\"${VERSION}\"/" \
    "$REPO_ROOT/kotlin/policy-core/src/main/kotlin/com/octetproof/octetpolicy/PolicyPackage.kt"

# 3. countries.json + states.json `policy_version` field
for f in \
    "$REPO_ROOT/policies/isOfacComprehensive/countries.json" \
    "$REPO_ROOT/policies/isUSState/states.json"
do
    sed_i "s/(\"policy_version\": )\"[^\"]*\"/\1\"${VERSION}\"/" "$f"
done

# 4. Version-pinned tests (Swift + Kotlin)
sed_i "s/(XCTAssertEqual\(PolicyPackage\.version, )\"[^\"]*\"\)/\1\"${VERSION}\")/" \
    "$REPO_ROOT/swift/Tests/OctetPolicyCoreTests/PolicyResultTests.swift"
sed_i "s/(assertEquals\()\"[^\"]*\"(, PolicyPackage\.VERSION)/\1\"${VERSION}\"\2/" \
    "$REPO_ROOT/kotlin/policy-core/src/test/kotlin/com/octetproof/octetpolicy/PolicyResultTests.kt"

# 5. Android sample's octetpolicy dependency pin.
sed_i "s|(implementation\(\"com\.octetproof:octetpolicy:)[^\"]*(\"\))|\1${VERSION}\2|" \
    "$REPO_ROOT/samples/android-sample/app/build.gradle.kts"

# 6. Top-level README installation snippets + Status line.
sed_i "s|(\.package\(url: \"https://github.com/octetproof/octet-policy\", exact: )\"[^\"]*\"|\1\"${VERSION}\"|" \
    "$REPO_ROOT/README.md"
sed_i "s|(implementation\(\"com\.octetproof:octetpolicy:)[^\"]*(\"\))|\1${VERSION}\2|" \
    "$REPO_ROOT/README.md"
sed_i "s|(currently \`)[^\`]+(\`)|\1${VERSION}\2|" \
    "$REPO_ROOT/README.md"

# 7. isOfacComprehensive README's "package version" header.
sed_i "s|(## Current list — effective [0-9-]+, package version )[^ ]+$|\1${VERSION}|" \
    "$REPO_ROOT/policies/isOfacComprehensive/README.md"

# 8. Android sample's versionName (display version in Settings → Apps).
sed_i "s|(versionName = )\"[^\"]*\"|\1\"${VERSION}\"|" \
    "$REPO_ROOT/samples/android-sample/app/build.gradle.kts"

echo "Synced VERSION=${VERSION} into all octetpolicy version references."
