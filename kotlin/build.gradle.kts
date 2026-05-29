// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

// Root project. Each subproject brings its own plugin block; this
// file declares plugin versions (apply false), shared config
// (group, version), and the maven-publish wiring.
//
// Predicate modules are Android libraries (they consume the octet-sdk
// AAR); policy-core stays a pure Kotlin/JVM module.
plugins {
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
}

allprojects {
    group = "com.octetproof"

    // Read the canonical version from the repo-root VERSION file,
    // not a duplicate inside kotlin/. Keeps Kotlin and Swift in
    // lockstep with one version string the CI version-sync lint enforces.
    val versionFile = rootProject.layout.projectDirectory
        .file("../VERSION").asFile
    version = versionFile.readText().trim()
}

// Apply maven-publish to every subproject. Each subproject's
// artifactId defaults to its Gradle name (e.g. :is-singapore →
// artifactId "is-singapore"). The local Maven layout writes to
// kotlin/build/mvn-repo/ via the LocalMvnRepo repository, which
// is what kotlin/scripts/verify-consumer-maven.sh consumes and
// what kotlin/scripts/publish-to-mvn-branch.sh (called by the CI
// release workflow on tag pushes) reads to push to the mvn-repo
// branch.
subprojects {
    apply(plugin = "maven-publish")

    // Shared pom metadata, applied to whichever publication a module
    // produces.
    val configurePom: MavenPublication.() -> Unit = {
        pom {
            name.set(this@subprojects.name)
            description.set("Part of octetpolicy — Apache 2.0 policy predicates for the Octet location SDK.")
            url.set("https://github.com/octetproof/octet-policy")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://github.com/octetproof/octet-policy")
            }
            organization {
                name.set("Understone, Inc.")
            }
        }
    }

    val configureRepo: PublishingExtension.() -> Unit = {
        repositories {
            maven {
                name = "LocalMvnRepo"
                url = uri(rootProject.layout.buildDirectory.dir("mvn-repo"))
            }
        }
    }

    // Android libraries (predicates + aggregator) publish the
    // `release` software component, which AGP only creates in its own
    // afterEvaluate — so resolve `from(...)` inside afterEvaluate to
    // dodge the ordering race. The component is enabled per module via
    // `android { publishing { singleVariant("release") } }`.
    plugins.withId("com.android.library") {
        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("maven") {
                    afterEvaluate { from(components["release"]) }
                    configurePom()
                }
            }
            configureRepo()
        }
    }

    // Pure-JVM module (policy-core) publishes the `java` component.
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("maven") {
                    afterEvaluate { from(components["java"]) }
                    configurePom()
                }
            }
            configureRepo()
        }
    }
}
