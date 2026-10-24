// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.LocalTime
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

internal object ConceptKeySerializer : ValueSerializer<ConceptAs<*>>() {
    override fun serialize(value: ConceptAs<*>, generator: JsonGenerator, serializers: SerializationContext) {
        val text = when (val raw = value.value()) {
            is String -> raw
            is BigDecimal -> raw.toPlainString()
            is OffsetDateTime -> WIRE_OFFSET_DATE_TIME.format(raw)
            is LocalTime -> TemporalWireFormat.formatTime(raw)
            else -> raw.toString()
        }
        generator.writeName(text)
    }
}
