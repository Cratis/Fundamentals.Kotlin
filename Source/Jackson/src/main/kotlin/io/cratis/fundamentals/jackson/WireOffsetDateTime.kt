// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField.NANO_OF_SECOND
import java.util.Locale

internal val WIRE_OFFSET_DATE_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
    .appendFraction(NANO_OF_SECOND, 0, 9, true)
    .appendOffset("+HH:MM", "+00:00")
    .toFormatter(Locale.ROOT)
