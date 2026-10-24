// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.strings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.databind.json.JsonMapper

class StringExtensionsTest {
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("cases")
    fun `casing matches the pinned dotnet fixtures`(input: String, camel: String, pascal: String) {
        assertEquals(camel, input.toCamelCase())
        assertEquals(pascal, input.toPascalCase())
    }

    @Test
    fun `empty strings and dotted capital i are unchanged`() {
        assertEquals("", "".toCamelCase())
        assertEquals("", "".toPascalCase())
        assertEquals("İstanbul", "İstanbul".toCamelCase())
    }

    companion object {
        @JvmStatic
        fun cases(): List<Arguments> = checkNotNull(
            StringExtensionsTest::class.java.getResourceAsStream("/strings/case.json")
        ).use { stream ->
            val rows = JsonMapper.builder().build().readTree(stream).get("cases")
            (0 until rows.size()).map { index ->
                val row = rows.get(index)
                Arguments.of(row.get("input").asString(), row.get("camel").asString(), row.get("pascal").asString())
            }
        }
    }
}
