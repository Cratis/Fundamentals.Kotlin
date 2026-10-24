// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.LocalDate

internal data class GenerationPatternToken(
    val characters: String,
    val minimum: Int = 1,
    val maximum: Int = 1,
    val segment: String? = null,
) {
    val width: Int get() = when (segment) { "yyyy" -> 4; null -> 1; else -> 2 }

    fun expand(date: LocalDate?): List<String> {
        if (segment == null) return listOf(characters)
        if (date == null) return List(width) { "0123456789" }
        val text = when (segment) {
            "yyyy" -> date.year.toString().padStart(4, '0')
            "yy" -> (date.year % 100).toString().padStart(2, '0')
            "MM" -> date.monthValue.toString().padStart(2, '0')
            else -> date.dayOfMonth.toString().padStart(2, '0')
        }
        return text.map { it.toString() }
    }
}
