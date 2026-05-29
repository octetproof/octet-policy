# Sources — `isUSState`

The predicate matches against ISO 3166-2:US subdivision codes.

## Primary source

**ISO 3166-2** — the international standard for subdivision codes,
maintained by the International Organization for Standardization.

- ISO 3166 main page: <https://www.iso.org/iso-3166-country-codes.html>
- ISO 3166-2 entry: <https://www.iso.org/standard/72483.html> (paid)
- Public reference (Wikipedia, not authoritative but useful for
  cross-referencing): <https://en.wikipedia.org/wiki/ISO_3166-2:US>

The codes in [`states.json`](states.json) are listed **without** the
`"US-"` prefix (e.g. `"CA"`, not `"US-CA"`) — that's the form callers
pass to `isUSState`. The predicate hands each code to
`OctetRegion.usState(code)`, which forms the full ISO 3166-2 region
(`US-CA`) the SDK evaluates.

## What this list contains

- **50 states** (AL → WY).
- **District of Columbia** (`DC`).
- **Five unincorporated territories**:
  - `AS` American Samoa
  - `GU` Guam
  - `MP` Northern Mariana Islands
  - `PR` Puerto Rico
  - `VI` U.S. Virgin Islands

Total: 56 entries.

## What this list does NOT contain

- `UM` — U.S. Minor Outlying Islands. ISO 3166-2:US includes this
  code, but it represents uninhabited or sparsely inhabited islands
  and is excluded from this v1 list. Revisit if a customer use case
  requires it.
- Indian reservations, U.S. military overseas bases, and similar
  sub-state entities — these are not ISO 3166-2 subdivisions and
  are out of scope for this predicate.

## Why no off-the-shelf data dependency

ISO 3166-2 is stable enough that an embedded JSON file beats any
runtime fetch from a third-party API. The 56 entries change on the
order of once a decade (the most recent addition was `UM` in 2010,
which we still don't include). Embedding the list keeps the
predicate deterministic, offline-capable, and audit-friendly — a
compliance officer pointing at the tagged release sees exactly the
codes the predicate matched against.
