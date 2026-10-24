// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

internal object TemporalWireFormat {
    private val timeFormat = DateTimeFormatterBuilder()
        .appendPattern("HH:mm:ss")
        .appendFraction(ChronoField.NANO_OF_SECOND, 7, 7, true)
        .toFormatter(java.util.Locale.ROOT)

    fun formatTime(value: LocalTime): String = timeFormat.format(value)

    fun parseTime(value: String): LocalTime = LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME)

    fun parseDate(value: String): LocalDate {
        if ('T' !in value) return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
        return LocalDate.from(DateTimeFormatter.ISO_DATE_TIME.parse(value))
    }
}
