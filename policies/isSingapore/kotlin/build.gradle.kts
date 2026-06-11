// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
//
// :is-singapore — Android library. Consumes the octet-sdk AAR
// (OctetLoc / OctetRegion / OctetVerdict), so it can't be a plain
// kotlin("jvm") module. policy-core (PolicyResult etc.) stays JVM.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.octetproof.octetpolicy.singapore"
    compileSdk = 34
    defaultConfig {
        minSdk = 30   // matches the octet-sdk AAR's minSdk
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
    implementation(project(":policy-core"))
    // `api` so consumers of this module see OctetLoc / OctetRegion /
    // OctetVerdict (needed to name sdk.loc at the call site).
    api("com.octetproof:sdk:1.0.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
