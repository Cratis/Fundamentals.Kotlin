// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.util.Base64
import java.util.HexFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class GenerateValueRandomStringsTest {
    @Test
    fun `nano id defaults to twenty one URL safe characters`() {
        val value = GenerateValue.nanoId()
        assertEquals(21, value.length)
        assertTrue(value.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-" })
        assertEquals(32, GenerateValue.nanoId(32).length)
        assertTrue(GenerateValue.nanoId(100, "abc").all { it in "abc" })
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 257])
    fun `nano id rejects invalid sizes`(size: Int) {
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.nanoId(size) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "aba", "aé", "a\n", "a\u007f"])
    fun `nano id rejects non distinct or non printable alphabets`(alphabet: String) {
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.nanoId(21, alphabet) }
    }

    @Test
    fun `nano id accepts printable alphabet and size boundaries`() {
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.nanoId(21, "a".repeat(257)) }
        listOf(1, 256).forEach { size ->
            val value = GenerateValue.nanoId(size, " ~")
            assertEquals(size, value.length)
            assertTrue(value.all { it in " ~" })
        }
        val printable = (' '..'~').joinToString("")
        assertTrue(GenerateValue.nanoId(256, printable).all { it in printable })
    }

    @ParameterizedTest
    @EnumSource(ValueEncoding::class)
    fun `bytes encode every supported count without padding and round trip`(encoding: ValueEncoding) {
        for (count in 1..512) {
            val value = GenerateValue.bytes(count, encoding)
            val decoded = when (encoding) {
                ValueEncoding.BASE64_URL -> {
                    assertTrue(value.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-" })
                    assertEquals((count * 8 + 5) / 6, value.length)
                    Base64.getUrlDecoder().decode(value)
                }
                ValueEncoding.HEX -> {
                    assertTrue(value.all { it in "0123456789abcdef" })
                    assertEquals(count * 2, value.length)
                    HexFormat.of().parseHex(value)
                }
                ValueEncoding.BASE32 -> {
                    assertEquals((count * 8 + 4) / 5, value.length)
                    assertTrue(value.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" })
                    decodeBase32(value).also {
                        assertEquals(value, Base32Encoder.encode(it, Base32Encoder.RFC4648))
                    }
                }
            }
            assertEquals(count, decoded.size)
        }
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.bytes(0, encoding) }
        assertThrows(IllegalArgumentException::class.java) { GenerateValue.bytes(513, encoding) }
    }

    @Test
    fun `bytes default to unpadded base64 url`() {
        assertEquals(16, Base64.getUrlDecoder().decode(GenerateValue.bytes(16)).size)
    }

    private fun decodeBase32(value: String): ByteArray {
        val bits = value.flatMap { character ->
            val number = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(character)
            (4 downTo 0).map { (number ushr it) and 1 }
        }
        return ByteArray(bits.size / 8) { index ->
            bits.subList(index * 8, index * 8 + 8).fold(0) { current, bit -> (current shl 1) or bit }.toByte()
        }
    }
}
