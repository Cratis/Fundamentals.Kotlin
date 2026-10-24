<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Fundamentals.Kotlin — project context

A Kotlin and Java JVM library, not a Spring application or an event-sourced application. Read this file and [the parity ledger](../../../Documentation/parity.md) before changing behavior. These project-owned instructions override conflicting shared Cratis guidance.

## Authority and scope

- C# authority: `Cratis/Fundamentals` at `f87af60265a2e59bd9b7435b0386ee06cb6d646a`, primarily `Source/DotNET/Fundamentals`. Ledger entries must name exact paths, implemented contracts, tests and deliberate deviations. Do not infer full parity from successful compilation or a round trip.
- Kotlin seed: `Cratis/Arc.Kotlin` at `8a4a9be13b4e4a9bb5c20d238726c641fb4412a2`. Casing fixture seed: `Cratis/Fundamentals.Go` at `c04690df426c5383b62dbf6c71bd799530d5bf0a`.
- Two artifacts: `io.cratis:fundamentals` (core) and `io.cratis:fundamentals-jackson` (optional Jackson 3). Packages begin `io.cratis.fundamentals`. No hard Jackson or Spring dependency in core.
- Arc.Kotlin's current concept/JSON ownership remains unchanged. Do not add re-exports, migration shims or an Arc dependency as an incidental improvement. A later extraction owns that migration.
- v1 covers ConceptAs, CorrelationId, exact name-casing rules and a bounded scalar-concept Jackson module. No generated values, general factories/maps, derived-type discovery, correlation ambient storage or broader library parity. Keep missing behavior separate from deliberate differences; every Not implemented ledger row links to a real follow-up issue.

## Workspace and toolchain

`Source/Fundamentals` and `Source/Jackson` are separate Gradle modules. Production code is under `src/main/kotlin`, tests under `src/test/kotlin` and `src/test/java`, golden fixtures under `src/test/resources`. One public top-level type per production file; private/internal support stays narrow. Keep copyright and MIT notices.

Use JDK 17, JVM target/release 17, Kotlin 2.4.20 and Gradle wrapper 8.14.4. Kotlin uses explicit API mode and JVM default ENABLE. Kotlin and Java warnings are errors. Jackson is 3.2.2 (`tools.jackson.*`), aligned with Spring Boot 4.1; do not import Jackson 2 databind/core. Jackson annotations retain their upstream `com.fasterxml.jackson.annotation` package. The core's only implementation dependency is Kotlin stdlib; Jackson in core tests does not make it a runtime dependency.

## Public APIs and wire behavior

`ConceptAs<T>` has one required abstract method, `value()`, plus a synthetic Kotlin extension property. Java records can implement it without getters or annotations; primitive record components must be boxed (`Integer`, not `int`). Implementations own equality, hashing, comparison, display, validation and null rejection. Static helpers resolve generic interfaces/base classes; do not silently convert an unresolved type to Object.

`CorrelationId` is a final immutable UUID-backed class, not an inline/value class. Preserve `NOT_SET`, static `create()`, value equality/hash and lowercase UUID text. Do not add ThreadLocal storage, parsing or propagation APIs in this slice.

String functions live in `StringExtensions.kt`, with Java facade `StringExtensions`. Preserve the two-leading-uppercase acronym guard, full camel loop and UTF-16 behavior; do not replace the loop with a first-character shortcut. Kotlin String receivers are non-null; this deliberately differs from C# null handling. Invariant U+0130/U+0131 exceptions are pinned. Do not claim universal Unicode table equivalence.

Jackson construction order is record canonical constructor, public static `of(value)`, public single-argument constructor. Match primitive and boxed parameters and cache reflection per class. Preserve null as null. Explicitly handle the supported UUID/string/numeric/boolean/date/offset scalar set; parse UUID, LocalDate and OffsetDateTime directly from text. Offset reads must preserve offsets without relying on Jackson's default time-zone adjustment. Offset output always includes seconds and numeric `+00:00`, trims fractions and preserves JVM nanoseconds.

Map keys are underlying text, not embedded JSON strings. BigDecimal is plain notation. Whole floats/exponents, Unicode escaping, signed bytes, numeric ranges and timestamp precision differ from .NET; keep direction-specific fixtures and the ledger honest rather than normalizing unrelated Jackson behavior globally. Fallback delegation does not create a parity guarantee.

## Testing and API compatibility

Use JUnit Jupiter (`org.junit.jupiter.*`, currently 6.1.3); use parameterized tests, descriptive Kotlin backtick names, ordinary Java tests, explicit assertions and small private fixture types. No MockK dependency is needed. Do not import .NET testing conventions: no `given` inheritance trees, `Establish` / `Because` / `It` fields, per-assertion files, C# spec-folder layouts or assertion-framework substitutions.

Write tests first. Every public API change needs a Java consumer test under `src/test/java`, not just Kotlin tests. Exercise records, statics, generic contracts and Jackson construction from plain Java. Golden tests consume every fixture row and name source pins/direction; self round trips alone are not .NET wire evidence. Keep the casing UTF-16 cases and the Jackson ServiceLoader registration test.

Required checks:

```sh
./gradlew apiDump                 # only for an intentional API change; review generated diffs
./gradlew clean build apiCheck    # full JVM build and non-integration tests, warnings as errors
actionlint -color                 # after workflow changes
```

Do not hand-edit `.api` files or hide API changes by regenerating dumps after a failed check. Run `apiCheck` against the reviewed committed baseline. Follow the invoking parent's exact queue commands/policy; AI build/test commands go through `~/.pi/agent/bin/pi-phase run`, never bypass shared admission or elevate priority without authorization. Report exact commands and results. Never call a partial check a full gate. Repository work is not PR-ready without its required gates.

## Git, release and work records

Use small imperative commits. No push, merge, GitHub mutation or release without explicit user authorization. Never rewrite published history. Real merge commits are the default merge policy. Every PR requires exactly one of `major`, `minor`, `patch`, `no-release`.

Workflow actions are SHA-pinned; jobs need bounded timeouts, least privilege and cancellation-safe concurrency. Build workflow runs the complete build. Publication verifies first, then publishes both artifacts in one release-action job behind `maven-central`; verification must not cancel publication. Do not add unrelated scheduled jobs or broaden secrets exposure. Owner-managed secrets stay in environment variables, never source, labels, logs or command lines. Use `actionlint` for workflow changes.

Scratch plans, logs and handoffs belong only in ignored `.ai-work/`; never commit them, relax ignore rules or force-add them. Preserve unrelated work. Do not fix nearby problems opportunistically. If source evidence conflicts with a binding decision or the scope widens, stop and ask.
