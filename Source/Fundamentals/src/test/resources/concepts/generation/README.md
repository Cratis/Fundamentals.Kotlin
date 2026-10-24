<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Generation vector provenance

`generation-patterns.json` and `generation-checksums.json` are copied verbatim from
Cratis/Fundamentals at `f87af60265a2e59bd9b7435b0386ee06cb6d646a`,
`Source/DotNET/Fundamentals.Specs/Concepts/for_GenerateValue/Vectors/`.
That source copied them from Cratis/Screenplay at
`7b00cd9ffa72ff4f0eafaf56a0ef604b61da31bb`, `Source/Screenplay/Compiler/Conformance/`.

Tests consume every `support` (19), `invalid` (25), `membership` (10),
`checksums` (18) and `validation` (24) row. `assessments` (the binder's
101-year horizon) and `ranges` (Screenplay source-text parsing) are not runtime
operations and are deliberately not consumed, matching the C# runtime specs.
