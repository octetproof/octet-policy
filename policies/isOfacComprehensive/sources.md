# Sources — `isOfacComprehensive`

The predicate matches against the OFAC comprehensive embargo list.
"Comprehensive" means the sanctions apply to an entire country or
region — distinct from list-based sanctions like the SDN list,
which target named individuals or entities.

## Primary sources

| Source                        | URL                                                                                  |
|-------------------------------|--------------------------------------------------------------------------------------|
| OFAC programs index           | <https://ofac.treasury.gov/sanctions-programs-and-country-information>               |
| OFAC recent actions           | <https://ofac.treasury.gov/recent-actions>                                           |
| OFAC email alerts (subscribe) | <https://service.govdelivery.com/accounts/USTREAS/subscriber/new?topic_id=USTREAS_89> |
| Machine-readable lists        | <https://ofac.treasury.gov/sanctions-list-service>                                   |

The engineering on-call rotation is subscribed to the email alerts.
See the
[top-level README](../../README.md#review-sla--comprehensively-sanctioned-countries)
for the five-business-day SLA on responding to changes that affect
this list.

## Per-entry source URLs

Each entry in [`countries.json`](countries.json) carries its own
`source_url` linking to the OFAC page authoritative for that
sanctions program. **Adding or removing an entry requires citing the
primary-source URL in the [CHANGELOG](CHANGELOG.md) and in the
PR.** This is enforced by `CODEOWNERS` plus the pull-request
template.

## Why we do NOT consume the full SDN list

The Specially Designated Nationals (SDN) list is hundreds of
thousands of records meant for **KYC name screening** — checking
whether a specific named person or entity is on a sanctions list.
That is a different problem from "is this device located in a
sanctioned country/region", which is what this predicate answers.

If you need name screening, you need a KYC product. This is not
that — name screening is an explicit non-goal of this library.

## What about Russia, Syria, and the other Ukrainian oblasts?

- **Russia**: not comprehensively sanctioned. The U.S. has extensive
  sectoral and SDN-list sanctions targeting Russian entities, but
  the country as a whole does not have a comprehensive embargo. This
  predicate's contract is country-or-region-level comprehensive
  sanctions only.
- **Syria**: the regime is in flux. Recent developments are pending
  counsel review. Revisit before the next minor release.
- **Other Ukrainian oblasts (e.g. Zaporizhia, Kherson)**: not in the
  v1 list pending counsel review of the most recent OFAC actions.
  If OFAC's stance broadens further, a compliance issue should be
  opened and reviewed within the five-day SLA.

The history of any future addition or removal lives in
[`CHANGELOG.md`](CHANGELOG.md).
