// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

// MARK: - Umbrella re-exports
//
// `OctetPolicy` is the umbrella product. `@_exported import` of every
// other module lets consumers add this one product and write a single
// `import OctetPolicy` to reach every predicate plus the shared types.
//
// The SDK API (`Octet`, `OctetLoc`, `OctetRegion`, `OctetVerdict`) is
// re-exported too, so a single `import OctetPolicy` is enough to both
// start the SDK and call the predicates — mirroring the Kotlin
// aggregator's `api(sdk)`.

@_exported import OctetPolicyCore
@_exported import IsSingapore
@_exported import IsUS
@_exported import IsUSState
@_exported import IsOfacComprehensive
@_exported import OctetSDK
