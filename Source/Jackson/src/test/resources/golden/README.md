<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Concept wire fixtures

`concepts.json` is hand-derived, not captured from a running .NET oracle. Its authority is [Cratis Fundamentals at f87af60265a2e59bd9b7435b0386ee06cb6d646a](https://github.com/Cratis/Fundamentals/tree/f87af60265a2e59bd9b7435b0386ee06cb6d646a), particularly `Source/DotNET/Fundamentals/Json/ConceptAsJsonConverter.cs` and `Source/DotNET/Fundamentals.Specs/Json/for_ConceptAsJsonConverter/`. Each row records its source, JSON bytes and read/both direction; `ConceptsModuleGoldenTest` exercises every row and rejects missing names.

The GUID and date specifications use runtime-generated values; these fixtures freeze representative values using the same formatting contracts. Numeric extrema reuse enum-backed specs as evidence for the same integer converter branches, not as a claim of enum parity. Float and DateTimeOffset have no dedicated upstream concept spec at this pin: rows are explicitly hand-derived from the converter's float/default branch and System.Text.Json formatting. DateTimeOffset writes seconds, trimmed fractions and `+00:00` for UTC; reads accept ISO `Z` and preserve the original offset without Jackson time-zone adjustment.

The `double_whole` row is read-only because .NET writes `42` and Jackson writes `42.0`. Kotlin tests separately pin Jackson's emitted whole-double form, plain-decimal notation, non-ASCII string emission and nanosecond timestamps. These are deliberate deviations, not oracle evidence; see [the parity ledger](../../../../../../Documentation/parity.md). Map-key fixtures live in Kotlin and Java conformance tests because those tests exercise the chosen text-key contract, which intentionally differs from C# `WriteAsPropertyName` for GUID/string/date values.
