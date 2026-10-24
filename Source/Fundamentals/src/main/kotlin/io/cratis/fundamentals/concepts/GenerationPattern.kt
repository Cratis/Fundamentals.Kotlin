// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

internal class GenerationPattern(val text: String, val tokens: List<GenerationPatternToken>, val checksum: Checksum) {
    val checksumWidth: Int get() = GenerationChecksum.width(checksum)
    val minimumLength: Int get() = tokens.sumOf { it.width * it.minimum } + checksumWidth
    val maximumLength: Int get() = tokens.sumOf { it.width * it.maximum } + checksumWidth
    val hasDates: Boolean get() = tokens.any { it.segment != null }

    companion object {
        const val LENGTH_LIMIT = 256

        fun parse(pattern: String, checksum: Checksum = Checksum.NONE): GenerationPattern {
            val result = GenerationPattern(pattern, GenerationPatternParser(pattern).parse(), checksum)
            if (result.maximumLength > LENGTH_LIMIT) {
                throw GenerationErrors.invalidPattern(pattern, "final expanded length exceeds 256 characters including check digits")
            }
            return result
        }
    }
}
