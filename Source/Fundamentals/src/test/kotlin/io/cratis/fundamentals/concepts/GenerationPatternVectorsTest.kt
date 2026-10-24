// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.databind.JsonNode

class GenerationPatternVectorsTest {
    @Test
    fun `all runtime pattern vector rows are consumed`() {
        assertEquals(19, supports().size)
        assertEquals(25, invalids().size)
        assertEquals(10, memberships().size)
    }

    @ParameterizedTest
    @MethodSource("supports")
    fun `counts distinct checksum conditioned Screenplay support`(row: JsonNode) {
        val pattern = GenerationPattern.parse(GenerationVectors.text(row, "pattern"), GenerationVectors.algorithm(row))
        val support = GenerationPatternSupport(pattern, GenerationVectors.date(row))
        assertEquals(BigInteger(GenerationVectors.text(row, "count")), support.count)
    }

    @ParameterizedTest
    @MethodSource("invalids")
    fun `rejects invalid Screenplay patterns with original reason`(row: JsonNode) {
        val pattern = GenerationVectors.text(row, "pattern")
        val exception = assertThrows(IllegalArgumentException::class.java) { GenerateValue.pattern(pattern, GenerationVectors.algorithm(row)) }
        assertTrue(exception.message!!.contains(pattern))
        assertTrue(exception.message!!.contains(GenerationVectors.text(row, "reason")))
    }

    @ParameterizedTest
    @MethodSource("memberships")
    fun `matches Screenplay payload and suffix shapes without validating arithmetic`(row: JsonNode) {
        val pattern = GenerationPattern.parse(GenerationVectors.text(row, "pattern"), GenerationVectors.algorithm(row))
        assertEquals(row.get("valid").asBoolean(), GenerationPatternSupport.matches(pattern, GenerationVectors.text(row, "value"), GenerationVectors.instant(row)))
    }

    @Test
    fun `date bearing support requires a captured UTC date`() {
        assertThrows(IllegalArgumentException::class.java) {
            GenerationPatternSupport(GenerationPattern.parse("{yyyy}#"))
        }
    }

    @Test
    fun `exhausted operation budget is reported before work`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            GenerationPatternSupport(GenerationPattern.parse("#"), budget = GenerationWorkBudget("#", 0))
        }
        assertTrue(exception.message!!.contains("Generation support operation budget (0 operations) exceeded."))
    }

    @ParameterizedTest
    @MethodSource("smallSupports")
    fun `unranking bijectively enumerates every admissible string`(text: String, checksum: Checksum) {
        val instant = Instant.parse("2024-02-29T00:00:00Z")
        val pattern = GenerationPattern.parse(text, checksum)
        val support = GenerationPatternSupport(pattern, LocalDate.parse("2024-02-29"))
        val values = mutableSetOf<String>()
        for (index in 0 until support.count.intValueExact()) {
            val value = support.unrank(BigInteger.valueOf(index.toLong()))
            assertTrue(values.add(value), "Duplicate $value for $text")
            assertTrue(GenerationPatternSupport.matches(pattern, value, instant), "Not a member: $value")
            if (checksum != Checksum.NONE) assertTrue(GenerateValue.hasValidChecksum(value, checksum))
        }
        assertTrue(support.count > BigInteger.ZERO)
        assertEquals(support.count, BigInteger.valueOf(values.size.toLong()))
        assertThrows(IllegalArgumentException::class.java) { support.unrank(BigInteger.valueOf(-1)) }
        assertThrows(IllegalArgumentException::class.java) { support.unrank(support.count) }
    }

    companion object {
        @JvmStatic fun supports(): List<JsonNode> = GenerationVectors.read("generation-patterns.json", "support")
        @JvmStatic fun invalids(): List<JsonNode> = GenerationVectors.read("generation-patterns.json", "invalid")
        @JvmStatic fun memberships(): List<JsonNode> = GenerationVectors.read("generation-patterns.json", "membership")
        @JvmStatic fun smallSupports(): List<Arguments> = listOf(
            Arguments.of("[AB]{1,3}[AB]{1,3}", Checksum.NONE), Arguments.of("[AAABBB]", Checksum.NONE),
            Arguments.of("#{2}", Checksum.MOD11), Arguments.of("[A0-2]{1,2}[A0-2]{1,2}", Checksum.MOD11),
            Arguments.of("{yyyy}{yy}{MM}{dd}[AB]{1,2}", Checksum.LUHN),
            Arguments.of("[12]{1,2}{dd}[12]{1,2}", Checksum.MOD11), Arguments.of("[AB]{1,2}", Checksum.MOD97),
        )
    }
}
