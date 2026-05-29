# Changelog — `isUS`

All changes to this predicate's behaviour, scope, or data are
recorded here. This is the per-policy audit log. Package-level
changes (build, CI, etc.) live in the
[top-level CHANGELOG](../../CHANGELOG.md).

## [0.0.1-alpha] — 2026-05-29

### Added

- Initial implementation. Matches ISO 3166-1 alpha-2 `US`
  (50 states + DC).
- Explicitly does NOT match unincorporated territories: `PR` (Puerto
  Rico), `GU` (Guam), `VI` (U.S. Virgin Islands), `AS` (American
  Samoa), `MP` (Northern Mariana Islands). They have their own ISO
  codes; composition is the caller's responsibility.

Reviewer: `@btf8000` — initial review completed 2026-05-29; ISO
3166-1 alpha-2 `US` and the territory-exclusion semantics attested.
