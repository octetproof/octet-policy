# Changelog — `isOfacComprehensive`

All changes to this predicate's data file
([`countries.json`](countries.json)), behaviour, or scope are
recorded here. **This file is the per-policy audit log** —
compliance officers reading "what was on the list on date X" point
to this file plus the tagged release matching
`PolicyResult.policyVersion`.

Each entry must include:

- The date of the change (ISO 8601).
- The primary-source URL.
- The compliance reviewer's name.
- A semver bump in the top-level `VERSION` file and
  `PolicyPackage.version`.

## [0.0.2-alpha] — 2026-06-04

### Changed

- `policy_version` in [`countries.json`](countries.json) bumped from
  `0.0.1-alpha` to `0.0.2-alpha` to align with the package version.
  **No data changes** — same six entries (CU, IR, KP, UA-43, UA-14,
  UA-09) as `0.0.1-alpha`.

Reviewer: `@btf8000` — version-bump-only; the initial-list review
from `0.0.1-alpha` (completed 2026-05-29) carries through unchanged.

## [0.0.1-alpha] — 2026-05-29

### Added — initial list

Initial implementation. The v1 comprehensive embargo list:

| ISO 3166         | Name                      | Primary source                                                                                                       |
|------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------|
| `CU` (country)   | Cuba                      | <https://ofac.treasury.gov/sanctions-programs-and-country-information/cuba-sanctions>                                |
| `IR` (country)   | Iran                      | <https://ofac.treasury.gov/sanctions-programs-and-country-information/iran-sanctions>                                |
| `KP` (country)   | North Korea               | <https://ofac.treasury.gov/sanctions-programs-and-country-information/north-korea-sanctions>                         |
| `UA-43` (subdiv) | Crimea (Ukraine)          | <https://ofac.treasury.gov/sanctions-programs-and-country-information/ukraine-russia-related-sanctions>              |
| `UA-14` (subdiv) | Donetsk Oblast (Ukraine)  | <https://ofac.treasury.gov/sanctions-programs-and-country-information/ukraine-russia-related-sanctions>              |
| `UA-09` (subdiv) | Luhansk Oblast (Ukraine)  | <https://ofac.treasury.gov/sanctions-programs-and-country-information/ukraine-russia-related-sanctions>              |

**Explicitly NOT included:**

- **Syria** — the sanctions regime is in flux; pending counsel
  review. Revisit before the next minor release.
- **Russia** — not comprehensively sanctioned. Sectoral and SDN
  programs target named entities and are out of scope for this
  predicate (see [`sources.md`](sources.md)).

Reviewer: `@btf8000` — initial list review completed 2026-05-29.
All six entries (CU, IR, KP, UA-43, UA-14, UA-09) attested as
country/region-level OFAC comprehensive embargoes; Syria's exclusion
(in-flux regime, pending counsel) and Russia's exclusion (sectoral,
not comprehensive) confirmed. Backup reviewer: `@drew-octet`.
