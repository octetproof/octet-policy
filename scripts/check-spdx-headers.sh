#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Understone, Inc.
#
# Asserts every Swift / Kotlin source and build script in the repo
# carries the `SPDX-License-Identifier: Apache-2.0` header in its
# first 3 lines. Run by .github/workflows/ci.yml on every PR.
#
# Exit codes:
#   0  every source file has the header
#   1  one or more files missing the header

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

total=0
bad=0
missing=""

# Walk all .swift / .kt / .kts files outside build & cache dirs.
# Use `while read` over process substitution so the loop runs in
# the current shell (variables stay visible). Avoid `mapfile`
# (bash 4+) so this also runs on stock macOS bash 3.2.
while IFS= read -r f; do
    total=$((total + 1))
    if ! head -3 "$f" | grep -q "SPDX-License-Identifier: Apache-2.0"; then
        bad=$((bad + 1))
        missing="${missing}  ${f}"$'\n'
    fi
done < <(find . -type f \
    \( -name "*.swift" -o -name "*.kt" -o -name "*.kts" \) \
    -not -path './.build/*' \
    -not -path '*/.gradle/*' \
    -not -path '*/build/*' \
    -not -path '*/.kotlin/*' \
    -not -path '*/.idea/*' \
    -not -path './samples/android-sample/gradle/wrapper/*' \
    -not -path '*/xcuserdata/*' \
    | sort)

if [ "$bad" -gt 0 ]; then
    echo "FAILED: $bad of $total source file(s) missing the SPDX-License-Identifier header:"
    printf '%s' "$missing"
    echo ""
    echo "Every Swift / Kotlin / Gradle Kotlin DSL source file must start with:"
    echo "  // SPDX-License-Identifier: Apache-2.0"
    echo "  // Copyright 2026 Understone, Inc."
    exit 1
fi

echo "OK: all $total source files carry the SPDX-License-Identifier header"
