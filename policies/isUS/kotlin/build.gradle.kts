// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
//
// :is-us — Android library (consumes the octet-sdk AAR). See
// :is-singapore for the shape rationale.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.octetproof.octetpolicy.us"
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
    implementation(project(":policy-core"))
    api("com.octetproof:sdk:1.1.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
