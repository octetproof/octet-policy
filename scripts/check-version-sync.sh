#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Understone, Inc.
#
# CI lint: fails if any hardcoded octetpolicy version reference drifts
# from the VERSION file. Runs `sync-version.sh` on a fresh copy of the
# affected files and compares — if `sync-version.sh` would change
# anything, the commit isn't in sync.
#
# Exit codes:
#   0  every version in sync
#   1  drift detected (prints the diff)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Pristine baseline: capture current state.
BASELINE="$(mktemp -d -t octetpolicy-version-baseline.XXXXXX)"
trap 'rm -rf "$BASELINE"' EXIT

# Files the sync script writes.
TOUCHED_FILES=(
    "swift/Sources/OctetPolicyCore/PolicyPackage.swift"
    "kotlin/policy-core/src/main/kotlin/com/octetproof/octetpolicy/PolicyPackage.kt"
    "policies/isOfacComprehensive/countries.json"
    "policies/isUSState/states.json"
    "swift/Tests/OctetPolicyCoreTests/PolicyResultTests.swift"
    "kotlin/policy-core/src/test/kotlin/com/octetproof/octetpolicy/PolicyResultTests.kt"
    "samples/android-sample/app/build.gradle.kts"
    "README.md"
    "policies/isOfacComprehensive/README.md"
)

for f in "${TOUCHED_FILES[@]}"; do
    mkdir -p "$BASELINE/$(dirname "$f")"
    cp "$REPO_ROOT/$f" "$BASELINE/$f"
done

bash "$REPO_ROOT/scripts/sync-version.sh" >/dev/null

DRIFT=0
for f in "${TOUCHED_FILES[@]}"; do
    if ! diff -q "$BASELINE/$f" "$REPO_ROOT/$f" >/dev/null; then
        echo "DRIFT: $f"
        diff -u "$BASELINE/$f" "$REPO_ROOT/$f" | sed 's/^/    /'
        # Restore the baseline so the lint doesn't leave dirty state.
        cp "$BASELINE/$f" "$REPO_ROOT/$f"
        DRIFT=1
    fi
done

if [ "$DRIFT" -eq 0 ]; then
    echo "All octetpolicy version references in sync with VERSION."
    exit 0
fi

echo
echo "ERROR: hardcoded version(s) drifted from VERSION."
echo "Fix: bash scripts/sync-version.sh && git add -p"
exit 1
