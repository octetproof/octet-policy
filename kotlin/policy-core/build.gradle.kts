// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
//
// policy-core: the shared types every Kotlin policy predicate
// returns (PolicyResult, Confidence, PolicyReasonCode) plus the
// PolicyPackage.VERSION constant that predicates stamp onto every
// result.
//
// Standalone pure-Kotlin/JVM module — PolicyResult doesn't reference
// any SDK type, so it stays JVM (the predicate modules that touch
// the octet-sdk AAR are Android libraries). Plugin version is
// declared once at the root build.gradle.kts; applied here without
// a version.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
