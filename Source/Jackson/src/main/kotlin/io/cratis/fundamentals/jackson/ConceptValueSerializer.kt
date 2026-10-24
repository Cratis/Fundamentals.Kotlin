// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

internal object ConceptValueSerializer : ValueSerializer<ConceptAs<*>>() {
    override fun serialize(value: ConceptAs<*>, generator: JsonGenerator, serializers: SerializationContext) {
        when (val raw = value.value()) {
            is String -> generator.writeString(raw)
            is UUID -> generator.writeString(raw.toString())
            is Int -> generator.writeNumber(raw)
            is Long -> generator.writeNumber(raw)
            is Short -> generator.writeNumber(raw)
            is Byte -> generator.writeNumber(raw.toShort())
            is Double -> generator.writeNumber(raw)
            is Float -> generator.writeNumber(raw)
            is BigDecimal -> generator.writeNumber(raw.toPlainString())
            is Boolean -> generator.writeBoolean(raw)
            is LocalDate -> generator.writeString(raw.toString())
            is OffsetDateTime -> generator.writeString(WIRE_OFFSET_DATE_TIME.format(raw))
            else -> serializers.writeValue(generator, raw)
        }
    }
}
