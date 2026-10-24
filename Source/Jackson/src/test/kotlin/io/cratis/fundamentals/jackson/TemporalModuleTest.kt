// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper

class TemporalModuleTest {
    class Properties {
        var date: LocalDate? = null
        var time: LocalTime? = null
    }
    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).addModule(TemporalModule()).build()
    private val date = LocalDate.of(2024, 1, 15)
    private val time = LocalTime.of(10, 30, 45, 123456700)

    @TestFactory
    fun `temporal and collection wire follows the pinned CSharp rows`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/temporal-collections.json")).use { stream ->
            mapper.readTree(stream).get("cases").toList().map { row ->
                val name = row.get("name").asString()
                DynamicTest.dynamicTest("$name ${row.get("source").asString()}") {
                    val (value, type) = when (row.get("type").asString()) {
                        "date" -> date to mapper.constructType(LocalDate::class.java)
                        "time" -> LocalTime.parse(row.get("value").asString()) to mapper.constructType(LocalTime::class.java)
                        "timeConcept" -> ConceptCollectionsTest.TimeConcept(LocalTime.parse(row.get("value").asString())) to mapper.constructType(ConceptCollectionsTest.TimeConcept::class.java)
                        "intList" -> listOf(ConceptsModuleGoldenTest.IntConcept(42)) to mapper.constructType(object : TypeReference<List<ConceptsModuleGoldenTest.IntConcept>>() {}.type)
                        "emptyList" -> emptyList<Any>() to mapper.constructType(object : TypeReference<List<ConceptsModuleGoldenTest.IntConcept>>() {}.type)
                        else -> error("Unknown fixture type: $name")
                    }
                    val json = row.get("json").asString()
                    assertEquals(value, mapper.readValue<Any>(json, type))
                    if (row.get("direction").asString() == "both") assertEquals(json, mapper.writeValueAsString(value))
                }
            }
        }

    @Test
    fun `raw properties lists and nulls use string scalars`() {
        val value = Properties().also { it.date = date; it.time = time }
        val json = "{\"date\":\"2024-01-15\",\"time\":\"10:30:45.1234567\"}"
        assertEquals(json, mapper.writeValueAsString(value))
        val restored = mapper.readValue(json, Properties::class.java)
        assertEquals(date, restored.date)
        assertEquals(time, restored.time)
        assertEquals("{\"date\":null,\"time\":null}", mapper.writeValueAsString(Properties()))
        assertEquals(null, mapper.readValue("null", LocalDate::class.java))
        assertEquals(null, mapper.readValue("null", LocalTime::class.java))
        assertEquals(null, mapper.readValue("{\"date\":null,\"time\":null}", Properties::class.java).time)
        assertEquals("[\"2024-01-15\"]", mapper.writeValueAsString(listOf(date)))
        assertEquals(listOf(time), mapper.readValue("[\"10:30:45.1234567\"]", object : TypeReference<List<LocalTime>>() {}))
        assertEquals("[\"10:30:45.1234567\"]", mapper.writeValueAsString(listOf(time)))
        assertEquals(listOf(date), mapper.readValue("[\"2024-01-15\"]", object : TypeReference<List<LocalDate>>() {}))
    }

    @Test
    fun `invalid input and non-string tokens produce databind diagnostics`() {
        for (type in listOf(LocalDate::class.java, LocalTime::class.java)) {
            for (json in listOf("\"not-a-date-or-time\"", "42", "true", "[]", "{}")) {
                val error = assertThrows(DatabindException::class.java) { mapper.readValue(json, type) }
                assertTrue(checkNotNull(error.message).contains(type.simpleName), error.message)
            }
        }
    }

    @Test
    fun `explicit and service loaded registration keep temporal opt-in`() {
        val rawMapper = JsonMapper.builder().addModule(TemporalModule()).build()
        assertEquals("\"10:30:45.1234567\"", rawMapper.writeValueAsString(time))
        val discovered = JsonMapper.builder().findAndAddModules().build()
        assertEquals("\"10:30:00.0000000\"", discovered.writeValueAsString(ConceptCollectionsTest.TimeConcept(LocalTime.of(10, 30))))
        // Without TemporalModule raw java.time serialization retains Jackson's built-in configuration.
        assertEquals(JsonMapper.builder().build().writeValueAsString(time), discovered.writeValueAsString(time))
        val discoveredWithTemporal = JsonMapper.builder().findAndAddModules().addModule(TemporalModule()).build()
        assertEquals("\"10:30:45.1234567\"", discoveredWithTemporal.writeValueAsString(time))
    }

    @Test
    fun `time concept and map keys share fixed seven-digit output without embedded quotes`() {
        val value = ConceptCollectionsTest.TimeConcept(time)
        val json = "{\"10:30:45.1234567\":1}"
        assertEquals("\"10:30:45.1234567\"", mapper.writeValueAsString(value))
        assertEquals(value, mapper.readValue("\"10:30:45.1234567\"", ConceptCollectionsTest.TimeConcept::class.java))
        assertEquals(json, mapper.writeValueAsString(mapOf(value to 1)))
        assertEquals(mapOf(value to 1), mapper.readValue(json, object : TypeReference<Map<ConceptCollectionsTest.TimeConcept, Int>>() {}))
    }

    @Test
    fun `nanoseconds truncate to hundred-nanosecond precision on write`() {
        val nanos = LocalTime.of(10, 30, 45, 123456789)
        assertEquals("\"10:30:45.1234567\"", mapper.writeValueAsString(nanos))
        assertEquals("\"10:30:45.1234567\"", mapper.writeValueAsString(ConceptCollectionsTest.TimeConcept(nanos)))
        assertEquals(nanos, mapper.readValue("\"10:30:45.123456789\"", LocalTime::class.java))
    }
}
