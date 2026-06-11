// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.
//
// :is-us-state — Android library (consumes the octet-sdk AAR) that
// also ships states.json as a java resource.
//
// states.json is the single source of truth at
// policies/isUSState/states.json (shared with the Swift target). On
// the JVM we could just point `resources.srcDir("..")` at the folder,
// but that would bundle README.md / swift sources too, which collide
// across modules once the aggregator pulls several predicates onto one
// classpath. So we copy *only* the JSON into a generated resources dir.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val generatedPolicyResources = layout.buildDirectory.dir("generated/policyResources")

val copyPolicyData = tasks.register<Copy>("copyPolicyData") {
    from(layout.projectDirectory.file("../states.json"))
    into(generatedPolicyResources)
}

android {
    namespace = "com.octetproof.octetpolicy.usstate"
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

// preBuild anchors the whole graph, so the copy lands before any
// java-resource processing (main or unit-test).
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
