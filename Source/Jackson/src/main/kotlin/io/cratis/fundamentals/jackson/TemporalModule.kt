// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

/**
 * Explicit, opt-in support for C# `DateOnlyJsonConverter` / `TimeOnlyJsonConverter`
 * wire contracts on raw [LocalDate] and [LocalTime].
 *
 * Dates write ISO `yyyy-MM-dd` and read ISO dates or date-times (local date, no zone shift).
 * Times write `HH:mm:ss.fffffff`, truncating nanoseconds beyond seven digits, and read ISO
 * local times. Invalid tokens/text produce databind errors; null stays null. This module
 * is not auto-discovered and does not change Jackson defaults unless explicitly registered.
 */
public class TemporalModule : SimpleModule("Cratis.Fundamentals.Temporal") {
    init {
        addSerializer(LocalDate::class.java, TemporalSerializer { it.toString() })
        addDeserializer(LocalDate::class.java, TemporalDeserializer(LocalDate::class.java, TemporalWireFormat::parseDate))
        addSerializer(LocalTime::class.java, TemporalSerializer(TemporalWireFormat::formatTime))
        addDeserializer(LocalTime::class.java, TemporalDeserializer(LocalTime::class.java, TemporalWireFormat::parseTime))
    }
}

private class TemporalSerializer<T : Any>(private val format: (T) -> String) : ValueSerializer<T>() {
    override fun serialize(value: T, generator: JsonGenerator, context: SerializationContext) {
        generator.writeString(format(value))
    }
}

private class TemporalDeserializer<T : Any>(
    private val type: Class<T>, private val parse: (String) -> T
) : ValueDeserializer<T>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) return type.cast(context.handleUnexpectedToken(type, parser))
        val text = parser.string
        return try {
            parse(text)
        } catch (exception: DateTimeParseException) {
            type.cast(context.handleWeirdStringValue(type, text, "Invalid ${type.simpleName}: ${exception.message}"))
        }
    }
}
