# Changelog

All notable changes to the OctetPolicy are recorded here. This is the
**package-level** changelog. Each policy under `policies/<name>/` has
its own `CHANGELOG.md` documenting changes to its data and behaviour —
the per-policy CHANGELOG is the audit artifact for sanctions / data
updates.

This project follows [Semantic Versioning](https://semver.org/).
`v1.0.0` will be the first stable release.

## [Unreleased]

- Nothing yet.

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
