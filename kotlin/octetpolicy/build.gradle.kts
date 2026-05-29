// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
//
// :octetpolicy — the umbrella aggregator. Consumers add
//
//     implementation("com.octetproof:octetpolicy:VERSION")
//
// and get every v1 predicate (isSingapore, isUS, isUSState,
// isOfacComprehensive) plus the shared types (PolicyResult,
// Confidence, PolicyReasonCode) and — transitively, via the predicate
// modules — the octet-sdk API types (OctetLoc, OctetRegion,
// OctetVerdict) needed to name `sdk.loc` at the call site.
//
// It is an Android library because the predicate modules it
// aggregates are Android libraries (they consume the octet-sdk AAR).
//
// Per-predicate Maven coordinates are also published; consumers who
// want just one predicate can depend on
// `com.octetproof:is-singapore:VERSION` (etc.) directly.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.octetproof.octetpolicy.aggregator"
    compileSdk = 34
    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    publishing {
        singleVariant("release")
    }
}

dependencies {
    // api(...) so consumers see these transitively. implementation
    // would hide them.
    api(project(":policy-core"))
    api(project(":is-singapore"))
    api(project(":is-us"))
    api(project(":is-ofac-comprehensive"))
    api(project(":is-us-state"))
}
