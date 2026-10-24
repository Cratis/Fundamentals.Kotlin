// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

internal class GenerationPatternParser(private val pattern: String) {
    private var offset = 0
    private var hasRandom = false

    fun parse(): List<GenerationPatternToken> {
        if (pattern.isEmpty()) fail("empty pattern")
        if (pattern.any { it !in ' '..'~' }) fail("patterns require printable ASCII")
        val tokens = mutableListOf<GenerationPatternToken>()
        var maximumLength = 0
        while (offset < pattern.length) {
            var token = token()
            if (hasRepeat()) token = repeat(token)
            maximumLength += token.width * token.maximum
            if (maximumLength > GenerationPattern.LENGTH_LIMIT) {
                fail("final expanded length exceeds 256 characters including check digits")
            }
            tokens.add(token)
        }
        if (!hasRandom) fail("pattern requires a random token (#, ?, *, or a class)")
        return tokens
    }

    private fun hasRepeat(): Boolean {
        if (offset + 1 >= pattern.length) return false
        return pattern[offset] == '{' && pattern[offset + 1] in '0'..'9'
    }

    private fun token(): GenerationPatternToken {
        val character = pattern[offset++]
        return when (character) {
            '#' -> random("0123456789")
            '?' -> random("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            '*' -> random("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            '[' -> random(characterClass())
            '\\' -> GenerationPatternToken(escape("#?*[]{}\\").toString())
            '{' -> segment()
            ']', '}' -> fail("unescaped closing delimiter")
            else -> GenerationPatternToken(character.toString())
        }
    }

    private fun random(characters: String): GenerationPatternToken {
        hasRandom = true
        return GenerationPatternToken(characters)
    }

    private fun segment(): GenerationPatternToken {
        val end = pattern.indexOf('}', offset)
        if (end < 0) fail("unclosed date segment")
        val segment = pattern.substring(offset, end)
        if (segment.isNotEmpty() && segment[0] in '0'..'9') fail("repeat has no preceding token")
        if (segment !in listOf("yyyy", "yy", "MM", "dd")) fail("unknown date segment; use yyyy, yy, MM or dd")
        offset = end + 1
        return GenerationPatternToken("", segment = segment)
    }

    private fun repeat(token: GenerationPatternToken): GenerationPatternToken {
        offset++
        val end = pattern.indexOf('}', offset)
        if (end < 0) fail("unclosed repeat")
        val parts = pattern.substring(offset, end).split(',')
        validateRepeatParts(parts)
        val minimum = parts.first().toIntOrNull() ?: fail("repeat bound exceeds 64")
        val maximum = parts.last().toIntOrNull() ?: fail("repeat bound exceeds 64")
        if (minimum < 1) fail("repeat minimum must be at least 1")
        if (minimum > maximum) fail("repeat minimum exceeds maximum")
        if (maximum > 64) fail("repeat maximum exceeds 64")
        offset = end + 1
        return token.copy(minimum = minimum, maximum = maximum)
    }

    private fun validateRepeatParts(parts: List<String>) {
        if (parts.size !in 1..2) fail("malformed repeat; use {n} or {m,n}")
        if (parts.any { it.isEmpty() }) fail("malformed repeat; use {n} or {m,n}")
        if (parts.any { part -> part.any { it !in '0'..'9' } }) fail("malformed repeat; use {n} or {m,n}")
    }

    private fun escape(allowed: String): Char {
        if (offset == pattern.length) fail("dangling escape")
        val character = pattern[offset++]
        if (character !in allowed) fail("illegal escape")
        return character
    }

    private fun characterClass(): String {
        if (offset < pattern.length && pattern[offset] == '^') fail("negated classes are not supported")
        val characters = sortedSetOf<Char>()
        while (!atClassEnd()) {
            val first = classCharacter()
            if (hasClassRange()) {
                characters.addAll(classRange(first))
            } else {
                characters.add(first)
            }
        }
        if (offset == pattern.length) fail("unclosed class")
        if (characters.isEmpty()) fail("empty class")
        offset++
        return characters.joinToString("")
    }

    private fun atClassEnd(): Boolean = offset == pattern.length || pattern[offset] == ']'

    private fun hasClassRange(): Boolean = offset < pattern.length && pattern[offset] == '-'

    private fun classRange(first: Char): CharRange {
        offset++
        if (offset == pattern.length || pattern[offset] == ']') {
            fail("class range needs an endpoint; escape a literal hyphen")
        }
        val last = classCharacter()
        if (last < first) fail("descending class range")
        return first..last
    }

    private fun classCharacter(): Char {
        val character = pattern[offset++]
        if (character == '\\') return escape("]-\\")
        if (character == '-' || character == '[') fail("escape a literal class delimiter or hyphen")
        return character
    }

    private fun fail(reason: String): Nothing = throw GenerationErrors.invalidPattern(pattern, reason)
}
