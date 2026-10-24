// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import java.time.Duration
import java.time.format.DateTimeParseException

internal object TimeSpanWireFormat {
    private val canonical = Regex("^(-)?(?:(\\d+)\\.)?(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{1,7}))?$")

    fun format(value: Duration): String {
        val absolute = value.abs()
        val ticks = absolute.nano / 100
        val negative = value.isNegative && (absolute.seconds != 0L || ticks != 0)
        val days = absolute.seconds / 86400
        val seconds = absolute.seconds % 86400
        return buildString {
            if (negative) append('-')
            if (days != 0L) append(days).append('.')
            append((seconds / 3600).toString().padStart(2, '0')).append(':')
            append((seconds / 60 % 60).toString().padStart(2, '0')).append(':')
            append((seconds % 60).toString().padStart(2, '0'))
            if (ticks != 0) append('.').append(ticks.toString().padStart(7, '0'))
        }
    }

    fun parse(value: String): Duration = try {
        parseDuration(value)
    } catch (exception: Exception) {
        throw DateTimeParseException("Invalid TimeSpan: ${exception.message}", value, 0, exception)
    }

    private fun parseDuration(value: String): Duration {
        val match = canonical.matchEntire(value)
        if (match == null) {
            require(value.startsWith("P") || value.startsWith("-P")) { "Expected a TimeSpan in 'c' format" }
            return Duration.parse(value)
        }
        val groups = match.groupValues
        val sign = groups[1]
        val dayText = groups[2]
        val fraction = groups[6]
        val hours = groups[3].toLong()
        val minutes = groups[4].toLong()
        val seconds = groups[5].toLong()
        require(hours < 24 && minutes < 60 && seconds < 60) { "TimeSpan components out of range" }
        val days = if (dayText.isEmpty()) 0L else dayText.toLong()
        val total = Math.addExact(Math.multiplyExact(days, 86400L), hours * 3600 + minutes * 60 + seconds)
        val nanos = if (fraction.isEmpty()) 0L else fraction.padEnd(7, '0').toLong() * 100
        val duration = Duration.ofSeconds(total, nanos)
        return if (sign.isEmpty()) duration else duration.negated()
    }
}
