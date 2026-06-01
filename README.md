# Maven repository for the Octet Policy SDK

This branch is the **Maven repository** for the
[Octet Policy SDK](https://github.com/octetproof/octet-policy-sdk).
It is not human-readable source — it's a Maven layout consumed by
Gradle. The source lives on the `main` branch.

## Consume

Add the repo to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/octetproof/octet-policy-sdk/mvn-repo")
        }
    }
}
```

Then depend on the umbrella artifact in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.octetproof:octet-policy:0.0.1")
}
```

That single dependency transitively pulls in every v1 predicate
(`isSingapore`, `isUS`, `isUSState`, `isOfacComprehensive`) and the
shared types (`PolicyResult`, `Confidence`, `PolicyReasonCode`).

Power users who want a single predicate can depend on a granular
artifact instead:

- `com.octetproof:is-singapore:<version>`
- `com.octetproof:is-us:<version>`
- `com.octetproof:is-us-state:<version>`
- `com.octetproof:is-ofac-comprehensive:<version>`
- `com.octetproof:policy-core:<version>`            (shared types)
- `com.octetproof:octet-types-placeholder:<version>` (verdict types — deletable when upstream `octet-sdk` publishes)

## How this branch is updated

Artifacts are pushed here automatically by the
`.github/workflows/release-kotlin.yml` workflow on the `main` branch
whenever a `vX.Y.Z` tag is pushed. The workflow runs
`gradle publishAllPublicationsToLocalMvnRepoRepository`, copies the
generated `kotlin/build/mvn-repo/` layout into a clone of this
branch, commits, and pushes.

See the
[distribution guide](https://github.com/octetproof/octet-policy-sdk/blob/main/octet-policy-sdk-distribution-guide.md)
§4.3 on `main` for the design.

## Available versions

This branch will be empty until the first tagged release (P16+ in
the v1 phase plan). Once releases land, they'll appear under
`com/octetproof/<artifact-id>/<version>/`.

## License

Apache License, Version 2.0. See the `LICENSE` file on the `main`
branch.
