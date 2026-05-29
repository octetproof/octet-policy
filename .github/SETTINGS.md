# Repository settings (maintainer checklist)

Settings that **cannot** be expressed as code in this repo but are
load-bearing for the v1 trust model. Configure these in the GitHub
repo's web UI under **Settings**. Re-audit at every maintainer
rotation.

## Branch protection — `main`

Available on the Teams plan (since 2026-05-29). Two equivalent
ways to set this up — pick one:

### Option A — Repository ruleset (recommended)

**Settings → Rules → Rulesets → New branch ruleset**, target
`main`:

- Enforcement status: **Active**
- **Bypass list**: add yourself (the active maintainer) so direct
  pushes to `main` are still possible. Anyone not on the list goes
  through a PR.
- ☑ **Require a pull request before merging**
- ☑ Required approvals: **1**
- ☑ **Require review from Code Owners** ← *load-bearing*; `CODEOWNERS`
  is advisory without it
- ☑ **Require status checks to pass**
  - ☑ Require branches to be up to date before merging
  - Required checks (from `.github/workflows/ci.yml`):
    - `SPDX header lint`
    - `Swift tests + consumer`
    - `Kotlin tests + consumer`

### Option B — Classic branch protection

**Settings → Branches → Branch protection rules → Add rule**, pattern
`main`:

- ☑ **Require a pull request before merging**
- ☑ Required approvals: **1**
- ☑ **Require review from Code Owners**
- ☑ **Require status checks to pass before merging**
  - ☑ Require branches to be up to date before merging
  - Required checks: `SPDX header lint`, `Swift tests + consumer`,
    `Kotlin tests + consumer`
- ☐ **Do not allow bypassing the above settings** ← leave UNCHECKED so
  repo admins can still push directly to `main` during active
  development. Tighten to ☑ later if you want the compliance gate to
  apply to *everyone* including admins.

### Bypass trade-off

While bypass is allowed, the Code Owners gate is only enforced for
non-admin contributors. That means the compliance gate on
`countries.json` runs on honor system for direct admin pushes. Once
external contributors start arriving, re-evaluate whether to remove
the bypass.

## Branch protection — `mvn-repo`

Configured 2026-05-29 as the **Protect mvn-repo** ruleset
(Settings → Rules → Rulesets) — target `mvn-repo` branch, **Active**,
no bypass list. Three rules:

- ☑ **Require linear history** — no merge commits.
- ☑ **Block force pushes** (`non_fast_forward`) — append-only history.
- ☑ **Restrict deletions** — the artifacts branch can't be deleted.

Routine commits to `mvn-repo` come from
`.github/workflows/release-kotlin.yml` via
`kotlin/scripts/publish-to-mvn-branch.sh`, which does a normal
fast-forward push — none of the rules above interfere. Yanking a
release is an exceptional manual operation that requires admin
override.

## CODEOWNERS — reviewers

[`CODEOWNERS`](../CODEOWNERS) at the repo root assigns review:

| Path                                                                     | Owner(s)                                          |
|--------------------------------------------------------------------------|---------------------------------------------------|
| `*` (default engineering review)                                         | `@octethacker`                                    |
| `countries.json` / `states.json` / `sources.md` / `isOfacComprehensive/` | `@btf8000` (primary), `@drew-octet` (backup)      |

The same compliance reviewers are recorded in the per-policy
`CHANGELOG.md` "Reviewer:" lines,
`policies/isOfacComprehensive/README.md` ("Reviewers" section), and the
`reviewed_by` field of each `countries.json` entry.

> Note: "Require review from Code Owners" only gates a PR if the owner
> isn't its author, so a meaningful compliance gate needs at least one
> reviewer who isn't the person opening the `countries.json` change.

## GitHub Actions secrets

`Settings → Secrets and variables → Actions`:

- _No custom secrets required._ The octet-sdk is now consumed from its
  public distribution channels — the iOS xcframework via the public
  SwiftPM URL (`github.com/octetproof/octet-sdk-ios`) and the Android
  AAR via the public `mvn-repo` branch of `octet-sdk-android`. The
  previously-needed `OCTET_SDK_PAT` can be deleted.

The release workflows use the default `GITHUB_TOKEN` for pushes to the
`mvn-repo` branch and for creating GitHub releases
(`permissions: contents: write` in the workflow files).

## Repository visibility

For a new public release, every item in the checklist below should be
☑ before the repo is made public:

- ☑ Reviewer assignments recorded in `CODEOWNERS`, `countries.json`,
  and per-policy `CHANGELOG.md` (currently: `@octethacker` default
  engineering; `@btf8000` primary + `@drew-octet` backup compliance).
- ☑ Compliance review of the data lists performed and attested in
  `countries.json` per-entry `reviewed_by` fields and in the
  per-policy CHANGELOGs.
- ☑ Copyright holder confirmed and the placeholder replaced across
  SPDX headers / NOTICE / POM.
- ☑ Branch protection settings above are all on for `main` and
  `mvn-repo`.
- ☑ CI workflows all green on the commit being released.
- ☑ At least one tagged release has run both release workflows
  end-to-end successfully.
- ☑ The `mvn-repo` branch has been populated by at least one release
  (otherwise consumer install instructions reference a branch with
  no artifacts).

## Future settings (post-v1)

Not blockers for v1.0.0:

- **GitHub Discussions** — for community Q&A. Turn on once the
  repo is public.
- **Dependabot** — now applicable: the repo depends on the real
  `octet-sdk` plus the usual transitive deps. Turn on once public.
- **Signed commits required on `main`** — overkill for v1; add
  later if signing key infrastructure is in place.
- **Project / Issue automation** — once compliance-concern issues
  start arriving in volume.
