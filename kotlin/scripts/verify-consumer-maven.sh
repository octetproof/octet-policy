#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Understone, Inc.
#
# Verifies the octetpolicy Kotlin artifacts work from a clean consumer:
# publishes them to the local Maven repo, stands up a throwaway ANDROID
# library project at a temp path that depends on
# com.octetproof:octetpolicy:VERSION, and compiles a Kotlin file that
# imports every predicate.
#
# This is a COMPILE + RESOLVE check, not a behavioural one. The
# predicates are `suspend` and take a live `OctetLoc` from a running SDK
# (device sensors + license key), which a CI/headless consumer can't
# provide — so we prove the artifact resolves (including the octet-sdk
# AAR transitively) and the public API is reachable. Behaviour is
# covered by the unit tests.
#
# The consumer is an Android library, not a kotlin("jvm") project,
# because the predicate modules are Android libraries (AARs) — a plain
# JVM project can't consume them.
#
# Requires:
#   - ANDROID_HOME / ANDROID_SDK_ROOT set (Android library build)
#   - internet access — the octet-sdk AAR (com.octetproof:sdk +
#     com.octetproof:libpf) is fetched from the public octet-sdk-android
#     mvn-repo branch.
#
# Re-runnable; the throwaway consumer is NOT committed.
#
# Exit codes:
#   0  consumer resolved + compiled
#   1  build failure, missing artifact, or missing ANDROID_HOME

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
KOTLIN_ROOT="$REPO_ROOT/kotlin"
MVN_REPO="$KOTLIN_ROOT/build/mvn-repo"
VERSION="$(tr -d '[:space:]' < "$REPO_ROOT/VERSION")"
WORK_DIR="$(mktemp -d -t octetpolicy-maven-consumer.XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
    echo "ERROR: ANDROID_HOME (or ANDROID_SDK_ROOT) must be set — the consumer is an Android library." >&2
    exit 1
fi

echo "==> Publishing octetpolicy to local Maven repo: $MVN_REPO"
(cd "$KOTLIN_ROOT" && ./gradlew publishAllPublicationsToLocalMvnRepoRepository --no-daemon -q)

echo "==> Creating throwaway Android-library consumer at: $WORK_DIR"
echo "    Depending on com.octetproof:octetpolicy:$VERSION"

mkdir -p "$WORK_DIR/consumer/src/main/kotlin/com/example/consumer"

# Reuse this repo's Gradle wrapper so the consumer runs with a known-good
# Gradle version without needing a system gradle.
cp -R "$KOTLIN_ROOT/gradle" "$WORK_DIR/gradle"
cp "$KOTLIN_ROOT/gradlew" "$WORK_DIR/gradlew"
chmod +x "$WORK_DIR/gradlew"

cat > "$WORK_DIR/settings.gradle.kts" <<EOF
rootProject.name = "ConsumerSmokeTest"
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("file://$MVN_REPO") }
        // Public octet-sdk-android mvn-repo: serves com.octetproof:sdk
        // and com.octetproof:libpf transitively.
        maven { url = uri("https://raw.githubusercontent.com/octetproof/octet-sdk-android/mvn-repo") }
    }
}
include(":consumer")
EOF

cat > "$WORK_DIR/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
}
EOF

cat > "$WORK_DIR/gradle.properties" <<'EOF'
android.useAndroidX=true
android.nonTransitiveRClass=true
EOF

cat > "$WORK_DIR/consumer/build.gradle.kts" <<EOF
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.consumer"
    compileSdk = 34
    defaultConfig { minSdk = 30 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    // The umbrella artifact ONLY. It pulls in every predicate, the
    // shared types, and (transitively) the octet-sdk API.
    implementation("com.octetproof:octetpolicy:$VERSION")
}
EOF

cat > "$WORK_DIR/consumer/src/main/kotlin/com/example/consumer/Consumer.kt" <<'EOF'
package com.example.consumer

import com.octetproof.octetpolicy.PolicyPackage
import com.octetproof.octetpolicy.PolicyResult
import com.octetproof.octetpolicy.isOfacComprehensive
import com.octetproof.octetpolicy.isSingapore
import com.octetproof.octetpolicy.isUS
import com.octetproof.octetpolicy.isUSState
import com.octetproof.sdk.api.OctetLoc

// Compile-only proof that a clean consumer can, from the single
// octetpolicy artifact, see every predicate, the shared types, and
// (transitively) the SDK's OctetLoc needed to call them. The predicates
// need a live OctetLoc (device sensors + license key), so they can't run
// here — behaviour is covered by the unit tests.
@Suppress("unused")
internal suspend fun apiSurfaceCheck(loc: OctetLoc): List<PolicyResult> {
    check(PolicyPackage.VERSION.isNotBlank())
    return listOf(
        isSingapore(loc),
        isUS(loc),
        isUSState(loc, listOf("CA", "NY")),
        isOfacComprehensive(loc),
    )
}
EOF

echo "==> Resolving and compiling consumer..."
cd "$WORK_DIR"
./gradlew :consumer:assembleDebug --no-daemon

echo ""
echo "==> Consumer Maven verification: PASSED (resolved + compiled)"
