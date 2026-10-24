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
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper

class TemporalModuleTest {
    class Properties {
        var date: LocalDate? = null
        var time: LocalTime? = null
    }
    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).addModule(TemporalModule()).build()
    private val date = LocalDate.of(2024, 1, 15)
    private val time = LocalTime.of(10, 30, 45, 123456700)

    private val expected = expectedValues()
    private val collectionTypes = mapOf(
        "string_list" to mapper.typeFactory.constructCollectionType(List::class.java, ConceptsModuleGoldenTest.StringConcept::class.java),
        "guid_list" to mapper.typeFactory.constructCollectionType(List::class.java, ConceptsModuleGoldenTest.GuidConcept::class.java),
        "int_list" to mapper.typeFactory.constructCollectionType(List::class.java, ConceptsModuleGoldenTest.IntConcept::class.java),
        "empty_list" to mapper.typeFactory.constructCollectionType(List::class.java, ConceptsModuleGoldenTest.IntConcept::class.java),
        "time_list" to mapper.typeFactory.constructCollectionType(List::class.java, ConceptCollectionsTest.TimeConcept::class.java),
        "date_array" to mapper.typeFactory.constructArrayType(ConceptsModuleGoldenTest.DateConcept::class.java),
        "int_set" to mapper.typeFactory.constructCollectionType(Set::class.java, ConceptsModuleGoldenTest.IntConcept::class.java)
    )

    @TestFactory
    fun `temporal and collection wire follows the pinned CSharp rows`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/temporal-collections.json")).use { stream ->
            val rows = mapper.readTree(stream).get("cases").toList()
            assertEquals(expected.keys, rows.map { it.get("name").asString() }.toSet())
            assertEquals(expected.size, rows.size)
            rows.map { row ->
                val name = row.get("name").asString()
                DynamicTest.dynamicTest("$name ${row.get("source").asString()}") {
                    val value = expected.getValue(name)
                    val type = collectionTypes[name] ?: mapper.constructType(value.javaClass)
                    val json = row.get("json").asString()
                    val restored = mapper.readValue<Any>(json, type)
                    assertEquals(comparable(value), comparable(restored))
                    if (row.get("direction").asString() == "both") assertEquals(json, mapper.writeValueAsString(value))
                }
            }
        }

    private fun comparable(value: Any): Any = if (value is Array<*>) value.toList() else value

    private fun expectedValues(): Map<String, Any> {
        val whole = LocalTime.of(10, 30)
        val fraction = LocalTime.of(10, 30, 0, 123456700)
        val maximum = LocalTime.of(23, 59, 59, 999999900)
        val temporal = mapOf(
            "time" to whole, "time_concept" to ConceptCollectionsTest.TimeConcept(whole),
            "time_fraction" to fraction, "time_fraction_concept" to ConceptCollectionsTest.TimeConcept(fraction),
            "time_max" to maximum, "time_max_concept" to ConceptCollectionsTest.TimeConcept(maximum),
            "time_seconds" to whole, "time_seconds_concept" to ConceptCollectionsTest.TimeConcept(whole),
            "time_minutes" to whole, "time_minutes_concept" to ConceptCollectionsTest.TimeConcept(whole),
            "date" to date, "date_concept" to ConceptsModuleGoldenTest.DateConcept(date),
            "date_from_local_datetime" to date, "date_from_offset_datetime" to date, "date_from_z_datetime" to date
        )
        return temporal + mapOf(
            "string_list" to listOf(ConceptsModuleGoldenTest.StringConcept("hello"), ConceptsModuleGoldenTest.StringConcept("world")),
            "guid_list" to listOf(ConceptsModuleGoldenTest.GuidConcept(java.util.UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff"))),
            "int_list" to listOf(1, 2, 3).map { ConceptsModuleGoldenTest.IntConcept(it) },
            "empty_list" to emptyList<ConceptsModuleGoldenTest.IntConcept>(),
            "time_list" to listOf(ConceptCollectionsTest.TimeConcept(whole)),
            "date_array" to arrayOf(ConceptsModuleGoldenTest.DateConcept(date)),
            "int_set" to setOf(ConceptsModuleGoldenTest.IntConcept(42))
        )
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
    fun `temporal module always writes strings regardless of timestamp setting`() {
        val timestamps = JsonMapper.builder().enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(TemporalModule()).build()
        val strings = JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(TemporalModule()).build()
        for (configured in listOf(timestamps, strings)) {
            assertEquals("\"2024-01-15\"", configured.writeValueAsString(date))
            assertEquals("\"10:30:45.1234567\"", configured.writeValueAsString(time))
            assertEquals(date, configured.readValue("\"2024-01-15T23:30:00-04:00\"", LocalDate::class.java))
        }
    }

    @Test
    fun `nanoseconds truncate to hundred-nanosecond precision on write`() {
        val nanos = LocalTime.of(10, 30, 45, 123456789)
        assertEquals("\"10:30:45.1234567\"", mapper.writeValueAsString(nanos))
        assertEquals("\"10:30:45.1234567\"", mapper.writeValueAsString(ConceptCollectionsTest.TimeConcept(nanos)))
        assertEquals(nanos, mapper.readValue("\"10:30:45.123456789\"", LocalTime::class.java))
    }
}
