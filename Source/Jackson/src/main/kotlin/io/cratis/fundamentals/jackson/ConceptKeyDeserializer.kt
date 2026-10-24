// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.concepts.ConceptDescriptor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.UUID
import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.KeyDeserializer

internal class ConceptKeyDeserializer(private val conceptType: JavaType) : KeyDeserializer() {
    private val valueType: JavaType = conceptType.findSuperType(ConceptAs::class.java)?.containedType(0)
        ?: throw IllegalArgumentException("${conceptType.rawClass.name} does not declare a ConceptAs value type")

    override fun deserializeKey(key: String, context: DeserializationContext): Any = try {
        ConceptDescriptor.of(conceptType.rawClass).create(checkNotNull(parseKey(key, context)))
    } catch (exception: Exception) {
        throw DatabindException.from(context, "Could not create ${conceptType.rawClass.name} key: ${exception.message}", exception)
    }

    private fun parseKey(key: String, context: DeserializationContext): Any? = when (valueType.rawClass) {
        String::class.java -> key
        UUID::class.java -> UUID.fromString(key)
        Int::class.javaObjectType -> key.toInt()
        Long::class.javaObjectType -> key.toLong()
        Short::class.javaObjectType -> key.toShort()
        Byte::class.javaObjectType -> key.toByte()
        Double::class.javaObjectType -> key.toDouble()
        Float::class.javaObjectType -> key.toFloat()
        BigDecimal::class.java -> BigDecimal(key)
        Boolean::class.javaObjectType -> key.toBooleanStrict()
        LocalDate::class.java -> LocalDate.parse(key)
        OffsetDateTime::class.java -> OffsetDateTime.parse(key, ISO_OFFSET_DATE_TIME)
        else -> context.findKeyDeserializer(valueType, null).deserializeKey(key, context)
    }
}
