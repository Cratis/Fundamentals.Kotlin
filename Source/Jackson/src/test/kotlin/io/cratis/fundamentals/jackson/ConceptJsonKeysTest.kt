// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.core.JsonGenerator
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DatabindException
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule

class ConceptJsonKeysTest {
    private val mapper = JsonMapper.builder().addModule(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)).addModule(EnumModule())
        .addModule(TimeSpanModule()).build()
    private val plain = JsonMapper.builder().addModule(ConceptsModule()).build()
    private val textMap = object : TypeReference<Map<ConceptsModuleGoldenTest.StringConcept, Int>>() {}
    private val expected = expectedMaps()
    private val keyTypes = mapOf(
        "string" to ConceptsModuleGoldenTest.StringConcept::class.java,
        "guid" to ConceptsModuleGoldenTest.GuidConcept::class.java,
        "int" to ConceptsModuleGoldenTest.IntConcept::class.java,
        "decimal" to ConceptsModuleGoldenTest.DecimalConcept::class.java,
        "date" to ConceptsModuleGoldenTest.DateConcept::class.java,
        "time" to ConceptCollectionsTest.TimeConcept::class.java,
        "offset" to ConceptsModuleGoldenTest.OffsetConcept::class.java,
        "enum" to EnumModuleTest.RankConcept::class.java,
        "duration" to TimeSpanModuleTest.SpanConcept::class.java,
        "empty" to ConceptsModuleGoldenTest.StringConcept::class.java,
        "null" to ConceptsModuleGoldenTest.StringConcept::class.java
    )

    @TestFactory
    fun `dictionary wire follows every pinned CSharp row`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/dictionaries.json")).use { stream ->
            val rows = mapper.readTree(stream).get("cases").toList()
            assertEquals(expected.keys, rows.map { it.get("name").asString() }.toSet())
            assertEquals(expected.size, rows.size)
            rows.map { row ->
                val name = row.get("name").asString()
                DynamicTest.dynamicTest("$name ${row.get("direction").asString()} ${row.get("source").asString()}") {
                    val configured = if (name == "concept_values") plain else mapper
                    val type = if (name == "concept_values") {
                        mapper.typeFactory.constructMapType(Map::class.java, String::class.java, ConceptsModuleGoldenTest.IntConcept::class.java)
                    } else mapper.typeFactory.constructMapType(Map::class.java, keyTypes.getValue(name), Int::class.javaObjectType)
                    val json = row.get("json").asString()
                    assertEquals(expected.getValue(name), configured.readValue<Any?>(json, type))
                    assertEquals(configured.readTree(json), configured.readTree(configured.writeValueAsString(expected.getValue(name))))
                }
            }
        }

    private fun expectedMaps(): Map<String, Any?> = mapOf(
        "string" to linkedMapOf(ConceptsModuleGoldenTest.StringConcept("one") to 1, ConceptsModuleGoldenTest.StringConcept("two") to 2, ConceptsModuleGoldenTest.StringConcept("three") to 3),
        "guid" to mapOf(ConceptsModuleGoldenTest.GuidConcept(UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff")) to 1),
        "int" to mapOf(ConceptsModuleGoldenTest.IntConcept(42) to 1),
        "decimal" to mapOf(ConceptsModuleGoldenTest.DecimalConcept(BigDecimal("42.43")) to 1),
        "date" to mapOf(ConceptsModuleGoldenTest.DateConcept(LocalDate.of(2024, 1, 15)) to 1),
        "time" to mapOf(ConceptCollectionsTest.TimeConcept(LocalTime.of(10, 30)) to 1),
        "offset" to mapOf(ConceptsModuleGoldenTest.OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30:00Z")) to 1),
        "enum" to mapOf(EnumModuleTest.RankConcept(EnumModuleTest.Rank.TWO) to 1),
        "duration" to mapOf(TimeSpanModuleTest.SpanConcept(Duration.ofMinutes(90)) to 1),
        "empty" to emptyMap<ConceptsModuleGoldenTest.StringConcept, Int>(), "null" to null,
        "concept_values" to mapOf("answer" to ConceptsModuleGoldenTest.IntConcept(42))
    )

    @Test
    fun `all existing text-key scalar families remain readable`() {
        for (name in listOf("string", "guid", "int", "decimal", "date", "time", "offset")) {
            val type = mapper.typeFactory.constructMapType(Map::class.java, keyTypes.getValue(name), Int::class.javaObjectType)
            assertEquals(expected.getValue(name), mapper.readValue<Any>(plain.writeValueAsString(expected.getValue(name)), type))
        }
        val enumType = object : TypeReference<Map<EnumModuleTest.RankConcept, Int>>() {}
        assertEquals(expected.getValue("enum"), mapper.readValue("{\"TWO\":1}", enumType))
        val durationType = object : TypeReference<Map<TimeSpanModuleTest.SpanConcept, Int>>() {}
        assertEquals(expected.getValue("duration"), mapper.readValue("{\"PT1H30M\":1}", durationType))
    }

    @Test
    fun `embedded strings round trip punctuation whitespace and JSON-looking text`() {
        for (raw in listOf("a\"b\\c", "", " leading ", "\"one\"", "42", "null", "true", "[]", "{}", "\"one\" \"two\"")) {
            val value = mapOf(ConceptsModuleGoldenTest.StringConcept(raw) to 1)
            assertEquals(value, mapper.readValue(mapper.writeValueAsString(value), textMap))
        }
        assertEquals(mapOf(ConceptsModuleGoldenTest.StringConcept("one") to 1), mapper.readValue("{\"\\\"one\\\"\":1}", textMap))
        assertEquals(mapOf(ConceptsModuleGoldenTest.StringConcept("null") to 1), mapper.readValue("{\"null\":1}", textMap))
        val trailing = mapOf(ConceptsModuleGoldenTest.StringConcept("\"one\" \"two\"") to 1)
        assertEquals(trailing, mapper.readValue(plain.writeValueAsString(trailing), textMap))
        val invalid = mapOf(ConceptsModuleGoldenTest.StringConcept("{broken") to 1)
        assertEquals(invalid, mapper.readValue(plain.writeValueAsString(invalid), textMap))
    }

    @Test
    fun `unparseable scalar keys fail and null values survive`() {
        val ints = object : TypeReference<Map<ConceptsModuleGoldenTest.IntConcept, Int?>>() {}
        assertThrows(DatabindException::class.java) { mapper.readValue("{\"bad\":1}", ints) }
        val value = mapOf(ConceptsModuleGoldenTest.IntConcept(42) to null)
        assertEquals("{\"42\":null}", mapper.writeValueAsString(value))
        assertEquals(value, mapper.readValue("{\"42\":null}", ints))
    }

    @Test
    fun `key format defaults to plain and is independent of other module order`() {
        val value = expected.getValue("string")
        assertEquals("{\"one\":1,\"two\":2,\"three\":3}", plain.writeValueAsString(value))
        val explicitPlain = JsonMapper.builder().addModule(ConceptsModule(ConceptKeyFormat.PLAIN)).build()
        assertEquals(plain.writeValueAsString(value), explicitPlain.writeValueAsString(value))
        for (modules in listOf(
            listOf(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)),
            listOf(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON), EnumModule(), TimeSpanModule()),
            listOf(EnumModule(), TimeSpanModule(), ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)),
            listOf(TimeSpanModule(), ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON), EnumModule()),
            listOf(EnumModule(), ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON), TimeSpanModule()),
            listOf(TimeSpanModule(), EnumModule(), ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)),
            listOf(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON), TimeSpanModule(), EnumModule())
        )) {
            val configured = JsonMapper.builder().addModules(modules).build()
            assertEquals(mapper.writeValueAsString(value), configured.writeValueAsString(value))
            assertEquals(value, configured.readValue(configured.writeValueAsString(value), textMap))
            if (modules.size == 3) {
                for (name in listOf("enum", "duration")) {
                    val type = configured.typeFactory.constructMapType(Map::class.java, keyTypes.getValue(name), Int::class.javaObjectType)
                    val json = configured.writeValueAsString(expected.getValue(name))
                    assertEquals(mapper.writeValueAsString(expected.getValue(name)), json)
                    assertEquals(expected.getValue(name), configured.readValue<Any>(json, type))
                }
            }
        }
    }

    @Test
    fun `opt-in keys do not alter scalar concepts nonconcept keys or discovery`() {
        val raw = ConceptsModuleGoldenTest.StringConcept("one")
        assertEquals(plain.writeValueAsString(raw), mapper.writeValueAsString(raw))
        val discovered = JsonMapper.builder().findAndAddModules().build()
        assertEquals(plain.writeValueAsString(expected.getValue("string")), discovered.writeValueAsString(expected.getValue("string")))
        val baseline = JsonMapper.builder().addModule(TimeSpanModule()).build()
        for (key in listOf("one", UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff"), Duration.ofMinutes(90))) {
            assertEquals(baseline.writeValueAsString(mapOf(key to 1)), mapper.writeValueAsString(mapOf(key to 1)))
        }
        val noEnum = JsonMapper.builder().addModule(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON)).build()
        val key = EnumModuleTest.RankConcept(EnumModuleTest.Rank.TWO)
        val expectedName = plain.writeValueAsString(key)
        assertEquals(expectedName, noEnum.readTree(noEnum.writeValueAsString(mapOf(key to 1))).properties().first().key)
    }

    data class ObjectValue(val x: Int, val y: Int)
    data class ObjectConcept(private val raw: ObjectValue) : ConceptAs<ObjectValue> {
        override fun value(): ObjectValue = raw
    }

    @Test
    fun `embedded key uses current serializers but never outer indentation`() {
        val custom = SimpleModule().addSerializer(ObjectValue::class.java, object : ValueSerializer<ObjectValue>() {
            override fun serialize(value: ObjectValue, generator: JsonGenerator, context: SerializationContext) {
                generator.writeStartObject()
                generator.writeNumberProperty("x", value.x)
                generator.writeNumberProperty("y", value.y)
                generator.writeEndObject()
            }
        })
        val pretty = JsonMapper.builder().addModule(custom).addModule(ConceptsModule(ConceptKeyFormat.EMBEDDED_JSON))
            .enable(SerializationFeature.INDENT_OUTPUT).build()
        val tree = pretty.readTree(pretty.writeValueAsString(mapOf(ObjectConcept(ObjectValue(1, 2)) to 1)))
        assertEquals("{\"x\":1,\"y\":2}", tree.properties().first().key)
    }
}
