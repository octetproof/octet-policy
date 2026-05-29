# Sources — `isSingapore`

The predicate matches against ISO 3166-1 alpha-2 country code `SG`.

## Primary source

**ISO 3166-1** — the international standard for country codes,
maintained by the International Organization for Standardization.

- <https://www.iso.org/iso-3166-country-codes.html>

The two-letter code `SG` is allocated by ISO to Singapore. ISO 3166
is the authoritative international standard for country codes; we
do not mirror or paraphrase the standard, we just match against
the two-letter code as a string literal in the source.

## Why no data file

`isSingapore` does **not** ship a `countries.json`. The matching
rule is a single literal string `"SG"` and it lives in the source
code, not in a data file.

Predicates with stable single-country matches (`isUS`,
`isSingapore`) follow this pattern. Predicates with lists that
change over time (`isOfacComprehensive`) ship a data file so that
compliance updates are reviewable diffs against a structured
document.
