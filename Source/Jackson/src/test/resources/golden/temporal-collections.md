<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Temporal and collection fixture provenance

Authority: Cratis/Fundamentals **f87af60265a2e59bd9b7435b0386ee06cb6d646a**. Paths in JSON rows are relative to `Source/DotNET/Fundamentals` (or the sibling `Fundamentals.Specs`). Rows are hand-derived from those branches/specifications, not live .NET output. Existing `concepts.json` is unchanged.

- DateOnly writes `yyyy-MM-dd`. Reads accept ISO dates, local date-times and offset date-times, taking the local date without shifting zones. .NET's broader culture-sensitive parsing is intentionally excluded.
- TimeOnly and TimeOnly concepts write `HH:mm:ss.fffffff`, including zero fractions. JVM nanoseconds beyond seven digits truncate, not round. ISO reads retain JVM nanoseconds; .NET cannot represent that extra precision.
- Enumerable concept output is an array of scalar values. `ConceptCollectionsTest` adds UUID, integer, decimal, string, LocalDate and LocalTime round trips plus empty/null lists.
- Deliberate null deviation: Jackson preserves null list elements as JSON null on read and write. C# enumerable reads drop null elements; its scalar writer emits no token for null elements. A null collection remains JSON null on the JVM.
- `TemporalModuleTest` covers invalid tokens/text, raw properties/lists/nulls, explicit/service-loaded concept registration, raw temporal opt-in, and text-only time concept keys. C# property-name serialization embeds JSON quotes for string-like keys; JVM keys deliberately do not.
- `TemporalModule` is explicit and not in ServiceLoader. `ConceptsModule` remains the only auto-discovered module; it includes LocalTime concept support without raw java.time overrides.

No new specialized collection converter is required: Jackson delegates each element to `ConceptsModule`. This is a bounded list contract, not enum, unsigned, Duration/TimeSpan, arbitrary enumerable/concept collection, or nested-concept parity.
