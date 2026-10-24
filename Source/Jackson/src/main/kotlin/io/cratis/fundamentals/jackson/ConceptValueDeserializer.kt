// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.concepts.ConceptDescriptor
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.UUID
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.ValueDeserializer

internal class ConceptValueDeserializer(private val conceptType: JavaType) : ValueDeserializer<Any>() {
    private val valueType: JavaType = conceptType.findSuperType(ConceptAs::class.java)?.containedType(0)
        ?: throw IllegalArgumentException("${conceptType.rawClass.name} does not declare a ConceptAs value type")

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Any? {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null
        return try {
            val value = readValue(parser, context)
            ConceptDescriptor.of(conceptType.rawClass).create(checkNotNull(value))
        } catch (exception: Exception) {
            throw DatabindException.from(parser, "Could not create ${conceptType.rawClass.name}: ${exception.message}", exception)
        }
    }

    private fun readValue(parser: JsonParser, context: DeserializationContext): Any? = when (valueType.rawClass) {
        UUID::class.java -> UUID.fromString(text(parser))
        LocalDate::class.java -> LocalDate.parse(text(parser))
        OffsetDateTime::class.java -> OffsetDateTime.parse(text(parser), ISO_OFFSET_DATE_TIME)
        else -> context.readValue<Any?>(parser, valueType)
    }

    private fun text(parser: JsonParser): String {
        require(parser.currentToken() == JsonToken.VALUE_STRING) { "Expected a string for ${valueType.rawClass.name}" }
        return parser.string
    }
}
