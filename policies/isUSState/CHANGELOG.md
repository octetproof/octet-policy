# Changelog — `isUSState`

All changes to this predicate's data file
([`states.json`](states.json)), behaviour, or scope are recorded
here. This is the per-policy audit log; the package-level changelog
lives at [`../../CHANGELOG.md`](../../CHANGELOG.md).

The codes in `states.json` come from the ISO 3166-2:US standard.
Changes would only happen if ISO 3166-2:US itself changes
(extremely rare — historic examples: the addition of `UM` for the
Minor Outlying Islands in 2010; not currently in our list — see
[`sources.md`](sources.md)).

## [0.0.2-alpha] — 2026-06-04

### Changed

- `policy_version` in [`states.json`](states.json) bumped from
  `0.0.1-alpha` to `0.0.2-alpha` to align with the package version.
  **No data changes** — same 56 entries (50 states + DC + 5
  territories) as `0.0.1-alpha`.

Reviewer: `@btf8000` — version-bump-only; the initial-list review
from `0.0.1-alpha` (completed 2026-05-29) carries through unchanged.

## [0.0.1-alpha] — 2026-05-29

### Added — initial list

Initial implementation. The list comprises:

- **50 states** — `AL`, `AK`, `AZ`, …, `WY`.
- **District of Columbia** — `DC` (treated as a distinct subdivision,
  not as a state).
- **Five unincorporated territories** as distinct entries:
  - `AS` — American Samoa
  - `GU` — Guam
  - `MP` — Northern Mariana Islands
  - `PR` — Puerto Rico
  - `VI` — U.S. Virgin Islands

Total: 56 entries.

**Not included:** `UM` (U.S. Minor Outlying Islands). This release
includes only the five major territories; revisit if a customer use
case requires it.

Reviewer: `@btf8000` — initial list review completed 2026-05-29.
All 56 entries (50 states + DC + 5 territories) attested against ISO
3166-2:US. `UM` (U.S. Minor Outlying Islands) deliberately excluded;
revisit if a customer use case requires it.
