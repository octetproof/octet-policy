// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Understone, Inc.

import Foundation

/// Structured reason on a `PolicyResult`. Mirrors the spirit of the
/// Octet SDK's `OctetVerdict.ReasonCode` so the audit chain from the
/// SDK's containment verdict → predicate result is preserved.
///
/// `.ok` paired with `match` is an answer. Any other reason paired
/// with `match == false` means **"we couldn't tell"** — distinct
/// from "the answer is no". Callers MUST inspect this field, not
/// just `match`, when making an enforcement decision.
public enum PolicyReasonCode: String, Codable, Equatable {
    /// The SDK answered determinately; `match` is authoritative.
    case ok                    = "OK"
    /// The SDK couldn't determine containment (no fix, cold sensors,
    /// geometry not yet released). `match == false`, but treat as
    /// "unknown", not "no".
    case verdictIndeterminate  = "VERDICT_INDETERMINATE"
    /// Caller passed invalid input (an empty list, an unknown state
    /// code). Predicates fail closed on input bugs.
    case inputInvalid          = "INPUT_INVALID"
}
