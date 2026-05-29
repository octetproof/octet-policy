// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // octetpolicy from its public mvn-repo branch. This is what
        // production consumers use.
        maven {
            url = uri("https://raw.githubusercontent.com/octetproof/octet-policy/mvn-repo")
        }
        // Local-publish fallback for maintainers hacking on an
        // unpublished version. To use, run from the repo root:
        //     cd kotlin && ./gradlew publishAllPublicationsToLocalMvnRepoRepository
        // and reference that version in app/build.gradle.kts.
        maven {
            url = uri("file://${rootDir.absolutePath}/../../kotlin/build/mvn-repo")
        }
        // The octetpolicy POM depends on the octet-sdk AAR
        // (com.octetproof:sdk + com.octetproof:libpf) — served from the
        // public `mvn-repo` branch of octetproof/octet-sdk-android.
        maven {
            url = uri("https://raw.githubusercontent.com/octetproof/octet-sdk-android/mvn-repo")
        }
    }
}

rootProject.name = "OctetPolicySample"
include(":app")
