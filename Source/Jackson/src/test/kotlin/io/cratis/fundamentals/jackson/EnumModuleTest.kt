// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.cratis.fundamentals.concepts.ConceptAs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper

class EnumModuleTest {
    enum class Rank { UNDEFINED, ONE, TWO, THREE }
    data class RankConcept(private val raw: Rank) : ConceptAs<Rank> {
        override fun value(): Rank = raw
    }
    enum class Named {
        @JsonProperty("first") @JsonAlias("uno", "eins") ONE,
        @JsonProperty("") TWO;
        override fun toString(): String = "display-$name"
    }
    enum class Collision {
        FIRST,
        @JsonAlias("first") SECOND,
        @JsonProperty("FIRST") THIRD
    }
    enum class WithBody {
        FIRST { override fun toString(): String = "body" }, SECOND
    }
    enum class WithValue {
        FIRST, SECOND;
        @JsonValue fun wire(): String = "custom-$name"
    }
    class Holder { var rank: Rank? = null }

    private val mapper = JsonMapper.builder().addModule(ConceptsModule()).addModule(EnumModule()).build()
    private val expected = mapOf(
        "number" to Rank.TWO, "name" to Rank.THREE, "name_lowercase" to Rank.THREE,
        "numeric_string" to Rank.TWO, "unknown_number" to null, "unknown_string" to null,
        "array" to null, "object" to null, "fractional" to null,
        "concept_number" to RankConcept(Rank.TWO), "concept_name" to RankConcept(Rank.TWO)
    )

    @TestFactory
    fun `enum wire follows every pinned CSharp row`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/enums.json")).use { stream ->
            val rows = mapper.readTree(stream).get("cases").toList()
            assertEquals(expected.keys, rows.map { it.get("name").asString() }.toSet())
            assertEquals(expected.size, rows.size)
            rows.map { row ->
                val name = row.get("name").asString()
                val direction = row.get("direction").asString()
                DynamicTest.dynamicTest("$name $direction ${row.get("source").asString()}") {
                    val type = if (name.startsWith("concept_")) RankConcept::class.java else Rank::class.java
                    val json = row.get("json").asString()
                    if (direction == "reject") {
                        val error = assertThrows(DatabindException::class.java) { mapper.readValue(json, type) }
                        if (name == "fractional") assertTrue(checkNotNull(error.message).contains("1.5"))
                    } else {
                        assertEquals(expected.getValue(name), mapper.readValue(json, type))
                        if (direction == "both") assertEquals(json, mapper.writeValueAsString(expected.getValue(name)))
                    }
                }
            }
        }

    @Test
    fun `undefined ordinals including numeric strings fail without coercion`() {
        for (json in listOf("\"99\"", "-1", "2147483648", "999999999999999999999999999999", "true")) {
            assertThrows(DatabindException::class.java) { mapper.readValue(json, Rank::class.java) }
        }
        assertEquals(Rank.TWO, mapper.readValue("\" 2 \"", Rank::class.java))
    }

    @Test
    fun `annotations names and display text accept exact and folded spellings`() {
        for (text in listOf("first", "uno", "eins", "ONE", "display-ONE", "EINS", " display-one ")) {
            assertEquals(Named.ONE, mapper.readValue("\"$text\"", Named::class.java))
        }
        assertEquals(Named.TWO, mapper.readValue("\"TWO\"", Named::class.java))
        assertThrows(DatabindException::class.java) { mapper.readValue("\"\"", Named::class.java) }
        assertEquals(Collision.SECOND, mapper.readValue("\"first\"", Collision::class.java))
        assertEquals(Collision.THIRD, mapper.readValue("\"FIRST\"", Collision::class.java))
        assertEquals(Collision.SECOND, mapper.readValue("\"FiRsT\"", Collision::class.java))
    }

    @Test
    fun `constant subclasses and JsonValue still use ordinals`() {
        assertEquals("0", mapper.writeValueAsString(WithBody.FIRST))
        assertEquals(WithBody.FIRST, mapper.readValue("0", WithBody::class.java))
        assertEquals(WithBody.FIRST, mapper.readValue("\"body\"", WithBody::class.java))
        assertEquals("1", mapper.writeValueAsString(WithValue.SECOND))
        assertEquals(WithValue.SECOND, mapper.readValue("1", WithValue::class.java))
    }

    @Test
    fun `lists and null properties retain their structure`() {
        assertEquals("[1,2]", mapper.writeValueAsString(listOf(Rank.ONE, Rank.TWO)))
        assertEquals("{\"rank\":null}", mapper.writeValueAsString(Holder()))
        assertEquals(null, mapper.readValue("{\"rank\":null}", Holder::class.java).rank)
        assertEquals(null, mapper.readValue("null", Rank::class.java))
    }

    @Test
    fun `enum module is opt-in and leaves map keys and existing concept delegation unchanged`() {
        val plain = JsonMapper.builder().build()
        val concepts = JsonMapper.builder().addModule(ConceptsModule()).build()
        val discovered = JsonMapper.builder().findAndAddModules().build()
        val keys = mapOf(Rank.ONE to 1, Rank.TWO to 2)
        assertEquals(plain.writeValueAsString(keys), mapper.writeValueAsString(keys))
        assertEquals(plain.writeValueAsString(Rank.TWO), concepts.writeValueAsString(RankConcept(Rank.TWO)))
        assertEquals(plain.writeValueAsString(Rank.TWO), discovered.writeValueAsString(Rank.TWO))
    }
}
