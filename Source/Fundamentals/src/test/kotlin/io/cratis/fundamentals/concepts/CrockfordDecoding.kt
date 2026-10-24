// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigInteger

internal object CrockfordDecoding {
    const val UPPER_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    const val LOWER_ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"

    fun decode(value: String): ByteArray {
        val number = value.uppercase().fold(BigInteger.ZERO) { current, character ->
            current * BigInteger.valueOf(32) + BigInteger.valueOf(UPPER_ALPHABET.indexOf(character).toLong())
        }
        val decoded = number.toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        return ByteArray(16).also { decoded.copyInto(it, it.size - decoded.size) }
    }

    fun timestamp(bytes: ByteArray): Long = bytes.take(6).fold(0L) { current, next ->
        (current shl 8) or (next.toLong() and 255)
    }
}
