// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

internal object GenerationChecksum {
    fun width(checksum: Checksum): Int = when (checksum) {
        Checksum.NONE -> 0
        Checksum.LUHN, Checksum.MOD11 -> 1
        Checksum.MOD97 -> 2
    }

    fun calculate(payload: String, checksum: Checksum): String? {
        val digits = payload.filter { it in '0'..'9' }.map { it - '0' }
        val value = when (checksum) {
            Checksum.LUHN -> luhn(digits)
            Checksum.MOD11 -> mod11(digits)
            Checksum.MOD97 -> mod97(digits)
            Checksum.NONE -> 0
        }
        return when {
            checksum == Checksum.NONE -> ""
            checksum == Checksum.MOD11 && value == 10 -> null
            else -> value.toString().padStart(if (checksum == Checksum.MOD97) 2 else 1, '0')
        }
    }

    fun matches(value: String, checksum: Checksum): Boolean {
        val width = width(checksum)
        if (width == 0) throw GenerationErrors.unknownChecksum(checksum)
        if (value.length < width || value.any { it !in ' '..'~' }) return false
        val suffix = value.takeLast(width)
        if (suffix.any { it !in '0'..'9' }) return false
        return suffix == calculate(value.dropLast(width), checksum)
    }

    private fun luhn(digits: List<Int>): Int {
        var sum = 0
        for (index in digits.indices) {
            val digit = digits[digits.size - index - 1] * if (index % 2 == 0) 2 else 1
            sum = (sum + if (digit > 9) digit - 9 else digit) % 10
        }
        return (10 - sum % 10) % 10
    }

    private fun mod11(digits: List<Int>): Int {
        var sum = 0
        for (index in digits.indices) sum = (sum + digits[digits.size - index - 1] * (2 + index % 6)) % 11
        return (11 - sum % 11) % 11
    }

    private fun mod97(digits: List<Int>): Int {
        var residue = 0
        for (digit in digits) residue = (residue * 10 + digit) % 97
        return 98 - (residue * 100 % 97)
    }
}
