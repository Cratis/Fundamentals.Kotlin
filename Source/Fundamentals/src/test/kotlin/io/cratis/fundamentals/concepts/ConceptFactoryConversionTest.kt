// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ConceptFactoryConversionTest {
    private data class Guid(private val raw: UUID) : ConceptAs<UUID> { override fun value(): UUID = raw }
    private data class Text(private val raw: String) : ConceptAs<String> { override fun value(): String = raw }
    private data class IntValue(private val raw: Int) : ConceptAs<Int> { override fun value(): Int = raw }
    private data class LongValue(private val raw: Long) : ConceptAs<Long> { override fun value(): Long = raw }
    private data class ShortValue(private val raw: Short) : ConceptAs<Short> { override fun value(): Short = raw }
    private data class ByteValue(private val raw: Byte) : ConceptAs<Byte> { override fun value(): Byte = raw }
    private data class DoubleValue(private val raw: Double) : ConceptAs<Double> { override fun value(): Double = raw }
    private data class FloatValue(private val raw: Float) : ConceptAs<Float> { override fun value(): Float = raw }
    private data class Decimal(private val raw: BigDecimal) : ConceptAs<BigDecimal> { override fun value(): BigDecimal = raw }
    private data class BooleanValue(private val raw: Boolean) : ConceptAs<Boolean> { override fun value(): Boolean = raw }
    private data class Date(private val raw: LocalDate) : ConceptAs<LocalDate> { override fun value(): LocalDate = raw }
    private data class Time(private val raw: LocalTime) : ConceptAs<LocalTime> { override fun value(): LocalTime = raw }
    private data class Offset(private val raw: OffsetDateTime) : ConceptAs<OffsetDateTime> { override fun value(): OffsetDateTime = raw }
    private data class Unsupported(private val raw: List<String>) : ConceptAs<List<String>> {
        override fun value(): List<String> = raw
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conversions")
    fun `bounded scalar conversions follow the pinned CSharp branches`(
        source: String, type: Class<out ConceptAs<*>>, input: Any, expected: Any
    ) {
        val actual = ConceptFactory.create(type, input).value()
        if (expected is BigDecimal) {
            assertEquals(0, expected.compareTo(actual as BigDecimal), source)
        } else {
            assertEquals(expected, actual, source)
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failures")
    fun `conversion failures identify concept value type and input class`(
        source: String, type: Class<out ConceptAs<*>>, input: Any
    ) {
        val error = assertThrows(IllegalArgumentException::class.java) { ConceptFactory.create(type, input) }
        for (text in listOf(type.name, ConceptAs.valueTypeOf(type).name, input.javaClass.name)) {
            assertTrue(checkNotNull(error.message).contains(text), "$source: ${error.message}")
        }
    }

    @Test
    fun `TypeConversion 75-78 preserves an already typed value instance`() {
        val value = BigDecimal("123.4500")
        assertSame(value, ConceptFactory.create(Decimal::class.java, value).value())
        val list = listOf("identity only")
        assertSame(list, ConceptFactory.create(Unsupported::class.java, list).value())
    }

    companion object {
        private val uuid = UUID.fromString("4ab92720-3138-4a7b-a7e9-2a49f6624736")
        @JvmStatic
        fun conversions(): List<Arguments> = listOf(
            Arguments.of("for_ConceptFactory/when_creating_instance_of_guid_concept_with_value_as_string.cs; TypeConversion.cs:86-89", Guid::class.java, uuid.toString().uppercase(), uuid),
            Arguments.of("for_ConceptFactory/when_creating_instance_of_guid_concept_from_a_byte_array.cs; TypeConversion.cs:92", Guid::class.java, byteArrayOf(0x20, 0x27, 0xb9.toByte(), 0x4a, 0x38, 0x31, 0x7b, 0x4a, 0xa7.toByte(), 0xe9.toByte(), 0x2a, 0x49, 0xf6.toByte(), 0x62, 0x47, 0x36), uuid),
            Arguments.of("for_ConceptFactory/when_creating_instance_of_long_concept_with_value_as_string.cs; TypeConversion.cs:207-216", LongValue::class.java, "42", 42L),
            Arguments.of("for_ConceptFactory/when_creating_instance_of_decimal_concept_with_coming_in_as_decimal.cs", Decimal::class.java, 5.0, BigDecimal(5)),
            Arguments.of("TypeConversion.cs:215 Int to Long", LongValue::class.java, 42, 42L),
            Arguments.of("TypeConversion.cs:215 Int to Short", ShortValue::class.java, 42, 42.toShort()),
            Arguments.of("TypeConversion.cs:215 Int to Byte", ByteValue::class.java, 42, 42.toByte()),
            Arguments.of("TypeConversion.cs:215 Long to Int", IntValue::class.java, 42L, 42),
            Arguments.of("TypeConversion.cs:215 text to Double", DoubleValue::class.java, "4.5", 4.5),
            Arguments.of("TypeConversion.cs:215 number to Double", DoubleValue::class.java, 42, 42.0),
            Arguments.of("TypeConversion.cs:215 text to Float", FloatValue::class.java, "4.5", 4.5f),
            Arguments.of("TypeConversion.cs:215 number to Float", FloatValue::class.java, 42, 42.0f),
            Arguments.of("TypeConversion.cs:215 integral to decimal", Decimal::class.java, 42L, BigDecimal(42)),
            Arguments.of("TypeConversion.cs:215 text to decimal", Decimal::class.java, "42.5", BigDecimal("42.5")),
            Arguments.of("TypeConversion.cs:132-145 date", Date::class.java, "2024-01-15", LocalDate.of(2024, 1, 15)),
            Arguments.of("TypeConversion.cs:147-161 time", Time::class.java, "10:30:00.1234567", LocalTime.parse("10:30:00.1234567")),
            Arguments.of("TypeConversion.cs:164-176 offset", Offset::class.java, "2024-01-15T10:30:00+02:00", OffsetDateTime.parse("2024-01-15T10:30:00+02:00")),
            Arguments.of("TypeConversion.cs:215 boolean", BooleanValue::class.java, "TRUE", true),
            Arguments.of("TypeConversion.cs:215 boolean false", BooleanValue::class.java, "FALSE", false),
            Arguments.of("TypeConversion.cs:95-98 UUID text", Text::class.java, uuid, uuid.toString()),
            Arguments.of("TypeConversion.cs:209-211 numeric text", Text::class.java, 42, "42"),
            Arguments.of("TypeConversion.cs:209-211 decimal plain text", Text::class.java, BigDecimal("1E+3"), "1000"),
            Arguments.of("TypeConversion.cs:69-72 concept unwrap", IntValue::class.java, IntValue(42), 42)
        )

        @JvmStatic
        fun failures(): List<Arguments> = listOf(
            Arguments.of("TypeConversion.cs:90-93 invalid UUID (JVM fails)", Guid::class.java, "x"),
            Arguments.of("TypeConversion.cs:142-145 invalid date (JVM fails)", Date::class.java, "x"),
            Arguments.of("TypeConversion.cs:215 invalid integer", LongValue::class.java, "x"),
            Arguments.of("TypeConversion.cs:215 fractional integer (JVM exact)", IntValue::class.java, 4.5),
            Arguments.of("TypeConversion.cs:215 overflowing integer", IntValue::class.java, Long.MAX_VALUE),
            Arguments.of("TypeConversion.cs:92 invalid UUID bytes", Guid::class.java, ByteArray(15)),
            Arguments.of("TypeConversion.cs:224 unsupported target", Unsupported::class.java, "list"),
            Arguments.of("TypeConversion.cs:215 invalid boolean", BooleanValue::class.java, "yes")
        )
    }
}
