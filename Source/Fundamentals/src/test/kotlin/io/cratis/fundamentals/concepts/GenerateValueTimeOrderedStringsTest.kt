// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GenerateValueTimeOrderedStringsTest {
    private val now = OffsetDateTime.parse("2024-06-15T12:34:56.789+02:00").toInstant()
    private val context = GenerationContext(now, "tenant-a")

    @Test
    fun `ulid has uppercase Crockford shape current time and distinct randomness`() {
        val before = Instant.now().toEpochMilli()
        val values = List(1000) { GenerateValue.ulid() }
        val after = Instant.now().toEpochMilli()
        assertTrue(values.all { it.length == 26 && it.first() in '0'..'7' && it.all(CrockfordDecoding.UPPER_ALPHABET::contains) })
        assertTrue(values.all { CrockfordDecoding.timestamp(CrockfordDecoding.decode(it)) in before..after })
        assertEquals(1000, values.toSet().size)
    }

    @Test
    fun `ulid in context captures milliseconds and epoch without monotonic counter`() {
        val values = List(1000) { GenerateValue.ulid(context) }
        assertTrue(values.all { CrockfordDecoding.timestamp(CrockfordDecoding.decode(it)) == now.toEpochMilli() })
        assertEquals(1000, values.toSet().size)
        assertEquals(0L, CrockfordDecoding.timestamp(CrockfordDecoding.decode(GenerateValue.ulid(GenerationContext(Instant.EPOCH)))))
    }

    @Test
    fun `type id has lowercase UUID v7 suffix current time and distinct randomness`() {
        val before = Instant.now().toEpochMilli()
        val values = List(1000) { GenerateValue.typeId("order") }
        val after = Instant.now().toEpochMilli()
        assertEquals(1000, values.toSet().size)
        values.forEach {
            assertTrue(it.startsWith("order_"))
            val suffix = it.removePrefix("order_")
            assertEquals(26, suffix.length)
            assertTrue(suffix.all(CrockfordDecoding.LOWER_ALPHABET::contains))
            val bytes = CrockfordDecoding.decode(suffix)
            assertTrue(CrockfordDecoding.timestamp(bytes) in before..after)
            assertEquals(7, (bytes[6].toInt() and 255) ushr 4)
            assertEquals(2, (bytes[8].toInt() and 255) ushr 6)
        }
        assertEquals(26, GenerateValue.typeId("").length)
    }

    @Test
    fun `type id in context uses captured timestamp and distinct random UUIDs`() {
        val values = List(1000) { GenerateValue.typeId("order", context) }
        assertEquals(1000, values.toSet().size)
        assertTrue(values.all { CrockfordDecoding.timestamp(CrockfordDecoding.decode(it.substringAfter('_'))) == now.toEpochMilli() })
        assertTrue(GenerateValue.typeId("order", GenerationContext(Instant.EPOCH)).startsWith("order_00000000"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["User", "_user", "user_", "us-er", "user1", "user\n"])
    fun `type id rejects invalid prefixes`(prefix: String) {
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.typeId(prefix) }
    }

    @Test
    fun `type id validates prefix length and permits valid boundaries`() {
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.typeId("a".repeat(64)) }
        listOf("a", "a_b", "a" + "_".repeat(61) + "z").forEach {
            assertTrue(GenerateValue.typeId(it).startsWith(it + "_"))
        }
    }

    @Test
    fun `time ordered strings reject missing and pre epoch times`() {
        listOf(Instant.parse("0001-01-01T00:00:00Z"), Instant.EPOCH.minusNanos(100)).forEach {
            assertThrows(IllegalArgumentException::class.java) { GenerateValue.ulid(GenerationContext(it)) }
            assertThrows(IllegalArgumentException::class.java) { GenerateValue.typeId("order", GenerationContext(it)) }
        }
    }
}
