// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.BooleanConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.ByteConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.DateConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.DecimalConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.DoubleConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.GuidConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.IntConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.OffsetConcept
import io.cratis.fundamentals.jackson.ConceptsModuleGoldenTest.StringConcept
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper

class ConceptsModuleTest {
    class Sku private constructor(private val raw: String) : ConceptAs<String> {
        override fun value(): String = raw
        companion object {
            @JvmStatic
            fun of(value: String): Sku = Sku(value)
        }
    }
    class Plain(private val raw: Int) : ConceptAs<Int> {
        override fun value(): Int = raw
    }
    class Missing private constructor() : ConceptAs<String> {
        override fun value(): String = "missing"
    }
    class Competing private constructor(private val raw: Int, val path: String) : ConceptAs<Int> {
        constructor(raw: Int) : this(raw, "constructor")
        override fun value(): Int = raw
        companion object {
            @JvmStatic
            fun of(value: Int): Competing = Competing(value, "factory")
        }
    }
    data class NullableField(val id: GuidConcept?)

    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).build()
    private val uuid = UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff")

    @Test
    fun `null values and nullable list elements stay null`() {
        assertNull(mapper.readValue("null", GuidConcept::class.java))
        assertEquals("{\"id\":null}", mapper.writeValueAsString(NullableField(null)))
        val type = object : TypeReference<List<GuidConcept?>>() {}
        assertEquals(listOf<GuidConcept?>(null), mapper.readValue("[null]", type))
        assertEquals("[null]", mapper.writeValueAsString(listOf<GuidConcept?>(null)))
    }

    @Test
    fun `concept map keys use the underlying text form`() {
        val guidType = object : TypeReference<Map<GuidConcept, Int>>() {}
        val guidMap = mapOf(GuidConcept(uuid) to 1)
        val guidJson = "{\"6f9619ff-8b86-d011-b42d-00c04fc964ff\":1}"
        assertEquals(guidJson, mapper.writeValueAsString(guidMap))
        assertEquals(guidMap, mapper.readValue(guidJson, guidType))

        val stringType = object : TypeReference<Map<StringConcept, Int>>() {}
        assertEquals("{\"plain\":42}", mapper.writeValueAsString(mapOf(StringConcept("plain") to 42)))
        assertEquals(mapOf(StringConcept("plain") to 42), mapper.readValue("{\"plain\":42}", stringType))

        val intType = object : TypeReference<Map<IntConcept, String>>() {}
        assertEquals("{\"42\":\"answer\"}", mapper.writeValueAsString(mapOf(IntConcept(42) to "answer")))
        assertEquals(mapOf(IntConcept(42) to "answer"), mapper.readValue("{\"42\":\"answer\"}", intType))
    }

    @Test
    fun `decimal and temporal map keys use the fixed wire formats`() {
        val decimalType = object : TypeReference<Map<DecimalConcept, Int>>() {}
        val decimal = mapOf(DecimalConcept(BigDecimal("1000")) to 1)
        assertEquals("{\"1000\":1}", mapper.writeValueAsString(decimal))
        assertEquals(decimal, mapper.readValue("{\"1000\":1}", decimalType))
        val dateType = object : TypeReference<Map<DateConcept, Int>>() {}
        val date = mapOf(DateConcept(LocalDate.of(2024, 1, 15)) to 1)
        assertEquals(date, mapper.readValue(mapper.writeValueAsString(date), dateType))
        val offsetType = object : TypeReference<Map<OffsetConcept, Int>>() {}
        val offset = mapOf(OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30Z")) to 1)
        assertEquals("{\"2024-01-15T10:30:00+00:00\":1}", mapper.writeValueAsString(offset))
        assertEquals(offset, mapper.readValue(mapper.writeValueAsString(offset), offsetType))
    }

    @Test
    fun `offset date time reads keep the original offset with a default mapper`() {
        val value = mapper.readValue("\"2024-01-15T10:30:00+02:00\"", OffsetConcept::class.java)
        assertEquals(OffsetDateTime.parse("2024-01-15T10:30:00+02:00"), value.value())
    }

    @Test
    fun `factories precede public single argument constructors`() {
        assertEquals("sku", mapper.readValue("\"sku\"", Sku::class.java).value())
        assertEquals(42, mapper.readValue("42", Plain::class.java).value())
        assertEquals("factory", mapper.readValue("42", Competing::class.java).path)
    }

    @Test
    fun `missing factory reports the class and all accepted shapes`() {
        val error = assertThrows(DatabindException::class.java) {
            mapper.readValue("\"value\"", Missing::class.java)
        }
        val message = checkNotNull(error.message)
        for (text in listOf(Missing::class.java.name, "record", "static of", "single-arg constructor")) {
            assertTrue(message.contains(text), message)
        }
    }

    @Test
    fun `concept lists are scalar and non concept values are unaffected`() {
        assertEquals("[1,2]", mapper.writeValueAsString(listOf(IntConcept(1), IntConcept(2))))
        val type = object : TypeReference<List<IntConcept>>() {}
        assertEquals(listOf(IntConcept(1), IntConcept(2)), mapper.readValue("[1,2]", type))
        assertEquals("{\"ordinary\":42}", mapper.writeValueAsString(mapOf("ordinary" to 42)))
        assertEquals(listOf(1, 2), mapper.readValue("[1,2]", object : TypeReference<List<Int>>() {}))
    }

    @Test
    fun `service loader discovers the module and duplicate registration is harmless`() {
        val automatic = JsonMapper.builder().findAndAddModules().build()
        val duplicate = JsonMapper.builder().addModule(ConceptsModule()).addModule(ConceptsModule()).build()
        for (configured in listOf(automatic, duplicate)) {
            assertEquals("42", configured.writeValueAsString(IntConcept(42)))
            assertEquals(IntConcept(42), configured.readValue("42", IntConcept::class.java))
        }
    }

    @Test
    fun `whole doubles unicode decimals and nanoseconds pin documented deviations`() {
        assertEquals("42.0", mapper.writeValueAsString(DoubleConcept(42.0)))
        assertEquals("\"café\"", mapper.writeValueAsString(StringConcept("café")))
        assertEquals("1000", mapper.writeValueAsString(DecimalConcept(BigDecimal("1E+3"))))
        val value = OffsetConcept(OffsetDateTime.parse("2024-01-15T10:30:00.123456789+02:00"))
        assertEquals("\"2024-01-15T10:30:00.123456789+02:00\"", mapper.writeValueAsString(value))
        assertEquals(value, mapper.readValue(mapper.writeValueAsString(value), OffsetConcept::class.java))
    }

    @Test
    fun `jvm signed bytes and double exponent spelling are deliberate deviations`() {
        assertEquals("-128", mapper.writeValueAsString(ByteConcept(Byte.MIN_VALUE)))
        assertEquals(ByteConcept(Byte.MIN_VALUE), mapper.readValue("-128", ByteConcept::class.java))
        assertEquals("1.0E100", mapper.writeValueAsString(DoubleConcept(1.0e100)))
        assertEquals(DoubleConcept(1.0e100), mapper.readValue("1E+100", DoubleConcept::class.java))
    }

    @Test
    fun `invalid special values and boolean keys fail as databind exceptions`() {
        assertThrows(DatabindException::class.java) { mapper.readValue("\"invalid\"", GuidConcept::class.java) }
        assertThrows(DatabindException::class.java) { mapper.readValue("\"invalid\"", DateConcept::class.java) }
        assertThrows(DatabindException::class.java) { mapper.readValue("\"invalid\"", OffsetConcept::class.java) }
        val type = object : TypeReference<Map<BooleanConcept, Int>>() {}
        assertThrows(DatabindException::class.java) { mapper.readValue("{\"TRUE\":1}", type) }
    }
}
