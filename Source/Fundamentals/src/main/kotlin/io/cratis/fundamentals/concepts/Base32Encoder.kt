// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

internal object Base32Encoder {
    const val CROCKFORD_UPPER = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    const val CROCKFORD_LOWER = "0123456789abcdefghjkmnpqrstvwxyz"
    const val RFC4648 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(bytes: ByteArray, alphabet: String, leadingPadding: Boolean = false): String {
        val bitCount = bytes.size * 8
        val padding = if (leadingPadding) (5 - (bitCount % 5)) % 5 else 0
        return CharArray((bitCount + 4) / 5) { index ->
            var value = 0
            for (bit in 0 until 5) {
                val sourceBit = index * 5 + bit - padding
                value = value shl 1
                if (sourceBit >= 0 && sourceBit < bitCount) {
                    value = value or ((bytes[sourceBit / 8].toInt() ushr (7 - (sourceBit % 8))) and 1)
                }
            }
            alphabet[value]
        }.concatToString()
    }
}
