// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenerateValueUuidTest {
    @Test
    fun `uuid is version four with RFC variant and distinct values`() {
        val values = List(1000) { GenerateValue.uuid() }
        assertTrue(values.all { it.version() == 4 && it.variant() == 2 })
        assertEquals(1000, values.toSet().size)
    }

    @Test
    fun `uuid v7 uses current milliseconds and random RFC bits`() {
        val before = Instant.now().toEpochMilli()
        val values = List(1000) { GenerateValue.uuidV7() }
        val after = Instant.now().toEpochMilli()
        assertTrue(values.all { it.version() == 7 && it.variant() == 2 })
        assertTrue(values.all { (it.mostSignificantBits ushr 16) in before..after })
        assertEquals(1000, values.toSet().size)
        assertEquals(36, values.first().toString().length)
    }

    @Test
    fun `uuid v7 in context preserves UTC milliseconds including epoch`() {
        val instant = OffsetDateTime.parse("2024-06-15T12:34:56.789+02:00").toInstant()
        val context = GenerationContext(instant, "tenant-a")
        val values = List(1000) { GenerateValue.uuidV7(context) }
        assertTrue(values.all { it.mostSignificantBits ushr 16 == instant.toEpochMilli() })
        assertTrue(values.all { it.version() == 7 && it.variant() == 2 })
        assertEquals(1000, values.toSet().size)
        assertEquals(0L, GenerateValue.uuidV7(GenerationContext(Instant.EPOCH)).mostSignificantBits ushr 16)
    }

    @Test
    fun `uuid v7 rejects missing time before checking the epoch`() {
        val missing = assertThrows(IllegalArgumentException::class.java) {
            GenerateValue.uuidV7(GenerationContext(Instant.parse("0001-01-01T00:00:00Z")))
        }
        assertEquals("A generation context must contain the time the operation was received.", missing.message)
        assertThrows(IllegalArgumentException::class.java) {
            GenerateValue.uuidV7(GenerationContext(Instant.EPOCH.minusNanos(100)))
        }
    }
}
