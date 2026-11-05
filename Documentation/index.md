---
title: Get started with Fundamentals for Kotlin
description: Add Cratis Fundamentals to a JVM application, wrap a UUID in a typed concept, and round-trip it as scalar JSON with the optional Jackson module.
---

<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

Cratis Fundamentals provides typed domain values for Kotlin and Java applications. Start by giving an order's UUID its own type, then serialize and restore it with Jackson. The core artifact has no Jackson runtime dependency; this example uses the optional Jackson artifact.

## Add the dependencies

Use JDK 17 or later. Repository builds use Kotlin 2.4.21 and Jackson 3.2.4 (`tools.jackson.*`); compatibility with other Kotlin compiler versions is not established here. In your Gradle Kotlin build file, replace `<released-version>` with a published release version:

```kotlin
repositories { mavenCentral() }

val fundamentalsVersion = "<released-version>"
dependencies {
    implementation("io.cratis:fundamentals:$fundamentalsVersion")
    // Optional; also brings in fundamentals:
    implementation("io.cratis:fundamentals-jackson:$fundamentalsVersion")
}
```

## Round-trip a typed identifier

Add this Kotlin entry point to your JVM application. It uses the concept and mapper setup from the [README Kotlin example](../README.md#kotlin):

```kotlin
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.concepts.ConceptFactory
import io.cratis.fundamentals.concepts.value
import io.cratis.fundamentals.jackson.ConceptsModule
import io.cratis.fundamentals.jackson.TemporalModule
import java.util.UUID
import tools.jackson.databind.json.JsonMapper

data class OrderId(private val id: UUID) : ConceptAs<UUID> {
    override fun value(): UUID = id
}

fun main() {
    val id = ConceptFactory.create(OrderId::class.java, "6f9619ff-8b86-d011-b42d-00c04fc964ff")
    println(id.value) // Kotlin extension view of value()

    val mapper = JsonMapper.builder().addModule(ConceptsModule()).addModule(TemporalModule()).build()
    val json = mapper.writeValueAsString(id) // a quoted UUID, not { "value": ... }
    check(mapper.readValue(json, OrderId::class.java) == id)
}
```

Run `main()` from your application or IDE. It prints `6f9619ff-8b86-d011-b42d-00c04fc964ff`; the final check completes without throwing. The serialized value is `"6f9619ff-8b86-d011-b42d-00c04fc964ff"`, not an object with a `value` property. `ConceptsModule` handles the concept conversion; `TemporalModule` opts into date and time formatting and is not needed for the UUID itself.

## Next steps

- [Use the Java example](../README.md#java) for the same JVM API without Kotlin source.
- [Configure construction and Jackson modules](../README.md#jackson-registration-and-construction) for supported scalar types and constructor shapes.
- [Check parity and boundaries](parity.md) before exchanging payloads with .NET or adding modules to an Arc-configured mapper.
