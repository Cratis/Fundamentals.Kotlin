// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

class ConceptCollectionsTest {
    data class TimeConcept(private val raw: LocalTime) : ConceptAs<LocalTime> {
        override fun value(): LocalTime = raw
    }
    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).build()

    @Test
    fun `CSharp enumerable converter list wire includes common scalar families`() {
        roundTrip(listOf(ConceptsModuleGoldenTest.GuidConcept(UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff"))),
            "[\"6f9619ff-8b86-d011-b42d-00c04fc964ff\"]", object : TypeReference<List<ConceptsModuleGoldenTest.GuidConcept>>() {})
        roundTrip(listOf(ConceptsModuleGoldenTest.IntConcept(42)), "[42]", object : TypeReference<List<ConceptsModuleGoldenTest.IntConcept>>() {})
        roundTrip(listOf(ConceptsModuleGoldenTest.DecimalConcept(BigDecimal("42.43"))), "[42.43]", object : TypeReference<List<ConceptsModuleGoldenTest.DecimalConcept>>() {})
        roundTrip(listOf(ConceptsModuleGoldenTest.StringConcept("hello")), "[\"hello\"]", object : TypeReference<List<ConceptsModuleGoldenTest.StringConcept>>() {})
        roundTrip(listOf(ConceptsModuleGoldenTest.DateConcept(LocalDate.of(2024, 1, 15))), "[\"2024-01-15\"]", object : TypeReference<List<ConceptsModuleGoldenTest.DateConcept>>() {})
        roundTrip(listOf(TimeConcept(LocalTime.of(10, 30))), "[\"10:30:00.0000000\"]", object : TypeReference<List<TimeConcept>>() {})
    }

    @Test
    fun `empty null and null-element lists retain the documented JVM contract`() {
        val type = object : TypeReference<List<ConceptsModuleGoldenTest.IntConcept?>?>() {}
        roundTrip(emptyList(), "[]", type)
        roundTrip(null, "null", type)
        roundTrip(listOf(ConceptsModuleGoldenTest.IntConcept(42), null), "[42,null]", type)
    }

    private fun <T> roundTrip(value: T, json: String, type: TypeReference<T>) {
        assertEquals(json, mapper.writeValueAsString(value))
        assertEquals(value, mapper.readValue(json, type))
    }
}
