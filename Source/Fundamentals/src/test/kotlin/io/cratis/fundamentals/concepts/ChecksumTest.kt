// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.databind.JsonNode

class ChecksumTest {
    @Test
    fun `all checksum vector rows are consumed`() {
        assertEquals(18, calculations().size)
        assertEquals(24, validations().size)
    }

    @ParameterizedTest
    @MethodSource("calculations")
    fun `calculates Screenplay checksum suffixes including excluded mod11 payloads`(row: JsonNode) {
        val suffix = row.get("suffix").let { if (it.isNull) null else it.asString() }
        assertEquals(suffix, GenerationChecksum.calculate(GenerationVectors.text(row, "payload"), GenerationVectors.algorithm(row, "algorithm")))
    }

    @ParameterizedTest
    @MethodSource("validations")
    fun `validates exact trailing digits through both public entry points`(row: JsonNode) {
        val value = GenerationVectors.text(row, "value")
        val checksum = GenerationVectors.algorithm(row, "algorithm")
        val valid = row.get("valid").asBoolean()
        assertEquals(valid, Checksum.hasValidChecksum(value, checksum))
        assertEquals(valid, GenerateValue.hasValidChecksum(value, checksum))
    }

    @Test
    fun `none is not a supported validation algorithm`() {
        val exception = assertThrows(IllegalArgumentException::class.java) { Checksum.hasValidChecksum("0", Checksum.NONE) }
        assertEquals("Checksum 'NONE' is not a supported checksum algorithm for this operation.", exception.message)
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.hasValidChecksum("0", Checksum.NONE) }
    }

    companion object {
        @JvmStatic fun calculations(): List<JsonNode> = GenerationVectors.read("generation-checksums.json", "checksums")
        @JvmStatic fun validations(): List<JsonNode> = GenerationVectors.read("generation-checksums.json", "validation")
    }
}
