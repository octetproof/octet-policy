# `isUS` — United States country-level predicate

`isUS(loc)` answers a single question:

> Is the device located in the United States?

It returns a structured `PolicyResult`. It does **not** block, log,
warn, or otherwise act — predicates answer questions; they do not
enforce.

## Match rule

The device is in the United States iff the Octet SDK answers *yes* for
the US country region — `loc.isWithin(.country(isoCode: "US"))`. This is
**the 50 states plus DC**.

### What is NOT included

ISO 3166-1 allocates separate codes to U.S. unincorporated territories,
and the SDK treats them as their own countries — so `country("US")`
returns *no* for a device in any of them. This predicate does **not**
match them:

| ISO 3166-1 | Territory                 |
|------------|---------------------------|
| `PR`       | Puerto Rico               |
| `GU`       | Guam                      |
| `VI`       | U.S. Virgin Islands       |
| `AS`       | American Samoa            |
| `MP`       | Northern Mariana Islands  |

If your use case is "US including territories", compose it yourself by
asking the SDK about the territory regions too:

```swift
var inUSOrTerritory = await isUS(loc).match
for code in ["PR", "GU", "VI", "AS", "MP"] where !inUSOrTerritory {
    if await loc.isWithin(region: .country(isoCode: code)).result == .yes {
        inUSOrTerritory = true
    }
}
```

The predicate stays narrow by design — different products treat the
territories differently (a sanctions regime might include them; a
delivery-zone product might exclude them), and we don't want to bake
one interpretation into the match rule.

## Result interpretation

| `match` | `reason`                | Meaning                                                          |
|---------|-------------------------|------------------------------------------------------------------|
| `true`  | `.ok`                   | Device is in the U.S. (50 states + DC).                          |
| `false` | `.ok`                   | Device is determinately **outside** the U.S.                     |
| `false` | `.verdictIndeterminate` | The SDK couldn't determine containment. Unknown — **not** "outside". |

`match == false` with `.verdictIndeterminate` is **not** "device is not
in the US" — it is "we could not tell."

## Primary source

ISO 3166-1, the international standard for country codes. See
[`sources.md`](sources.md).

## Usage

```swift
import OctetPolicy   // one import — predicates + the re-exported SDK API

let sdk = try await Octet.start(config: OctetConfig(licenseKey: "octet_live_…"))
let result = await isUS(sdk.loc)
if result.match {
    // Your decision.
}
```

```kotlin
import com.octetproof.octetpolicy.isUS
import com.octetproof.sdk.api.Octet
import com.octetproof.sdk.api.OctetConfig

val sdk = Octet.start(context, OctetConfig(licenseKey = "octet_live_…"))
val result = isUS(sdk.loc)   // suspend
```

## Reading the source

[`swift/IsUS.swift`](swift/IsUS.swift) mirrors
[`isSingapore`](../isSingapore/swift/IsSingapore.swift) almost exactly —
duplication is intentional ("resist DRY" — every predicate is a
complete worked example). Read both to understand how country-level
predicates work, and use either as the basis for your own. The Kotlin
port lives in [`kotlin/`](kotlin/).
