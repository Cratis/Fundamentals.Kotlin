// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import java.time.Duration
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.KeyDeserializer
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

/**
 * Opt-in C# TimeSpan "c" strings for Duration values and map keys, truncated toward zero to 100 ns.
 * Based on dotnet/runtime v9.0.0 System.Text.Json TimeSpanConverter.cs:17-95.
 * Not ServiceLoader-discovered. Reads accept canonical "c" and ISO-8601; unlike C#, no TimeSpan
 * range limit is enforced, and short day-only/hour-minute forms are not accepted.
 */
public class TimeSpanModule : SimpleModule("Cratis.Fundamentals.TimeSpan") {
    init {
        addSerializer(Duration::class.java, TemporalSerializer(TimeSpanWireFormat::format))
        addDeserializer(Duration::class.java, TemporalDeserializer(Duration::class.java, TimeSpanWireFormat::parse))
        addKeySerializer(Duration::class.java, TimeSpanKeySerializer)
        addKeyDeserializer(Duration::class.java, TimeSpanKeyDeserializer)
    }
}

private object TimeSpanKeySerializer : ValueSerializer<Duration>() {
    override fun serialize(value: Duration, generator: JsonGenerator, context: SerializationContext) {
        generator.writeName(TimeSpanWireFormat.format(value))
    }
}

private object TimeSpanKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, context: DeserializationContext): Any = try {
        TimeSpanWireFormat.parse(key)
    } catch (exception: Exception) {
        context.handleWeirdKey(Duration::class.java, key, exception.message ?: "Invalid TimeSpan")
    }
}
