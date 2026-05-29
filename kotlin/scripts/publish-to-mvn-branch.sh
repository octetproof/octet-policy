#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Understone, Inc.
#
# Pushes the locally-published Maven artifacts (under
# kotlin/build/mvn-repo/) to the `mvn-repo` branch on origin.
#
# Called by .github/workflows/release-kotlin.yml after tests and
# publication succeed. Can also be run locally with the right
# credentials (set REMOTE_URL or rely on the default GH_TOKEN
# env var the workflow provides).
#
# Args:
#   $1  version (e.g. "0.0.1-alpha") for the commit message.
#
# Exit codes:
#   0  push succeeded, or no changes to publish (same version)
#   1  bad args, missing artifacts, or git failure

set -euo pipefail

VERSION="${1:?missing version arg (e.g. 0.0.1-alpha)}"

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOCAL_REPO="$REPO_ROOT/kotlin/build/mvn-repo"
WORK_DIR="$(mktemp -d -t octet-mvn-repo-push.XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

if [ ! -d "$LOCAL_REPO" ] || [ -z "$(ls -A "$LOCAL_REPO" 2>/dev/null)" ]; then
    echo "ERROR: no artifacts at $LOCAL_REPO"
    echo "Run \`./gradlew publishAllPublicationsToLocalMvnRepoRepository\` from kotlin/ first."
    exit 1
fi

# REMOTE_URL can be overridden for local runs. In CI, GH_TOKEN comes
# from the workflow's permissions.contents-write GitHub Actions
# token. The default uses x-access-token (the GitHub Actions
# convention for token-based HTTPS auth).
REMOTE_URL="${REMOTE_URL:-https://x-access-token:${GH_TOKEN:-}@github.com/octetproof/octet-policy.git}"

echo "==> Cloning mvn-repo branch..."
git clone --branch mvn-repo --single-branch --depth 1 "$REMOTE_URL" "$WORK_DIR/mvn-repo"

echo "==> Copying $VERSION artifacts from $LOCAL_REPO..."
# cp -R (portable on macOS+Linux); rsync would be marginally faster
# but isn't always present on minimal runners.
cp -R "$LOCAL_REPO"/* "$WORK_DIR/mvn-repo/"

cd "$WORK_DIR/mvn-repo"
git config user.email "ci@octetproof.com"
git config user.name  "Octet CI"
git add .

if git diff --cached --quiet; then
    echo "==> No artifact changes — version $VERSION already on mvn-repo branch. Done."
    exit 0
fi

echo "==> Committing..."
git commit -m "Publish octetpolicy $VERSION"

echo "==> Pushing to origin/mvn-repo..."
git push origin mvn-repo

echo ""
echo "==> Artifacts for $VERSION pushed to mvn-repo branch."
echo "    Browse at:"
echo "    https://github.com/octetproof/octet-policy/tree/mvn-repo/com/octetproof"
