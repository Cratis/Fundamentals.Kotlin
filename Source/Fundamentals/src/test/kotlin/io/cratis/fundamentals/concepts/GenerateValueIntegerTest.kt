// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments

class GenerateValueIntegerTest {
    @Test
    fun `integer rejects inverted bounds and returns equal bounds`() {
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.integer(2, 1) }
        assertEquals(5L, GenerateValue.integer(5, 5))
    }

    @Test
    fun `integer reaches both values in a two value range`() {
        assertEquals(setOf(0L, 1L), List(200) { GenerateValue.integer(0, 1) }.toSet())
    }

    @ParameterizedTest
    @MethodSource("ranges")
    fun `integer preserves exact integral bounds without overflow`(minimum: Long, maximum: Long) {
        repeat(50) { assertTrue(GenerateValue.integer(minimum, maximum) in minimum..maximum) }
    }

    @Test
    fun `integer supports the full signed range`() {
        val values = List(200) { GenerateValue.integer(Long.MIN_VALUE, Long.MAX_VALUE) }
        assertTrue(values.any { it < 0 })
        assertTrue(values.any { it > 0 })
    }

    companion object {
        @JvmStatic
        fun ranges(): Stream<Arguments> = Stream.of(
            Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE), Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE),
            Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE + 2), Arguments.of(Long.MAX_VALUE - 2, Long.MAX_VALUE),
            Arguments.of(Long.MIN_VALUE, 0L), Arguments.of(-1L, Long.MAX_VALUE), Arguments.of(-7L, 3L),
            Arguments.of(9007199254740992L, 9007199254740993L),
        )
    }
}
