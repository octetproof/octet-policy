# OctetPolicy

Open-source policy predicates for the
[OctetSDK](https://sdk.octetproof.com) location proofs.

The OctetSDK ships a fact: a signed, attested claim about where a
device is. This library ships predicate functions that answer
*questions* about that fact — *"is the device in a country under
OFAC comprehensive embargo?"*, *"is the device in California?"*,
*"is the device in Singapore?"* — and returns structured results.
**Predicates answer questions; they do not enforce.** What you do
with the answer (block, warn, log, route, adapt UI) is up to your
code.

## Status

Pre-stable — currently `0.0.2-alpha`. API, naming, and on-disk
surface may still change across `0.0.x`. `v1.0.0` will be the first
stable release.

## License

[Apache License 2.0](LICENSE). See also [`NOTICE`](NOTICE).

This library is open source. The
[OctetSDK](https://sdk.octetproof.com) — which provides the
`OctetLoc` every predicate queries — is **commercial and requires a
license**. You can read, fork, and copy this policy library freely;
you cannot use it usefully at runtime without a valid OctetSDK
license. Sign up for a key (a free trial works for evaluation) at
[sdk.octetproof.com/signup](https://sdk.octetproof.com/signup). SDK
documentation lives at
[docs.octetproof.com](https://docs.octetproof.com).

## v1 predicates

All four are implemented on both Swift and Kotlin. Each is `async`
(`suspend` on Kotlin) and takes an `OctetLoc` from a started
OctetSDK.

| Predicate                      | Question                                                     |
|--------------------------------|--------------------------------------------------------------|
| `isOfacComprehensive(loc)`     | Is the device in a country under OFAC comprehensive embargo? |
| `isUS(loc)`                    | Is the device in the United States?                          |
| `isUSState(loc, states)`       | Is the device in one of these US states / territories?       |
| `isSingapore(loc)`             | Is the device in Singapore?                                  |

## Installation

### iOS (Swift Package Manager)

```swift
// Package.swift
dependencies: [
    .package(url: "https://github.com/octetproof/octet-policy", exact: "0.0.2-alpha"),
],
targets: [
    .target(
        name: "MyApp",
        dependencies: [
            // Single umbrella product — adds every predicate plus the
            // shared types via `@_exported import`. One `import OctetPolicy`
            // in your code brings in everything.
            .product(name: "OctetPolicy", package: "octet-policy"),
        ]
    ),
]
```

In Xcode (UI), the equivalent is: **File → Add Package Dependencies
→** paste `https://github.com/octetproof/octet-policy` → tick
`OctetPolicy` for your target. That's the only product you need.

Power users who want a single predicate without the rest can depend
on a granular product instead (`IsSingapore`, `IsUS`, `IsUSState`,
`IsOfacComprehensive`, `OctetPolicyCore`).

> Pre-stable: pin to `exact: "0.0.1-alpha"` while the API may still
> change across `0.0.x`. Once `v1.0.0` ships, switch to
> `from: "1.0.0"`.

### Android (Gradle)

In your project's root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // octetpolicy itself
        maven { url = uri("https://raw.githubusercontent.com/octetproof/octet-policy/mvn-repo") }
        // octetpolicy's transitive OctetSDK AAR dependency
        maven { url = uri("https://raw.githubusercontent.com/octetproof/octet-sdk-android/mvn-repo") }
    }
}
```

In your app `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.octetproof:octetpolicy:0.0.2-alpha")
}
```

`minSdk` must be 30 or newer (an OctetSDK requirement); `compileSdk`
34 or newer.

> Pre-stable: pin to the exact version `0.0.1-alpha` while the API
> may still change across `0.0.x`.

## Quick example

```swift
import OctetPolicy   // one import — predicates + the re-exported SDK API

// Start the OctetSDK once (needs your license key from
// sdk.octetproof.com/signup), then hand its `loc` to any predicate.
// The predicate runs the containment queries and returns a structured
// PolicyResult.
let sdk = try await Octet.start(
    config: OctetConfig(licenseKey: "octet_live_…"),
    startPosition: nil
)
let result = await isOfacComprehensive(sdk.loc)

if result.match {
    // Your code decides what to do — block, warn, log, route.
    // The predicate is intentionally silent on what you should do.
}
```

```kotlin
import com.octetproof.octetpolicy.isOfacComprehensive
import com.octetproof.sdk.api.Octet
import com.octetproof.sdk.api.OctetConfig

val sdk = Octet.start(context, OctetConfig(licenseKey = "octet_live_…"))
val result = isOfacComprehensive(sdk.loc)   // suspend

if (result.match) {
    // Your decision.
}
```

## Try the sample apps on your phone

Two ready-to-run sample apps live under [`samples/`](samples/), one
per platform. Both start the SDK, evaluate every v1 predicate against
your phone's **live location**, and render the results on-screen —
plus a map of where you are and a country picker. Open the project,
add your license key, plug in your phone, hit Run.

- **iOS** — [`samples/ios-sample/README.md`](samples/ios-sample/README.md)
- **Android** — [`samples/android-sample/README.md`](samples/android-sample/README.md)

Each sample is the simplest possible consumer: one umbrella
dependency, one `import OctetPolicy` (or
`import com.octetproof.octetpolicy.*`), one screen rendering results.

## Review SLA — comprehensively sanctioned countries

Changes to the OFAC comprehensive embargo list (currently: Cuba,
Iran, North Korea, and the Ukrainian regions of Crimea, Donetsk, and
Luhansk) are reviewed and either merged or formally rejected
**within five (5) business days** of being reported.

The engineering on-call rotation is subscribed to OFAC GovDelivery
email alerts (`USTREAS_89`). When an alert affects an embargoed
country, the on-call engineer opens a tracking issue within one (1)
business day; the assigned compliance reviewer responds within the
five-day window.

Other policy changes (US states, Singapore, etc.) have no SLA but
are still reviewed by a named `CODEOWNER` before merge.

## How to report a problem

Found a stale entry, a missing country, or a mismatched code? Open a
GitHub issue using the **Compliance Concern** template. Include:

1. The policy name (`isOfacComprehensive`, etc.).
2. The change you believe is needed.
3. A primary-source URL (e.g. an OFAC press release or
   recent-actions entry).
4. The date you would like the change effective.

The on-call engineer triages within one business day. For OFAC
comprehensive changes the five-day SLA above applies; for other
changes we'll commit to a date in the issue.

For non-compliance bugs (a predicate misbehaves, the SDK won't
compile, docs are wrong), use the standard **Bug Report** template.

## Writing your own predicates

Every predicate in this repository is a tutorial. We use only the
public OctetSDK API. We use long variable names. We comment on the
*why*, not the *what*. We do not abstract across predicates —
duplication is the point.

Pick a question your product asks and answer it with one function.
Some we've imagined but haven't shipped:

- `isAuthorizedDataCenter(loc, datacenterId)`
- `isInside3MileFulfillmentZone(loc, warehouseCoord)`
- `isCanadianProvince(loc, provinces)`
- `isInsideAny(loc, regions)` — composing across OctetSDK regions

Read [`policies/isUSState/`](policies/isUSState/) end-to-end — the
Swift implementation is the worked example. Copy the folder, rename,
edit. If something is awkward, that is feedback on the OctetSDK
public API — please open an issue.

**Goal: every engineer who reads the source comes away thinking of
three things they could build.**

## Repository layout

Each policy lives under `policies/<name>/` with its own README,
CHANGELOG, sources, data file, and per-language source. Swift
sources sit under `swift/`, Kotlin under `kotlin/`, sample apps
under `samples/`.

## Distribution

Releases are cut by tagging `vX.Y.Z`. Two GitHub Actions workflows
fire:

- **Release Swift** publishes a GitHub release that the SwiftPM URL
  resolves against.
- **Release Kotlin** publishes the Maven artifacts to the orphan
  [`mvn-repo` branch](../../tree/mvn-repo).

## Acknowledgements

Built on the [OctetSDK](https://sdk.octetproof.com). OFAC data
sourced from the U.S. Treasury Department's Office of Foreign Assets
Control public site; see each policy's `sources.md` for
primary-source URLs.
