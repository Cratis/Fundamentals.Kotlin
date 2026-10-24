// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper

class TimeSpanModuleTest {
    data class SpanConcept(private val raw: Duration) : ConceptAs<Duration> {
        override fun value(): Duration = raw
    }
    class Holder { var span: Duration? = null }
    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).addModule(TimeSpanModule()).build()
    private val whole = Duration.ofMinutes(90)
    private val expected = mapOf(
        "whole" to whole, "zero" to Duration.ZERO, "days" to Duration.ofSeconds(93784),
        "fraction" to whole.plusNanos(123456700), "negative" to whole.negated(),
        "negative_fraction" to Duration.ofNanos(-100), "negative_days" to Duration.ofDays(-2),
        "max" to Duration.ofSeconds(922337203685, 477580700), "short_fraction" to Duration.ofMillis(100),
        "number_token" to null, "not_a_timespan" to null, "hours_out_of_range" to null,
        "concept" to SpanConcept(whole), "key" to mapOf(whole to 1)
    )

    @TestFactory
    fun `duration wire follows every pinned STJ row`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/timespan.json")).use { stream ->
            val rows = mapper.readTree(stream).get("cases").toList()
            assertEquals(expected.keys, rows.map { it.get("name").asString() }.toSet())
            assertEquals(expected.size, rows.size)
            rows.map { row ->
                val name = row.get("name").asString()
                val direction = row.get("direction").asString()
                DynamicTest.dynamicTest("$name $direction ${row.get("source").asString()}") {
                    val type = when (name) {
                        "key" -> mapper.typeFactory.constructMapType(Map::class.java, Duration::class.java, Int::class.javaObjectType)
                        "concept" -> mapper.constructType(SpanConcept::class.java)
                        else -> mapper.constructType(Duration::class.java)
                    }
                    val json = row.get("json").asString()
                    if (direction == "reject") {
                        assertThrows(DatabindException::class.java) { mapper.readValue<Any>(json, type) }
                    } else {
                        assertEquals(expected.getValue(name), mapper.readValue<Any>(json, type))
                        if (direction == "both") assertEquals(json, mapper.writeValueAsString(expected.getValue(name)))
                    }
                }
            }
        }

    @Test
    fun `nanoseconds truncate toward zero without rounding or negative zero`() {
        assertEquals("\"00:00:01.2345678\"", mapper.writeValueAsString(Duration.ofNanos(1234567891)))
        assertEquals("\"-00:00:01.2345678\"", mapper.writeValueAsString(Duration.ofNanos(-1234567891)))
        assertEquals("\"00:00:00\"", mapper.writeValueAsString(Duration.ofNanos(-1)))
        assertEquals("\"00:00:01.0000001\"", mapper.writeValueAsString(Duration.ofNanos(1000000100)))
        assertEquals("\"00:00:00.1000000\"", mapper.writeValueAsString(Duration.ofMillis(100)))
    }

    @Test
    fun `canonical reads validate components precision shape and overflow`() {
        for (text in listOf("00:60:00", "00:00:60", "00:00:00.12345678", "00:00:00.123456789", "1", "01:30", "1.2:03:04", "1.02:3:04", "+01:30:00", " 01:30:00", "01:30:00 ", "999999999999999999.00:00:00", "999999999999999999999999999999999.00:00:00")) {
            assertThrows(DatabindException::class.java, { mapper.readValue("\"$text\"", Duration::class.java) }, text)
        }
        for (json in listOf("true", "[]", "{}")) {
            assertThrows(DatabindException::class.java) { mapper.readValue(json, Duration::class.java) }
        }
        assertThrows(DatabindException::class.java) {
            mapper.readValue("{\"bad\":1}", object : TypeReference<Map<Duration, Int>>() {})
        }
    }

    @Test
    fun `ISO is a JVM read leniency and the TimeSpan range is not imposed`() {
        assertEquals(whole, mapper.readValue("\"PT1H30M\"", Duration::class.java))
        assertEquals(whole.negated(), mapper.readValue("\"-PT1H30M\"", Duration::class.java))
        assertEquals(mapOf(whole to 1), mapper.readValue("{\"PT1H30M\":1}", object : TypeReference<Map<Duration, Int>>() {}))
        val large = Duration.ofDays(20000000)
        assertEquals("\"20000000.00:00:00\"", mapper.writeValueAsString(large))
        assertEquals(large, mapper.readValue("\"20000000.00:00:00\"", Duration::class.java))
    }

    @Test
    fun `properties lists and nulls are preserved`() {
        assertEquals("{\"span\":\"01:30:00\"}", mapper.writeValueAsString(Holder().also { it.span = whole }))
        assertEquals(whole, mapper.readValue("{\"span\":\"01:30:00\"}", Holder::class.java).span)
        assertEquals("[\"01:30:00\"]", mapper.writeValueAsString(listOf(whole)))
        assertEquals(listOf(whole), mapper.readValue("[\"01:30:00\"]", object : TypeReference<List<Duration>>() {}))
        assertEquals("{\"span\":null}", mapper.writeValueAsString(Holder()))
        assertEquals(null, mapper.readValue("null", Duration::class.java))
        assertEquals(null, mapper.readValue("{\"span\":null}", Holder::class.java).span)
    }

    @Test
    fun `duration module is opt-in and not discovered`() {
        val plain = JsonMapper.builder().build()
        val concepts = JsonMapper.builder().addModule(ConceptsModule()).build()
        val discovered = JsonMapper.builder().findAndAddModules().build()
        assertEquals(plain.writeValueAsString(whole), discovered.writeValueAsString(whole))
        assertEquals(plain.writeValueAsString(whole), concepts.writeValueAsString(SpanConcept(whole)))
        val raw = JsonMapper.builder().addModule(TimeSpanModule()).build()
        assertEquals("\"01:30:00\"", raw.writeValueAsString(whole))
    }
}
