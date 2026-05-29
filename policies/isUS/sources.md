# Sources — `isUS`

The predicate matches against ISO 3166-1 alpha-2 country code `US`.

## Primary source

**ISO 3166-1** — the international standard for country codes,
maintained by the International Organization for Standardization.

- <https://www.iso.org/iso-3166-country-codes.html>

The code `US` is allocated by ISO to the United States of America —
the 50 states plus the District of Columbia.

## Unincorporated territories — separate codes

ISO 3166-1 allocates distinct codes to U.S. unincorporated
territories. **These are not matched by `isUS`.**

| ISO 3166-1 | Territory                 |
|------------|---------------------------|
| `PR`       | Puerto Rico               |
| `GU`       | Guam                      |
| `VI`       | U.S. Virgin Islands       |
| `AS`       | American Samoa            |
| `MP`       | Northern Mariana Islands  |

If your use case needs "US plus territories", compose externally —
see [`README.md`](README.md).

## Why no data file

Same reasoning as `isSingapore`: a single literal country code lives
in the source code, not in a data file. Data files are reserved for
lists that change over time and benefit from reviewable diffs.
