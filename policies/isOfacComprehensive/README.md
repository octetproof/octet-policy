# `isOfacComprehensive` — OFAC comprehensive embargo predicate

`isOfacComprehensive(loc)` answers a single question:

> Is the device located in a country or region under OFAC
> **comprehensive** embargo?

It returns a structured `PolicyResult`. It does **not** block, log,
warn, or otherwise act. What you do with the answer is up to your
code — predicates answer questions; they do not enforce.

## What "comprehensive" means here

OFAC operates many sanctions programs. Most are *list-based* — they
restrict specific named individuals or entities (the SDN list). A
small number are *comprehensive*: they apply to an entire country or
region. This predicate covers only the comprehensive ones.

If you need SDN name screening, you need a KYC product. **This is
not that.** See [`sources.md`](sources.md).

## Current list — effective 2026-05-25, package version 1.0.0

| ISO 3166                | Name                              |
|-------------------------|-----------------------------------|
| `CU` (country)          | Cuba                              |
| `IR` (country)          | Iran                              |
| `KP` (country)          | North Korea                       |
| `UA` subdivision `43`   | Crimea (Ukraine)                  |
| `UA` subdivision `14`   | Donetsk Oblast (Ukraine)          |
| `UA` subdivision `09`   | Luhansk Oblast (Ukraine)          |

The exact, machine-readable list is in
[`countries.json`](countries.json). The audit trail of every change
is in [`CHANGELOG.md`](CHANGELOG.md).

### Not on this list

- **Syria** — the sanctions regime is in flux; pending counsel
  review. Re-evaluated before each minor release.
- **Russia** — **not** comprehensively sanctioned. Sectoral and
  SDN-list sanctions exist and are real, but they target named
  entities, not the country as a whole. Those programs belong in a
  KYC product, not this predicate.

## Match rule

The predicate walks the embargo list ([`countries.json`](countries.json))
and asks the SDK about each entry as a region:

- **country-level** entries (the entry's `subdivision` is `null`) →
  `loc.isWithin(.country(isoCode: entry.iso))`. Covers Cuba, Iran,
  North Korea.
- **subdivision-level** entries →
  `loc.isWithin(.subdivision(isoCode: "<iso>-<subdivision>"))`. Covers
  the three Ukrainian oblasts (e.g. `UA-43` for Crimea).

Aggregation is **fail-closed** — the right direction for sanctions:

1. Any region returns `yes` → `match = true`, with that entry's
   `country` and `subdivision` on the result. First match wins.
2. Else, if any region came back indeterminate → `.verdictIndeterminate`
   (unknown — never a false "not sanctioned").
3. Else (every region a determinate `no`) → clean negative (`.ok`).

### Fail-closed contract

If the SDK can't determine containment for a sanctioned region (no fix,
sensors still warming up, geometry not yet released, …) and nothing
else matched, the predicate returns `match=false,
reason=.verdictIndeterminate` — **not** a clean "device is not
sanctioned." A caller blocking on OFAC compliance MUST inspect
`result.reason` and treat `.verdictIndeterminate` as a blocking-worthy
"unknown", not a pass. The predicate stays enforcement-neutral — the
caller decides what to do — but the reason code carries the audit
information needed to fail closed.

## Result interpretation

| `match` | `reason`                | Meaning                                                                    |
|---------|-------------------------|----------------------------------------------------------------------------|
| `true`  | `.ok`                   | Device is in a sanctioned country/region.                                  |
| `false` | `.ok`                   | Device is determinately in **no** sanctioned region.                       |
| `false` | `.verdictIndeterminate` | The SDK couldn't determine containment for a sanctioned region (and none matched). **Unknown — fail closed.** |

**`match == false` with `.verdictIndeterminate` is "we could not tell" —
never "device is not in a sanctioned region."**

## Primary source

- OFAC Sanctions Programs and Country Information:
  <https://ofac.treasury.gov/sanctions-programs-and-country-information>
- OFAC Recent Actions:
  <https://ofac.treasury.gov/recent-actions>
- OFAC email alerts (engineering on-call subscribes):
  <https://service.govdelivery.com/accounts/USTREAS/subscriber/new?topic_id=USTREAS_89>

Per-entry source URLs are recorded in
[`countries.json`](countries.json) and listed in
[`sources.md`](sources.md).

## Review SLA

Changes to this list are reviewed within **five (5) business days**.
See the
[top-level README](../../README.md#review-sla--comprehensively-sanctioned-countries)
for the full SLA language.

## Reviewers

- `@btf8000` — primary
- `@drew-octet` — backup

Both are listed in [`CODEOWNERS`](../../CODEOWNERS). A pull request
changing [`countries.json`](countries.json) cannot merge without review
from at least one of them — assuming branch protection is properly
configured on `main` (see the top-level README's "For maintainers"
section). Initial v1 list review completed 2026-05-29 — see
[`CHANGELOG.md`](CHANGELOG.md).

## How to report a problem

See the
[top-level README](../../README.md#how-to-report-a-problem). Use the
*Compliance Concern* issue template and include a primary-source URL.

## Usage

```swift
import OctetPolicy   // one import — predicates + the re-exported SDK API

let sdk = try await Octet.start(config: OctetConfig(licenseKey: "octet_live_…"))
let result = await isOfacComprehensive(sdk.loc)

if result.match {
    // Device is in a sanctioned region.
    // Your code decides: block, log, warn, route.
}

if !result.match && result.reason != .ok {
    // .verdictIndeterminate: we couldn't tell which/whether.
    // Fail closed if your use case demands it.
}
```

```kotlin
import com.octetproof.octetpolicy.isOfacComprehensive
import com.octetproof.sdk.api.Octet
import com.octetproof.sdk.api.OctetConfig

val sdk = Octet.start(context, OctetConfig(licenseKey = "octet_live_…"))
val result = isOfacComprehensive(sdk.loc)   // suspend
```

## What `policyVersion` means

`result.policyVersion` is the semantic version of *this package* at
the time you compiled your app. If you ship one version and a new
country is added to the list in a later version, **your app keeps
matching against the version you compiled against until you upgrade
the dependency**. This is by design — your match behaviour stays
deterministic until you choose to change it.

The [CHANGELOG](CHANGELOG.md) and tagged releases are the audit
artifact: a compliance officer asking "which list did we evaluate on
date X?" should be able to read the `policyVersion` your app
reported, pull that tagged release, and read the exact list.

## Reading the source

[`swift/IsOfacComprehensive.swift`](swift/IsOfacComprehensive.swift)
and [`swift/OfacList.swift`](swift/OfacList.swift) together are the
predicate body and the JSON loader. Both are tutorial-style: long
variable names, comments on the *why*, no clever abstractions. The
Kotlin port lives in [`kotlin/`](kotlin/).

If you want to write your own list-based predicate (e.g. an EU
sanctions equivalent, or your own internal allowlist), copy this
folder and edit. The pattern translates directly.
