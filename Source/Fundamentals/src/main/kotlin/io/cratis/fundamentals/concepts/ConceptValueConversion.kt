// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.UUID

internal object ConceptValueConversion {
    private val conversions: Map<Class<*>, (Any) -> Any> = mapOf(
        String::class.java to { value -> if (value is BigDecimal) value.toPlainString() else value.toString() },
        UUID::class.java to ::uuid,
        Int::class.javaObjectType to { value -> decimal(value).intValueExact() },
        Long::class.javaObjectType to { value -> decimal(value).longValueExact() },
        Short::class.javaObjectType to { value -> decimal(value).shortValueExact() },
        Byte::class.javaObjectType to { value -> decimal(value).byteValueExact() },
        Double::class.javaObjectType to { value -> numericText(value).toDouble() },
        Float::class.javaObjectType to { value -> numericText(value).toFloat() },
        BigDecimal::class.java to ::decimal,
        Boolean::class.javaObjectType to { value -> (value as String).lowercase(java.util.Locale.ROOT).toBooleanStrict() },
        LocalDate::class.java to { value -> LocalDate.parse(value as String) },
        LocalTime::class.java to { value -> LocalTime.parse(value as String) },
        OffsetDateTime::class.java to { value -> OffsetDateTime.parse(value as String, ISO_OFFSET_DATE_TIME) }
    )

    fun convert(conceptType: Class<*>, valueType: Class<*>, value: Any): Any = try {
        val target = valueType.kotlin.javaObjectType
        if (target.isInstance(value)) return value
        val raw = if (value is ConceptAs<*>) checkNotNull(value.value()) else value
        if (target.isInstance(raw)) raw else checkNotNull(conversions[target])(raw)
    } catch (exception: Exception) {
        throw IllegalArgumentException(
            "Could not convert ${value.javaClass.name} to ${valueType.name} for ${conceptType.name}", exception
        )
    }

    private fun numericText(value: Any): String {
        require(value is Number || value is String) { "Expected a number or string" }
        return value.toString()
    }

    private fun decimal(value: Any): BigDecimal = when (value) {
        is Byte, is Short, is Int, is Long -> BigDecimal.valueOf((value as Number).toLong())
        else -> BigDecimal(numericText(value))
    }

    private fun uuid(value: Any): UUID = when (value) {
        is String -> UUID.fromString(value)
        is ByteArray -> {
            require(value.size == 16) { "A UUID requires 16 bytes" }
            val bytes = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
            val first = bytes.int.toLong() and 0xffffffffL
            val second = bytes.short.toLong() and 0xffffL
            val third = bytes.short.toLong() and 0xffffL
            UUID((first shl 32) or (second shl 16) or third, bytes.order(ByteOrder.BIG_ENDIAN).long)
        }
        else -> throw IllegalArgumentException("Expected a UUID string or byte array")
    }
}
