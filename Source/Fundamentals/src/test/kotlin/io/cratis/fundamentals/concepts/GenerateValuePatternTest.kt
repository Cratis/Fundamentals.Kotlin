// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.IntStream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Undefined numeric checksum enum values from C# are unrepresentable on the JVM. */
class GenerateValuePatternTest {
    @Test
    fun `generates readable order format and reaches both values of small support`() {
        repeat(20) {
            assertTrue(GenerateValue.pattern("ORD-[A-HJ-NP-Z2-9]{4}-#{4,6}").matches(Regex("ORD-[A-HJ-NP-Z2-9]{4}-[0-9]{4,6}")))
        }
        assertEquals(setOf("A", "B"), List(200) { GenerateValue.pattern("[AB]") }.toSet())
    }

    @Test
    fun `rejects empty conditioned support and dates without a context`() {
        val empty = assertThrows(IllegalArgumentException::class.java) { GenerateValue.pattern("000000006-[AB]", Checksum.MOD11) }
        assertEquals("Value pattern '000000006-[AB]' has no values with an emittable check digit.", empty.message)
        val dates = assertThrows(IllegalArgumentException::class.java) { GenerateValue.pattern("INV-{yyyy}-#{7}") }
        assertEquals("Value pattern 'INV-{yyyy}-#{7}' requires a generation context for its UTC date segments.", dates.message)
    }

    @Test
    fun `context requires captured time even without date segments`() {
        val context = GenerationContext(Instant.parse("0001-01-01T00:00:00Z"))
        listOf("#", "{yyyy}#").forEach {
            val exception = assertThrows(IllegalArgumentException::class.java) { GenerateValue.pattern(it, context) }
            assertEquals("A generation context must contain the time the operation was received.", exception.message)
        }
    }

    @Test
    fun `context supports years before epoch with four digit years`() {
        val context = GenerationContext(Instant.parse("0001-02-03T00:00:00Z"))
        assertEquals("0001010203A", GenerateValue.pattern("{yyyy}{yy}{MM}{dd}[A]", context))
    }

    @Test
    fun `context substitutes the UTC date and includes valid invoice checksum`() {
        val context = GenerationContext(OffsetDateTime.parse("2024-03-01T00:30:00+01:00").toInstant())
        val invoice = GenerateValue.pattern("INV-{yyyy}-#{7}", context, Checksum.LUHN)
        assertEquals(17, invoice.length)
        assertTrue(invoice.startsWith("INV-2024-"))
        assertTrue(invoice.substring(9).all { it in '0'..'9' })
        assertTrue(Checksum.hasValidChecksum(invoice, Checksum.LUHN))
        val date = GenerateValue.pattern("{yyyy}{yy}{MM}{dd}-{MM}{dd}-#", context)
        assertTrue(date.startsWith("2024240229-0229-"))
    }

    @Test
    fun `failed cache construction is removed and retried`() {
        val cache = GenerationCache<String, String>()
        var attempts = 0
        assertThrows(IllegalArgumentException::class.java) {
            cache.get("key") { attempts++; throw IllegalArgumentException("failed") }
        }
        assertEquals("value", cache.get("key") { attempts++; "value" })
        assertEquals("value", cache.get("key") { error("must be cached") })
        assertEquals(2, attempts)
    }

    @Test
    fun `capacity clears the cache and returns correct values`() {
        val cache = GenerationCache<Int, Int>()
        repeat(128) { assertEquals(it * 2, cache.get(it) { key -> key * 2 }) }
        assertEquals(0, cache.get(0) { error("must still be cached") })
        assertEquals(256, cache.get(128) { it * 2 })
        var rebuilt = false
        assertEquals(0, cache.get(0) { rebuilt = true; it * 2 })
        assertTrue(rebuilt)
    }

    @Test
    fun `concurrent cache churn preserves independent patterns and check digits`() {
        val failures = ConcurrentLinkedQueue<String>()
        IntStream.range(0, 400).parallel().forEach { index ->
            try {
                val value = GenerateValue.pattern("$index-[AB]#", Checksum.MOD11)
                if (!value.startsWith("$index-") || !GenerateValue.hasValidChecksum(value, Checksum.MOD11)) failures.add(value)
            } catch (exception: Exception) {
                failures.add(exception.toString())
            }
        }
        assertTrue(failures.isEmpty(), failures.toString())
    }
}
