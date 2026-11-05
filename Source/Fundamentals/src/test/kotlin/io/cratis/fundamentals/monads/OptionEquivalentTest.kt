// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Evidence for Monads/Option.cs and for_Option specs at 6ae7eb7, without a new Option type. */
class OptionEquivalentTest {
    @Test
    fun `optional value maps HasValue TryGetValue Match and Switch`() {
        val value = Optional.of(42)
        assertTrue(value.isPresent)
        assertEquals(42, value.getOrNull())
        assertEquals(42, value.map { it }.orElseGet { -1 })
        val calls = mutableListOf<Int>()
        value.ifPresentOrElse({ calls.add(it) }, { calls.add(-1) })
        assertEquals(listOf(42), calls)
    }

    @Test
    fun `empty optional maps None and selects only the absent branch`() {
        val none = Optional.empty<Int>()
        assertFalse(none.isPresent)
        assertNull(none.getOrNull())
        assertEquals(-1, none.map { it }.orElseGet { -1 })
        val calls = mutableListOf<Int>()
        none.ifPresentOrElse({ calls.add(it) }, { calls.add(-1) })
        assertEquals(listOf(-1), calls)
    }

    @Test
    fun `Kotlin nullable provides the native equivalent`() {
        val value: Int? = 42
        val none: Int? = null
        assertEquals(42, value?.let { it } ?: -1)
        assertEquals(-1, none?.let { it } ?: -1)
        val calls = mutableListOf<Int>()
        value?.let { calls.add(it) } ?: calls.add(-1)
        none?.let { calls.add(it) } ?: calls.add(-1)
        assertEquals(listOf(42, -1), calls)
    }
}
