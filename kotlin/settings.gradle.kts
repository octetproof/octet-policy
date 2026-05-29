// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

rootProject.name = "octetpolicy"

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
        // octet-sdk AAR (com.octetproof:sdk + com.octetproof:libpf) is
        // served from the orphan `mvn-repo` branch of octet-sdk-android.
        maven {
            url = uri("https://raw.githubusercontent.com/octetproof/octet-sdk-android/mvn-repo")
        }
    }
}

// Predicate modules are Android libraries (consume the octet-sdk AAR);
// policy-core is pure JVM; octetpolicy is the Android-library
// aggregator.
include(
    ":policy-core",
    ":is-singapore",
    ":is-us",
    ":is-ofac-comprehensive",
    ":is-us-state",
    ":octetpolicy",
)

project(":is-singapore").projectDir          = file("../policies/isSingapore/kotlin")
project(":is-us").projectDir                 = file("../policies/isUS/kotlin")
project(":is-ofac-comprehensive").projectDir = file("../policies/isOfacComprehensive/kotlin")
project(":is-us-state").projectDir           = file("../policies/isUSState/kotlin")
