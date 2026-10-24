// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("StringExtensions")

package io.cratis.fundamentals.strings

/**
 * Converts a name using .NET's invariant UTF-16 camel rule. Acronyms beginning with two
 * uppercase letters, leading non-uppercase code units and empty strings are unchanged.
 */
public fun String.toCamelCase(): String {
    if (isEmpty()) return this
    if (length >= 2 && isUpper(this[0]) && isUpper(this[1])) return this
    if (!isUpper(this[0])) return this
    val chars = toCharArray()
    for (index in chars.indices) {
        if (index == 1 && !isUpper(chars[index])) break
        val hasNext = index + 1 < chars.size
        if (index > 0 && hasNext && !isUpper(chars[index + 1])) {
            if (chars[index + 1] == ' ') chars[index] = lowerInvariant(chars[index])
            break
        }
        chars[index] = lowerInvariant(chars[index])
    }
    return String(chars)
}

/**
 * Uppercases only the first UTF-16 code unit with simple invariant casing, never a full-string
 * fold. Supplementary characters and unpaired surrogates are preserved. Selected Unicode
 * cases are pinned, not a guarantee for every JDK and .NET casing-table version.
 */
public fun String.toPascalCase(): String {
    if (isEmpty()) return this
    return upperInvariant(this[0]) + substring(1)
}

private fun isUpper(character: Char): Boolean =
    Character.getType(character) == Character.UPPERCASE_LETTER.toInt()

private fun lowerInvariant(character: Char): Char =
    if (character == '\u0130') character else Character.toLowerCase(character)

private fun upperInvariant(character: Char): Char =
    if (character == '\u0131') character else Character.toUpperCase(character)
