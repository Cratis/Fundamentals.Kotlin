// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import tools.jackson.databind.json.JsonMapper

class ConceptsModuleGoldenTest {
    data class GuidConcept(private val raw: UUID) : ConceptAs<UUID> {
        override fun value(): UUID = raw
    }
    data class StringConcept(private val raw: String) : ConceptAs<String> {
        override fun value(): String = raw
    }
    data class IntConcept(private val raw: Int) : ConceptAs<Int> {
        override fun value(): Int = raw
    }
    data class LongConcept(private val raw: Long) : ConceptAs<Long> {
        override fun value(): Long = raw
    }
    data class ShortConcept(private val raw: Short) : ConceptAs<Short> {
        override fun value(): Short = raw
    }
    data class ByteConcept(private val raw: Byte) : ConceptAs<Byte> {
        override fun value(): Byte = raw
    }
    data class DoubleConcept(private val raw: Double) : ConceptAs<Double> {
        override fun value(): Double = raw
    }
    data class FloatConcept(private val raw: Float) : ConceptAs<Float> {
        override fun value(): Float = raw
    }
    data class DecimalConcept(private val raw: BigDecimal) : ConceptAs<BigDecimal> {
        override fun value(): BigDecimal = raw
    }
    data class BooleanConcept(private val raw: Boolean) : ConceptAs<Boolean> {
        override fun value(): Boolean = raw
    }
    data class DateConcept(private val raw: LocalDate) : ConceptAs<LocalDate> {
        override fun value(): LocalDate = raw
    }
    data class OffsetConcept(private val raw: OffsetDateTime) : ConceptAs<OffsetDateTime> {
        override fun value(): OffsetDateTime = raw
    }

    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).build()
    private val uuid = UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff")
    private val expected = mapOf(
        "guid" to GuidConcept(uuid),
        "guid_upper" to GuidConcept(uuid),
        "string" to StringConcept("hello"),
        "int" to IntConcept(42),
        "int_max" to IntConcept(Int.MAX_VALUE),
        "int_min" to IntConcept(Int.MIN_VALUE),
        "long" to LongConcept(42L),
        "long_max" to LongConcept(Long.MAX_VALUE),
        "long_min" to LongConcept(Long.MIN_VALUE),
        "short" to ShortConcept(42),
        "short_max" to ShortConcept(Short.MAX_VALUE),
        "byte" to ByteConcept(42),
        "double" to DoubleConcept(42.43),
        "double_whole" to DoubleConcept(42.0),
        "double_exponent" to DoubleConcept(1.0e100),
        "float" to FloatConcept(42.43f),
        "decimal" to DecimalConcept(BigDecimal("42.43")),
        "boolean_true" to BooleanConcept(true),
        "boolean_false" to BooleanConcept(false),
        "date" to DateConcept(LocalDate.of(2024, 1, 15)),
        "offset" to OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30:00+02:00")),
        "offset_fraction" to OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30:00.1234567+02:00")),
        "offset_utc" to OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30:00Z")),
        "offset_z" to OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30:00Z"))
    )

    @TestFactory
    fun `concepts match the pinned dotnet wire fixtures`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/concepts.json")).use { stream ->
            val rows = mapper.readTree(stream).get("cases").toList()
            assertEquals(expected.keys, rows.map { it.get("name").asString() }.toSet())
            rows.map { row ->
                val name = row.get("name").asString()
                DynamicTest.dynamicTest("$name ${row.get("source").asString()}") {
                    val concept = expected.getValue(name)
                    val json = row.get("json").asString()
                    assertEquals(concept, mapper.readValue(json, concept.javaClass))
                    if (row.get("direction").asString() == "both") {
                        assertEquals(json, mapper.writeValueAsString(concept))
                    }
                }
            }
        }
}
