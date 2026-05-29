# `isSingapore` — Singapore country-level predicate

`isSingapore(loc)` answers a single question:

> Is the device located in Singapore?

It returns a structured `PolicyResult`. It does **not** block, log,
warn, or otherwise act. What you do with the answer is up to your
code — predicates answer questions; they do not enforce.

## Match rule

The device is in Singapore iff the Octet SDK's containment oracle
answers *yes* for the Singapore country region —
`loc.isWithin(.country(isoCode: "SG"))`. The predicate asks the SDK
that one question and maps the verdict onto a `PolicyResult`. It never
sees raw coordinates; the SDK only ever returns yes / no / can't-tell.

## Result interpretation

| `match` | `reason`                | Meaning                                                                              |
|---------|-------------------------|--------------------------------------------------------------------------------------|
| `true`  | `.ok`                   | Device is in Singapore (`country == "SG"`).                                          |
| `false` | `.ok`                   | Device is determinately **outside** Singapore.                                       |
| `false` | `.verdictIndeterminate` | The SDK couldn't determine containment (no fix, sensors still warming up, …). Unknown — **not** "outside". |

**`match == false` with `.verdictIndeterminate` is not "device is not
in Singapore" — it is "we could not tell."**

## Primary source

ISO 3166-1 country codes. `SG` is allocated to Singapore. The full
source list lives in [`sources.md`](sources.md).

## Usage

The predicate is `async` (`suspend` on Kotlin): it queries the SDK,
which is a live location oracle. Get a `loc` from a started SDK, then
evaluate.

```swift
import OctetPolicy   // one import — predicates + the re-exported SDK API

let sdk = try await Octet.start(config: OctetConfig(licenseKey: "octet_live_…"))
let result = await isSingapore(sdk.loc)
if result.match {
    // Your decision: allowlist, route, log, etc.
}
```

```kotlin
import com.octetproof.octetpolicy.isSingapore
import com.octetproof.sdk.api.Octet
import com.octetproof.sdk.api.OctetConfig

val sdk = Octet.start(context, OctetConfig(licenseKey = "octet_live_…"))
val result = isSingapore(sdk.loc)   // suspend
```

## Reading the source

[`swift/IsSingapore.swift`](swift/IsSingapore.swift) is ~40 lines and
deliberately verbose: long variable names, comments on the *why*, no
abstractions across predicates. Read it end-to-end and you should be
able to write your own country-level predicate by copy-paste-adapting
it. The Kotlin port in [`kotlin/`](kotlin/) mirrors it.

## Review

This predicate's match rule is the ISO 3166-1 standard itself, so
substantive review is needed only on initial implementation and any
future expansion (e.g. if "Singapore" ever needed to include
specific territories or jurisdictions). See [`CHANGELOG.md`](CHANGELOG.md).
