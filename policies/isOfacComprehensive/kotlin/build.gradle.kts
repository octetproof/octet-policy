// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
//
// :is-ofac-comprehensive — Android library (consumes the octet-sdk
// AAR) that also ships countries.json as a java resource.
//
// countries.json is the single source of truth at
// policies/isOfacComprehensive/countries.json (shared with the Swift
// target). We copy *only* the JSON into a generated resources dir —
// see :is-us-state build.gradle.kts for why we don't point srcDir at
// the whole policy folder.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val generatedPolicyResources = layout.buildDirectory.dir("generated/policyResources")

val copyPolicyData = tasks.register<Copy>("copyPolicyData") {
    from(layout.projectDirectory.file("../countries.json"))
    into(generatedPolicyResources)
}

android {
    namespace = "com.octetproof.octetpolicy.ofac"
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
    sourceSets.getByName("main").resources.srcDir(generatedPolicyResources)
    publishing {
        singleVariant("release")
    }
}

tasks.named("preBuild").configure { dependsOn(copyPolicyData) }

dependencies {
    implementation(project(":policy-core"))
    api("com.octetproof:sdk:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
