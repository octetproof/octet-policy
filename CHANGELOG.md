# Changelog

All notable changes to the OctetPolicy are recorded here. This is the
**package-level** changelog. Each policy under `policies/<name>/` has
its own `CHANGELOG.md` documenting changes to its data and behaviour —
the per-policy CHANGELOG is the audit artifact for sanctions / data
updates.

This project follows [Semantic Versioning](https://semver.org/).
`v1.0.0` will be the first stable release.

## [Unreleased]

### Changed

- Bumped the upstream `OctetSDK` pin to `1.1.0` on both platforms
  (`com.octetproof:sdk:1.1.0`, `octet-sdk-ios` exact `1.1.0`).
  Drop-in upgrade from `1.0.0` — the SDK 1.1.0 release is
  backwards-compatible (additive public-API changes, proof wire
  format unchanged). OctetPolicy's own public API and the policy
  data lists are unchanged.

## [1.0.0] — 2026-06-11

> **First stable release.** OctetPolicy's public API, policy data
> lists, and on-disk surface are committed to under semantic
> versioning from this release forward — backwards-compatible
> changes ship as 1.x.x.

### Changed

- Bumped the upstream `OctetSDK` pin to `1.0.0` on both platforms
  (`com.octetproof:sdk:1.0.0`, `octet-sdk-ios` exact `1.0.0`).
  Drop-in upgrade from `0.0.2-alpha` — license keys issued for
  `0.0.2-alpha` continue to verify against `1.0.0` (last
  wire-breaking license-key cutover was at SDK `0.0.2-alpha`).
- octetpolicy's own public API and the policy data lists are
  **unchanged** from `0.0.2-alpha`. Only the SDK pin moved.

### Fixed

- **OFAC loader JSON-strictness parity (M13).** The Kotlin
  `OfacList` loader now uses
  `Json { ignoreUnknownKeys = true }`, matching the Swift loader's
  default `JSONDecoder` behaviour and the existing `UsStateList`
  loader on both platforms. Without this, adding any audit-metadata
  field to `countries.json` would have crashed the Android build
  only — a hard parity break on the highest-stakes (sanctions)
  policy. Cross-platform unknown-key tests now fence both loaders
  on both platforms.

### Notes for consumers

- The OctetSDK's `Octet.start(config:, startPosition:)` and
  `loc.isWithin(region:, atTime:)` signatures are unchanged — no
  call-site edits required.
- SDK `1.0.0` carries forward the intervening alpha changes
  (transparent to OctetPolicy consumers): opt-in proof upload via
  `OctetConfig.proofUploadUrl` (default off), opt-in TLS
  public-key pinning, hardware-backed key storage with
  `DeviceKeySecurityLevel` reporting, fail-closed on-device proof
  verifier, magnetometer-based liveness signal, and a narrowed
  public-API surface scoped to the documented `OctetSDK` types.
  See the SDK CHANGELOGs for the full list.
- `0.0.2-alpha` is **deprecated**.

## [0.0.2-alpha] — 2026-06-04

### Changed

- Bumped the upstream `OctetSDK` pin to `0.0.2-alpha` on both
  platforms (`com.octetproof:sdk:0.0.2-alpha`,
  `octet-sdk-ios` exact `0.0.2-alpha`). OctetSDK 0.0.2-alpha is
  wire-breaking against v0-alpha license keys — **consumers
  upgrading must re-issue their license key** from
  [sdk.octetproof.com/signup](https://sdk.octetproof.com/signup).
- octetpolicy's own public API and the policy data lists are
  **unchanged** from `0.0.1-alpha`. Only the SDK pin moved.

### Notes for consumers

- The OctetSDK's `Octet.start(config:, startPosition:)` and
  `loc.isWithin(region:, atTime:)` signatures are unchanged — no
  call-site edits required.
- The license token format changed underneath (PASETO claim
  schema, device fingerprint, anti-rollback clock); none of that
  is visible at the call site. Paste the new token in and
  `Octet.start` handles it.
- `OctetConfig`'s `activationServerUrl` defaults to
  `https://api.octetproof.com` — override only if you run against
  a local backend.
- `0.0.1-alpha` is **deprecated**.

## [0.0.1-alpha] — 2026-05-29

First public release. Pre-stable — API, naming, and on-disk surface
may still change without notice across `0.0.x`.

### Added

- All four v1 predicates on both Swift and Kotlin:
  `isOfacComprehensive`, `isUS`, `isUSState`, `isSingapore`.
- Umbrella products on both platforms (`OctetPolicy` Swift module via
  `@_exported import`; `com.octetproof:octetpolicy` aggregator via
  Gradle `api(...)` deps) — consumers add one dependency, write one
  `import`.
- Real iOS and Android sample apps under `samples/`, installable on
  physical phones.
- CI workflows: PR tests on both platforms, tag-triggered releases.
- `mvn-repo` orphan branch initialized for Maven artifact distribution.
- iOS sample uses an xcconfig overlay so `DEVELOPMENT_TEAM` doesn't
  leak.
- OFAC `countries.json` and US `states.json` data lists reviewed and
  attested by `@btf8000` (2026-05-29).

### Distribution

- Swift: SwiftPM URL dep on this repo; the `OctetPolicy` umbrella
  product pulls in every predicate plus the re-exported octet-sdk
  API.
- Kotlin: Maven artifact `com.octetproof:octetpolicy` from the
  `mvn-repo` branch of this repo; transitively pulls the octet-sdk
  AAR from the public `octet-sdk-android` `mvn-repo` branch.
