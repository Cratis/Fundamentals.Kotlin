<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Cratis Fundamentals for Kotlin and Java

Shared domain primitives for JVM applications: typed concepts, correlation identifiers, .NET-compatible name casing and an optional Jackson 3 concept module. This is a small v1 surface, **not full .NET Fundamentals parity**. See the [parity ledger](Documentation/parity.md) for exact source pins, evidence, differences and follow-up work.

## Requirements and artifacts

- JDK **17** or later; build with JDK 17.
- Kotlin **2.4.20** for Kotlin consumers. Java consumers need no Kotlin source or compiler.
- Jackson **3.2.2** (`tools.jackson.*`), aligned with Spring Boot 4.1, for the optional Jackson artifact. No Spring dependency is required.
- Gradle wrapper **8.14.4** for repository builds; Maven Central for consumption.

| Artifact | Purpose |
| --- | --- |
| `io.cratis:fundamentals` | `ConceptAs`, `CorrelationId`, string casing; no Jackson runtime dependency |
| `io.cratis:fundamentals-jackson` | Jackson 3 `ConceptsModule`; includes core and Jackson databind transitively |

Choose a published release version and replace `<released-version>` below. No release is implied by this source checkout.

```kotlin
repositories { mavenCentral() }

val fundamentalsVersion = "<released-version>"
dependencies {
    implementation("io.cratis:fundamentals:$fundamentalsVersion")
    // Optional; also brings in fundamentals:
    implementation("io.cratis:fundamentals-jackson:$fundamentalsVersion")
}
```

## Kotlin

```kotlin
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.concepts.value
import io.cratis.fundamentals.execution.CorrelationId
import io.cratis.fundamentals.jackson.ConceptsModule
import io.cratis.fundamentals.strings.toCamelCase
import java.util.UUID
import tools.jackson.databind.json.JsonMapper

data class OrderId(private val id: UUID) : ConceptAs<UUID> {
    override fun value(): UUID = id
}

fun main() {
    val id = OrderId(UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff"))
    println(id.value) // Kotlin extension view of value()
    println(ConceptAs.valueTypeOf(OrderId::class.java)) // UUID
    println(CorrelationId.create()) // random UUID v4; NOT_SET is the all-zero UUID
    println("URLValue".toCamelCase()) // URLValue (acronym guard)

    val mapper = JsonMapper.builder().addModule(ConceptsModule()).build()
    val json = mapper.writeValueAsString(id) // a quoted UUID, not { "value": ... }
    check(mapper.readValue(json, OrderId::class.java) == id)
}
```

## Java

```java
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.fundamentals.execution.CorrelationId;
import io.cratis.fundamentals.jackson.ConceptsModule;
import io.cratis.fundamentals.strings.StringExtensions;
import java.util.Objects;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

public final class Example {
    record OrderId(UUID value) implements ConceptAs<UUID> {
        OrderId { Objects.requireNonNull(value); }
    }
    record Quantity(Integer value) implements ConceptAs<Integer> {}

    public static void main(String[] args) {
        var mapper = JsonMapper.builder().addModule(new ConceptsModule()).build();
        var id = new OrderId(UUID.randomUUID());
        var json = mapper.writeValueAsString(id);
        var restored = mapper.readValue(json, OrderId.class);
        System.out.println(restored.value());
        System.out.println(ConceptAs.isConcept(OrderId.class));
        System.out.println(CorrelationId.NOT_SET);
        System.out.println(StringExtensions.toPascalCase("order")); // Order
    }
}
```

A Java record's component must be boxed for generic primitive values: `Integer value`, not `int value`. There is only one required interface method, `value()`. Equality, hashing, comparison, validation, null rejection and display behavior belong to the implementation. Kotlin data classes and Java records supply value equality naturally.

## Jackson registration and construction

Use explicit `addModule(new ConceptsModule())` in Java or `addModule(ConceptsModule())` in Kotlin. Alternatively:

```java
var mapper = JsonMapper.builder().findAndAddModules().build();
```

The artifact registers `tools.jackson.databind.JacksonModule` through ServiceLoader. No annotations, Spring configuration or Kotlin Jackson module are needed for concept construction. The module chooses, in order:

1. A one-component Java record's canonical constructor (including package-private records on the classpath).
2. A public static `of(value)` returning the concept's exact class.
3. A public single-argument constructor with a compatible primitive or boxed parameter.

A private constructor plus a public static `of` works. Missing construction shapes fail with a Jackson databind error naming the type and the accepted shapes. Named Java modules must permit reflective access to the relevant package.

Supported underlying scalars: UUID, String, Int/Integer, Long, Short, Byte, Double, Float, BigDecimal, Boolean, LocalDate and OffsetDateTime. JSON null stays null; lists of concepts contain scalar values. UUID text is canonical lowercase. OffsetDateTime writes explicit seconds, a trimmed optional fraction and numeric offsets (including `+00:00`); reads preserve the original offset and accept `Z`.

Concept map keys use the underlying **text** value, not JSON text inside the property name. This deliberately differs from C# for string-like keys. Signed JVM bytes, whole-float/exponent spelling, Unicode escaping, arbitrary-precision decimal range and nanosecond timestamps also differ from .NET. See [wire differences](Documentation/parity.md#wire-and-language-differences) before sharing payloads across runtimes. Delegation for unsupported types does not imply parity.

## Build and test

```sh
./gradlew clean build apiCheck
```

The build compiles Kotlin and Java for JVM 17 with warnings as errors and runs JUnit tests. Public API dumps live in each module's `api/` directory and are checked by Kotlin binary compatibility validator. For an intentional public API change, regenerate with `./gradlew apiDump`, review the diff, then rerun `clean build apiCheck`; never hand-edit the dumps. Run `actionlint -color` after workflow changes. Local AI execution must also follow the repository's [project context](.cratis/ai/rules/project.md) and any parent build-queue policy.

## Releases

Every PR needs exactly one release-intent label: `major`, `minor`, `patch`, or `no-release`. Pushes to `main` touching source/build inputs trigger verification, then `cratis/release-action` and signed publication of **both** artifacts to Maven Central. Documentation-only changes need no artifact release; use `no-release`. Manual dispatch remains available. Publication uses the `maven-central` environment and repository-owner-managed signing/Central credentials; never place credentials in source or command lines. Publication and GitHub release creation stay in one job so failed uploads do not announce a successful release.

## Scope

Generated values, generation patterns/checksums, general ConceptMap/ConceptFactory APIs, temporal/collection converter parity, derived types, ambient correlation access and the broader Fundamentals library are not implemented. Their follow-up issues are linked in the [ledger](Documentation/parity.md#surfaces).

Arc.Kotlin still owns its existing `io.cratis.arc.concepts` and JSON code and has no dependency on these artifacts yet. Extraction is a separate change; this repository does not silently re-export or replace Arc's types.

Licensed under the [MIT license](LICENSE).
