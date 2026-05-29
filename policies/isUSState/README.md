# `isUSState` — US subdivision-level predicate

`isUSState(loc, states)` answers a single question:

> Is the device in one of these US states or territories?

It returns a structured `PolicyResult`. It does **not** block, log,
warn, or otherwise act — predicates answer questions; they do not
enforce.

This is the **worked example**. If you want to understand the
pattern for writing your own predicates, read this folder
end-to-end: the source, the tests, the data file, and these docs.

## Two overloads — single string and list

```swift
isUSState(loc, "CA")               // sugar; delegates to the list form
isUSState(loc, ["NY", "CA", "TX"]) // the list form is the load-bearing one
```

Both take ISO 3166-2:US subdivision codes **without** the `"US-"`
prefix:

- 50 states: `AL`, `AK`, …, `WY`
- District of Columbia: `DC`
- Territories: `PR` (Puerto Rico), `GU` (Guam), `VI` (U.S. Virgin
  Islands), `AS` (American Samoa), `MP` (Northern Mariana Islands)

The complete machine-readable list lives in
[`states.json`](states.json). The predicate validates caller input
against that file — passing an unknown code returns `.inputInvalid`.

## Match rule

The predicate queries the SDK once per requested state —
`loc.isWithin(.usState(code))` — and aggregates, fail-closed:

1. The caller's `states` list is non-empty (else `.inputInvalid`).
2. Every code is a known ISO 3166-2:US subdivision (else
   `.inputInvalid`). Both checks run **before any SDK call**.
3. For each requested state, ask `loc.isWithin(.usState(code))`. The
   **first `yes` wins** → `match = true`, `country = "US"`,
   `state = <that code>`.
4. If nothing matched but any query came back indeterminate →
   `.verdictIndeterminate` (unknown — fail closed, never a false "no").
5. If every query was a determinate `no` → clean negative (`.ok`).

When `match` is `true`, `result.state` is the matched code (always one
of the codes the caller passed).

## Why state detection is a separate predicate

Country-level positioning (MCC / network-based in the SDK) has a very
different confidence profile from subdivision-level positioning (which
needs GNSS-class accuracy). Keeping them as distinct predicates means a
country-only caller reaches for [`isUS`](../isUS/README.md) and a
subdivision caller reaches for this one — neither silently conflates the
two resolutions, which is exactly the audit-trail clarity the structured
result is designed to preserve.

## Result interpretation

| `match` | `reason`                | Meaning                                                                          |
|---------|-------------------------|----------------------------------------------------------------------------------|
| `true`  | `.ok`                   | Device is in one of the caller's listed states.                                  |
| `false` | `.ok`                   | Device is determinately in **none** of the listed states.                        |
| `false` | `.inputInvalid`         | Caller passed an empty list or an unknown subdivision code. **Programmer bug.**  |
| `false` | `.verdictIndeterminate` | At least one query was indeterminate and none matched. Unknown.                  |

**`match == false` with `.inputInvalid` or `.verdictIndeterminate` is
"you called the predicate wrong" or "we could not tell" — never "device
is not in your list."**

## Fail-closed input validation

Invalid input is reported as `.inputInvalid` **before** any SDK call.
The reasoning: an empty `states` list or an unknown code is a programmer
bug, and surfacing it ahead of runtime failure modes is more useful for
debugging. The predicate does the same with mixed valid/invalid input —
a single bad code poisons the whole call. We don't silently ignore the
bad entry; we fail the whole call and let the caller fix the input.

## Primary source

ISO 3166-2:US — the international standard for U.S. subdivisions
(50 states, DC, and 5 outlying territories). See
[`sources.md`](sources.md).

## Usage

```swift
import OctetPolicy   // one import — predicates + the re-exported SDK API

let sdk = try await Octet.start(config: OctetConfig(licenseKey: "octet_live_…"))
let result = await isUSState(sdk.loc, ["CA", "NY", "WA"])
if result.match {
    // Device is in CA, NY, or WA. Your decision: allowlist, route, etc.
}
if !result.match && result.reason != .ok {
    // We couldn't tell, or your input was bad. Inspect result.reason.
}
```

```kotlin
import com.octetproof.octetpolicy.isUSState
import com.octetproof.sdk.api.Octet
import com.octetproof.sdk.api.OctetConfig

val sdk = Octet.start(context, OctetConfig(licenseKey = "octet_live_…"))
val result = isUSState(sdk.loc, listOf("CA", "NY", "WA"))   // suspend
```

## Reading the source

| File                                                | Purpose                                                  |
|-----------------------------------------------------|----------------------------------------------------------|
| [`swift/IsUSState.swift`](swift/IsUSState.swift)    | The predicate — tutorial-style, `async`, region-by-region.|
| [`swift/UsStateList.swift`](swift/UsStateList.swift)| The `states.json` loader.                                |
| [`swift/Tests/`](swift/Tests/)                      | The test matrix doubles as documentation.                |
| [`kotlin/`](kotlin/)                                | The Kotlin port (Android library), mirroring the Swift.  |
| [`states.json`](states.json)                        | The 56 subdivision codes.                                |

## Make your own subdivision predicate

If you want, say, `isCanadianProvince(loc, provinces)`:

1. Copy this entire folder to `policies/isCanadianProvince/`.
2. Rename `states.json` → `provinces.json` and replace its contents
   with Canada's ISO 3166-2 codes (`AB`, `BC`, `MB`, `NB`, …).
3. Swap the region you query: instead of `.usState(code)` (which builds
   `US-<code>`), use `.subdivision(isoCode: "CA-\(code)")`.
4. Add a target + test target to `Package.swift` (and a Gradle module).
5. Update the tests to match.

The pattern translates directly. Duplication across predicates is
intentional — every file is a complete worked example. "Resist DRY"
is a deliberate choice here, not an oversight.
