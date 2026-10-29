<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Concept, temporal and opt-in wire fixtures

`concepts.json` is hand-derived, not captured from a running .NET oracle. Its authority is [Cratis Fundamentals at f87af60265a2e59bd9b7435b0386ee06cb6d646a](https://github.com/Cratis/Fundamentals/tree/f87af60265a2e59bd9b7435b0386ee06cb6d646a), particularly `Source/DotNET/Fundamentals/Json/ConceptAsJsonConverter.cs` and `Source/DotNET/Fundamentals.Specs/Json/for_ConceptAsJsonConverter/`. Each row records its source, JSON bytes and read/both direction; `ConceptsModuleGoldenTest` exercises every row and rejects missing names.

The GUID and date specifications use runtime-generated values; these fixtures freeze representative values using the same formatting contracts. Numeric extrema reuse enum-backed specs as evidence for the same integer converter branches, not as a claim of enum parity. Float and DateTimeOffset have no dedicated upstream concept spec at this pin: rows are explicitly hand-derived from the converter's float/default branch and System.Text.Json formatting. DateTimeOffset writes seconds, trimmed fractions and `+00:00` for UTC; reads accept ISO `Z` and preserve the original offset without Jackson time-zone adjustment.

`temporal-collections.json` uses the same revision/source-root/direction envelope and is also hand-derived, not a live .NET oracle. Its temporal rows cover raw and concept LocalTime (whole, fractional, maximum, seconds/minutes reads), raw and concept LocalDate, and raw date-from-date-time/offset/UTC reads. Authority is `Json/TimeOnlyJsonConverter.cs:15-18`, `DateOnlyJsonConverter.cs:17-32` and `ConceptAsJsonConverter.cs:122-129`. The DateOnly-from-offset specification compares against `UtcDateTime`, but the converter uses `.DateTime` (the date as written). The golden value is chosen so both dates agree; the converter line is authoritative. A separate Kotlin test pins a cross-midnight offset with no zone shift. LocalDate concept reads remain strict dates.

The combined fixture's collection rows cover concept List (string, UUID, integer, empty, time), Array (date) and Set (integer). These derive from `EnumerableConceptAsJsonConverter.cs:51-59` and `EnumerableConceptAsJsonConverterFactory.cs:18-22`, using per-item scalar output. `TemporalModuleTest` consumes every row and rejects missing/duplicate names; `ConceptCollectionsTest` adds other scalar-family/null round trips. Null collections stay null and null elements are retained on the JVM, unlike C#'s read-time dropping and write-time omission. Non-array tokens fail on the JVM rather than becoming empty collections. See [combined fixture details](temporal-collections.md).

Raw temporal handlers are explicit-only in `TemporalModule`. LocalTime concepts use C# seven-digit output even without it; finer nanoseconds truncate rather than round. Reads are ISO-only, and `@JsonFormat` overrides on raw LocalDate/LocalTime are not honored. These bounds are deliberate; those fixtures do not establish culture-sensitive, enum/dictionary, TimeSpan or specialized-collection parity. Separate opt-in fixtures below cover the bounded enum, TimeSpan and concept-key contracts.

The `double_whole` row is read-only because .NET writes `42` and Jackson writes `42.0`. Kotlin tests separately pin Jackson's emitted whole-double form, plain-decimal notation, non-ASCII string emission and nanosecond timestamps. These are deliberate deviations, not oracle evidence; see [the parity ledger](../../../../../../Documentation/parity.md). Existing Kotlin and Java conformance tests preserve the default text-key contract, deliberately different from C# `ComplexKeyDictionaryJsonConverterFactory.cs:64-79,87-124` for GUID/string/date values. The opt-in dictionary fixture covers embedded JSON instead.

## Opt-in geospatial

`geospatial.json` is **hand-derived** from `Json/{Point,LineString,Polygon}JsonConverter.cs` at the pinned revision, not captured from a live .NET oracle, and uses the same revision/source-root/cases envelope. Rows with whole-number coordinates are `read` only because Jackson writes `1.0` where .NET writes `1`. `GeospatialModuleTest` consumes every row, rejects missing/duplicate names and pins the C# validation messages; `GeospatialModuleJavaConformanceTest` round-trips a record from Java. `GeospatialModule` is never ServiceLoader-discovered.

## Opt-in enums, TimeSpan and dictionaries

`enums.json`, `timespan.json` and `dictionaries.json` are also **hand-derived**, not captured from a live .NET oracle. Each uses the same revision/source-root/cases envelope and records direction and source lines. `EnumModuleTest`, `TimeSpanModuleTest` and `ConceptJsonKeysTest` consume every row, reject missing/duplicate names and include the source in dynamic test names. `WireContractsJavaConformanceTest` exercises the public modules and key-format constructor from Java. Existing concept/temporal fixtures and their tests are unchanged.

`enums.json` follows pinned `Json/EnumConverter.cs:22-53,66-69`, `EnumJson.cs:21-48,57-67`, `ConceptAsJsonConverter.cs:83-86,96-102,182-185` and `Fundamentals.Specs/Json/for_EnumConverter/`. The JVM Rank enum starts with an UNDEFINED placeholder so ONE/TWO/THREE ordinals match C# MyEnum's One = 1, Two = 2, Three = 3. The unknown-string specification generates a GUID at runtime; this fixture freezes `9b2f6c1e-0d6a-4c8e-9f3a-2b7d1e5c4a10`. Ordinals do not model arbitrary C# explicit values, non-int backing types or flags. Numeric-string undefined rejection, annotations, toString, exact/folded lookup priority and unchanged enum map keys are separate JVM tests, not claims of C# equivalence.

`timespan.json` uses an authority **outside** the Fundamentals pin: [dotnet/runtime v9.0.0 TimeSpanConverter.cs](https://github.com/dotnet/runtime/blob/v9.0.0/src/libraries/System.Text.Json/src/System/Text/Json/Serialization/Converters/Value/TimeSpanConverter.cs), lines 17-31 and 54-95 (called STJ in the rows). Fundamentals uses this built-in System.Text.Json converter; `Types/TypeConversion.cs:177-191` describes CLR conversion, not JSON formatting. TimeSpanModule writes canonical "c" values and keys, truncating toward zero to 100 ns. Canonical-shape reads plus ISO-8601 leniency, no short day/hour-minute forms, no zero-on-failure and no C# range enforcement are documented JVM bounds. Sub-tick truncation and ISO reads are pinned separately rather than attributed to a live oracle.

`dictionaries.json` follows `Json/ComplexKeyDictionaryJsonConverterFactory.cs:64-79,87-124,142-145`, its concept-string-key specification and the pinned scalar branches of ConceptAsJsonConverter. UUIDs and dates are frozen representative values. The mapper uses `ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)` and explicit EnumModule/TimeSpanModule where needed; the concept-values row retains default ConceptsModule. C# commonly spells inner quotes as `\u0022`, whereas Jackson writes `\"`: tests compare parsed trees, so decoded property names and values match without asserting byte-identical outer escaping. Embedded reads try a complete non-null JSON value first, then plain text. JSON-string-literal legacy keys are ambiguous and decode first; malformed/trailing/null JSON falls back to text. Arbitrary complex non-concept keys remain out of scope.

`derived-types.json` is **hand-derived from pinned source/specifications, not a live .NET oracle**.
Authority is `f87af60265a2e59bd9b7435b0386ee06cb6d646a`, rooted at
`Source/DotNET/Fundamentals`: `Serialization/DerivedTypeJsonConverter.cs:40-47,58-92`,
`DerivedTypes.cs:78-85`, `DerivedTypeAttribute.cs` and `DerivedTypeId.cs`, plus
`Fundamentals.Specs/Serialization/for_DerivedTypeJsonConverter/` and `for_DerivedTypes/`.
Every row lists its narrower source/specification path and line range; the JSON-null row
explicitly relies on STJ runtime null handling outside the pin. JVM models use camelCase
Jackson properties (including the read-only id-first row), not a global STJ naming-policy clone.
All 17 named rows execute exactly once as both/read/null/reject cases, with a coverage count and
name assertion. Tree equality pins the discriminator and nested list/array/map/recursive shape;
separate ordering assertions pin the id last. Java records/sealed families and JVM registration,
module ordering, generic binding, strict-property delegation, empty-registry byte identity and
ServiceLoader bounds are separate conformance tests, not additional .NET-oracle claims.
