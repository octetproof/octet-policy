---
name: Feature Request
about: Suggest a new predicate or an enhancement to the SDK
title: '[Feature] '
labels: ['enhancement', 'needs-triage']
assignees: ''
---

## The question your predicate would answer

Predicates answer questions. What question would this new predicate
answer? (For example: *"is the device in a Canadian province?"*, *"is
the device inside our authorised data centre fleet?"*)

## Why does this matter?

What product decision does answering this question unblock?

## Proposed signature

If you've thought about the API surface, drop a draft signature:

```swift
func isCanadianProvince(_ loc: OctetLoc, _ provinces: [String]) async -> PolicyResult
```

## Is this generally useful?

The policy SDK ships predicates we believe will be **broadly useful
across many customers**. A predicate that's specific to one customer's
data centres is great — but it belongs in your own codebase, not this
repository. (See the README's *Writing your own predicates* section.)

If you think this predicate is broadly useful, explain why.

## Additional context

Anything else.
