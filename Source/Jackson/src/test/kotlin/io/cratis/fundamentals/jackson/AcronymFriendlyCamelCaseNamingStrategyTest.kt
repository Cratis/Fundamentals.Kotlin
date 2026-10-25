// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.strings.toCamelCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.json.JsonMapper

class AcronymFriendlyCamelCaseNamingStrategyTest {
    class Payload {
        var IsCIA: Boolean = true
        var FirstName: String = "x"
    }

    // Rows are taken from the pinned casing fixture (Source/Fundamentals/src/test/resources/strings/case.json),
    // which is not on this module's test classpath.
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource("URLValue,URLValue", "IsCIA,isCIA", "Person,person", "IPAddress,IPAddress", "A1Value,a1Value", "iPhone,iPhone", "_Name,_Name")
    fun `nameForField matches toCamelCase`(input: String, expected: String) {
        val strategy = AcronymFriendlyCamelCaseNamingStrategy()
        assertEquals(expected, input.toCamelCase())
        assertEquals(expected, strategy.nameForField(null, null, input))
    }

    @Test
    fun `nameForField null returns null`() {
        assertNull(AcronymFriendlyCamelCaseNamingStrategy().nameForField(null, null, null))
    }

    @Test
    fun `mapper writes acronym friendly property names`() {
        val mapper = JsonMapper.builder().propertyNamingStrategy(AcronymFriendlyCamelCaseNamingStrategy()).build()
        val json = mapper.writeValueAsString(Payload())
        assertEquals(mapper.readTree("{\"isCIA\":true,\"firstName\":\"x\"}"), mapper.readTree(json))
    }
}
