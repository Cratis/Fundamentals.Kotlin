<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Cratis Fundamentals for Kotlin and Java

Shared domain primitives for JVM applications: typed concepts, correlation identifiers, .NET-compatible name casing and an optional Jackson 3 concept module. The surface includes generated values and patterns/checksums, **not full .NET Fundamentals parity**. See the [parity ledger](Documentation/parity.md) for exact source pins, evidence, differences and follow-up work.

## Requirements and artifacts

- JDK **17** or later; build with JDK 17.
- Kotlin **2.4.21** for Kotlin consumers. Java consumers need no Kotlin source or compiler.
- Jackson **3.2.4** (`tools.jackson.*`), aligned with Spring Boot 4.1, for the optional Jackson artifact. No Spring dependency is required.
- Gradle wrapper **8.14.6** for repository builds; Maven Central for consumption.

| Artifact | Purpose |
| --- | --- |
| `io.cratis:fundamentals` | `ConceptAs`, `ConceptDescriptor`, `ConceptFactory`, `CorrelationId`, thread-bound `CorrelationIdContext`, derived-type annotation/registry, string casing, `NamingPolicy` with `DefaultNamingPolicy`/`CamelCaseNamingPolicy`; no Jackson runtime dependency |
| `io.cratis:fundamentals-jackson` | Jackson 3 `ConceptsModule` (optional embedded JSON keys), opt-in `TemporalModule`, `EnumModule`, `TimeSpanModule` and `DerivedTypesModule`, `AcronymFriendlyCamelCaseNamingStrategy`; includes core and Jackson databind transitively |

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
import io.cratis.fundamentals.concepts.ConceptFactory
import io.cratis.fundamentals.concepts.value
import io.cratis.fundamentals.execution.CorrelationId
import io.cratis.fundamentals.jackson.ConceptsModule
import io.cratis.fundamentals.jackson.TemporalModule
import io.cratis.fundamentals.strings.toCamelCase
import java.util.UUID
import tools.jackson.databind.json.JsonMapper

data class OrderId(private val id: UUID) : ConceptAs<UUID> {
    override fun value(): UUID = id
}

fun main() {
    val id = ConceptFactory.create(OrderId::class.java, "6f9619ff-8b86-d011-b42d-00c04fc964ff")
    println(id.value) // Kotlin extension view of value()
    println(ConceptAs.valueTypeOf(OrderId::class.java)) // UUID
    println(CorrelationId.create()) // random UUID v4; NOT_SET is the all-zero UUID
    println("URLValue".toCamelCase()) // URLValue (acronym guard)

    val mapper = JsonMapper.builder().addModule(ConceptsModule()).addModule(TemporalModule()).build()
    val json = mapper.writeValueAsString(id) // a quoted UUID, not { "value": ... }
    check(mapper.readValue(json, OrderId::class.java) == id)
}
```

## Java

```java
import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.fundamentals.concepts.ConceptFactory;
import io.cratis.fundamentals.execution.CorrelationId;
import io.cratis.fundamentals.jackson.ConceptsModule;
import io.cratis.fundamentals.jackson.TemporalModule;
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
        var mapper = JsonMapper.builder().addModule(new ConceptsModule()).addModule(new TemporalModule()).build();
        var id = ConceptFactory.create(OrderId.class, "6f9619ff-8b86-d011-b42d-00c04fc964ff");
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

`ConceptDescriptor.of(OrderId::class.java)` (Java: `ConceptDescriptor.of(OrderId.class)`) caches the value type, construction and extraction. Construction prefers a record canonical constructor, public static `of(value)` (Kotlin: `@JvmStatic`), then a public single-argument constructor. `ConceptFactory` converts a bounded scalar set; integer conversions reject loss/overflow and invalid values fail rather than becoming defaults.

`ConceptsModule` remains ServiceLoader-discoverable. `TemporalModule` is explicit only: raw LocalDate writes `yyyy-MM-dd`, and raw LocalTime writes `HH:mm:ss.fffffff`. LocalTime concepts always use that seven-digit form, truncating finer nanoseconds; this changes previously delegated LocalTime concept output. Reads are ISO-only, not culture-sensitive. Scalar concept collections use normal Jackson delegation, retaining null elements (unlike C#). See the [fixture provenance](Source/Jackson/src/test/resources/golden/README.md) and ledger for exact bounds.

### Opt-in wire contracts

Existing `ConceptsModule()` and `TemporalModule()` output is unchanged. `EnumModule` writes enum values as ordinal numbers and reads ordinals, numeric strings, case-insensitive names, `@JsonProperty`/`@JsonAlias` and `toString()` text; undefined values fail. Enum map keys keep Jackson's names. Ordinals match C# only for declarations ordered from zero without gaps; flags, explicit values and non-int backing types are not supported.

`TimeSpanModule` writes Duration values and map keys as C# TimeSpan `"c"` strings (`[-][d.]hh:mm:ss[.fffffff]`), truncating nanoseconds toward zero to 100 ns. Reads accept this shape and ISO-8601, not culture-sensitive or short day/hour-minute forms. JVM durations outside the C# TimeSpan range are not restricted.

Use `ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)` to write concept map keys as compact embedded JSON like C# dictionaries, while still reading old plain keys. A string-like key includes inner JSON quotes; legacy text containing a JSON string literal is ambiguous and its decoded value wins. Only concept keys change, not arbitrary complex keys. Enum- and Duration-valued concepts follow the modules registered on the same mapper.

```kotlin
val mapper = JsonMapper.builder()
    .addModule(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON))
    .addModule(EnumModule())
    .addModule(TimeSpanModule())
    .build()
```

```java
var mapper = JsonMapper.builder()
    .addModule(new ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON))
    .addModule(new EnumModule())
    .addModule(new TimeSpanModule())
    .build();
```

Import the types from `io.cratis.fundamentals.jackson`. `EnumModule`, `TimeSpanModule` and `TemporalModule` are never ServiceLoader-discovered; discovery creates the no-arg `ConceptsModule` with `ConceptKeyFormat.PLAIN`. Choose the key option explicitly on that module; registering it before or after EnumModule/TimeSpanModule gives the same wire.

## Derived types (opt-in)

Register every concrete member explicitly; no classpath scanning or Spring is required:

```kotlin
interface Shape
@DerivedType("circle")
data class Circle @JsonCreator constructor(@param:JsonProperty("radius") val radius: Double) : Shape

val types = DerivedTypes.of(Circle::class.java)
val mapper = JsonMapper.builder().addModule(DerivedTypesModule(types)).build()
val json = mapper.writeValueAsString(Circle(2.0)) // {"radius":2.0,"_derivedTypeId":"circle"}
val shape = mapper.readValue(json, Shape::class.java)
```

Use `io.cratis.fundamentals.serialization.DerivedType`/`DerivedTypes` and
`io.cratis.fundamentals.jackson.DerivedTypesModule`; the creator annotations are Jackson's
`com.fasterxml.jackson.annotation` types. Java uses `@DerivedType("circle")` on a record,
`DerivedTypes.of(Circle.class)` and `new DerivedTypesModule(types)`:

```java
sealed interface Shape permits Circle {}
@DerivedType("circle") record Circle(double radius) implements Shape {}
var types = DerivedTypes.of(Circle.class);
var mapper = JsonMapper.builder().addModule(new DerivedTypesModule(types)).build();
var shape = mapper.readValue(mapper.writeValueAsString(new Circle(2.0)), Shape.class);
```

Ids are stable strings, globally unique within the immutable registry, and unrelated to Chronicle
event types. With the default `Any`/`Object` sentinel, the primary target is the single non-JDK
interface (including inherited/transitive interfaces), otherwise the nearest non-JDK superclass.
Multiple interfaces require `@DerivedType(value = "circle", targetType = Shape::class)`
(Java: `targetType = Shape.class`); it must be assignable from the derived class. Non-JDK base
classes are also indexed for multilevel dispatch. Sealed families work but are not auto-registered.

The module is never service-discovered. Combine registrations into one registry per mapper;
a second module instance replaces the first, it does not merge. `DerivedTypes.EMPTY` is a no-op.
Registered runtime classes write `_derivedTypeId` last, including concrete/Object declarations.
Use runtime serialization (`writeValueAsString(value)`); explicitly static interface writers
retain Jackson's static body selection. Ids read in any position and are stripped before bean
delegation. Missing ids (including differently cased names) read as null, just like C#; unknown,
null and non-string ids fail. Annotated but unregistered members of a registered family fail on
write; unannotated implementations retain Jackson behavior. Body naming, creators, annotations,
null inclusion and custom serializers remain Jackson-owned. Nested families in lists, arrays,
maps and recursive objects use ordinary delegation. Native Jackson polymorphism/default typing
is not a replacement contract of this module.

## Generated values

Available in the core artifact, without Jackson. Kotlin and Java share static generators for UUID v4/v7, ULID, TypeID, NanoID, encoded random bytes and inclusive 64-bit integers. Randomness is cryptographically secure; UUID v7 and ULID do not use a monotonic counter.

```kotlin
import io.cratis.fundamentals.concepts.*
import java.time.Instant
import java.util.UUID

class SomeId(private val id: UUID) : ConceptAs<UUID>, Generatable<SomeId> {
    override fun value(): UUID = id
    companion object {
        @JvmStatic fun create(): SomeId = SomeId(GenerateValue.uuid())
    }
}

class OrderId(private val id: String) : ConceptAs<String>, GeneratableInContext<OrderId> {
    override fun value(): String = id
    companion object {
        @JvmStatic fun create(context: GenerationContext): OrderId =
            OrderId(GenerateValue.typeId("order", context))
    }
}

fun generatedValues() {
    val context = GenerationContext(Instant.parse("2024-02-29T10:00:00Z"), "tenant-a")
    val request = SomeId.create()
    val order = OrderId.create(context)
    check(request.value().version() == 4)
    check(order.value().startsWith("order_"))
    val invoice = GenerateValue.pattern("INV-{yyyy}-#{7}", context, Checksum.LUHN)
    check(Checksum.hasValidChecksum(invoice, Checksum.LUHN))
}
```

```java
import io.cratis.fundamentals.concepts.*;
import java.time.Instant;
import java.util.UUID;

class GeneratedValuesExample {
    record SomeId(UUID value) implements ConceptAs<UUID>, Generatable<SomeId> {
        public static SomeId create() { return new SomeId(GenerateValue.uuid()); }
    }
    record OrderId(String value) implements ConceptAs<String>, GeneratableInContext<OrderId> {
        public static OrderId create(GenerationContext context) {
            return new OrderId(GenerateValue.typeId("order", context));
        }
    }
    public static void main(String[] args) {
        var context = new GenerationContext(Instant.parse("2024-02-29T10:00:00Z"), "tenant-a");
        var request = SomeId.create();
        var order = OrderId.create(context);
        var invoice = GenerateValue.pattern("INV-{yyyy}-#{7}", context, Checksum.LUHN);
        if (!GenerateValue.hasValidChecksum(invoice, Checksum.LUHN)) throw new AssertionError();
    }
}
```

`Generatable` and `GeneratableInContext` are memberless markers: JVM interfaces cannot enforce static factories. The `create` convention must be supplied by each concept. `GenerationContext` captures one UTC instant and a portable tenant string (default: zero UUID); built-in generators do not incorporate the tenant. UTC years must be 1–9999. Year 1 midnight is the missing-time sentinel; Unix epoch is valid.

Patterns support `#` (digit), `?` (uppercase letter), `*` (digit/uppercase), sorted distinct classes such as `[A-HJ-NP-Z2-9]`, escapes, `{n}` / `{m,n}` repeats, and captured UTC `{yyyy}`, `{yy}`, `{MM}`, `{dd}` segments. For example, `GenerateValue.pattern("ORD-[A-HJ-NP-Z2-9]{4}-#{4,6}")`. They sample uniformly over distinct strings, not derivations or lengths. Checksum choices are `NONE`, `LUHN`, `MOD11`, `MOD97`; Mod11 excludes payloads requiring a check digit of 10. Validation requires the exact numeric suffix and rejects `NONE`. Date patterns can use pre-epoch dates; UUID v7, ULID and TypeID cannot.

Patterns are limited to 256 final characters (including check digits), repeat bounds 1–64, 2,048 automaton positions, 10,000 DFA/product states and 100,000,000 construction operations. Parsed patterns and counted supports use separate 128-entry caches. Exhausted budgets and invalid input fail with `IllegalArgumentException`; there is no biased fallback. Java null arguments fail at Kotlin's non-null boundary. See [exception mappings and vector evidence](Documentation/parity.md#generated-value-exception-mapping).

## Naming policies

`NamingPolicy` derives storage names from JVM types. `DefaultNamingPolicy` pluralizes the simple class name (English inflection, aligned with Humanizer for `Person`→`People` and `Index`→`Indices`); `CamelCaseNamingPolicy` also camel-cases read-model and member names. `AcronymFriendlyCamelCaseNamingStrategy` (in `fundamentals-jackson`) is an explicit Jackson property naming strategy; it is not registered automatically.

```kotlin
import io.cratis.fundamentals.serialization.CamelCaseNamingPolicy
import io.cratis.fundamentals.serialization.DefaultNamingPolicy

class Person

val collection = DefaultNamingPolicy().getReadModelName(Person::class.java)   // "People"
val camel = CamelCaseNamingPolicy().getReadModelName(Person::class.java)      // "people"
```

```java
import io.cratis.fundamentals.serialization.DefaultNamingPolicy;
import io.cratis.fundamentals.serialization.NamingPolicy;

NamingPolicy policy = new DefaultNamingPolicy(false);      // keep the simple class name
NamingPolicy custom = type -> "app_" + type.getSimpleName(); // getPropertyName defaults to identity
```

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

Concept map keys default to the underlying **text** value, not JSON text inside the property name. Opt into `ConceptKeyFormat.EMBEDDED_JSON` on `ConceptsModule` for C#-style names; outer quote escaping can still differ (`\"` versus `\u0022`) while decoded property names agree. Signed JVM bytes, whole-float/exponent spelling, Unicode escaping, arbitrary-precision decimal range and nanosecond timestamps also differ from .NET. See [wire differences](Documentation/parity.md#wire-and-language-differences) before sharing payloads across runtimes. Delegation for unsupported types does not imply parity.

## Build and test

```sh
./gradlew clean build apiCheck
```

The build compiles Kotlin and Java for JVM 17 with warnings as errors and runs JUnit tests. Public API dumps live in each module's `api/` directory and are checked by Kotlin binary compatibility validator. For an intentional public API change, regenerate with `./gradlew apiDump`, review the diff, then rerun `clean build apiCheck`; never hand-edit the dumps. Run `actionlint -color` after workflow changes. Local AI execution must also follow the repository's [project context](.cratis/ai/rules/project.md) and any parent build-queue policy.

## Releases

Every PR needs exactly one release-intent label: `major`, `minor`, `patch`, or `no-release`. Pushes to `main` touching source/build inputs trigger verification, then `cratis/release-action` and signed publication of **both** artifacts to Maven Central. Documentation-only changes need no artifact release; use `no-release`. Manual dispatch remains available. Publication uses the `maven-central` environment and repository-owner-managed signing/Central credentials; never place credentials in source or command lines. Publication and GitHub release creation stay in one job so failed uploads do not announce a successful release.

## Scope

General ConceptMap/ConceptFactory APIs, temporal/collection converter parity, AsyncLocal-style correlation propagation (`CorrelationIdContext` is thread-bound only) and the broader Fundamentals library are not implemented. Their follow-up issues are linked in the [ledger](Documentation/parity.md#surfaces).

Arc.Kotlin depends on `io.cratis:fundamentals` (concepts, casing, naming policy) and reads this `@DerivedType` annotation (Cratis/Arc.Kotlin#301). Arc keeps its own `_derivedTypeId` registry and codec: per-base registrations generated by its KSP processor, and a missing discriminator is an error. Arc does not use `fundamentals-jackson`; `DerivedTypesModule` is opt-in and is not discovered by service loading, and an Arc-configured mapper does not need it: Arc's codec already writes and reads `_derivedTypeId`. This repository does not silently re-export or replace Arc's types.

Licensed under the [MIT license](LICENSE).
