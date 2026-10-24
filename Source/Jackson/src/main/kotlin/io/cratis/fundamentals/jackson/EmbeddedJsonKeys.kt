// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.io.StringWriter
import tools.jackson.core.JsonGenerator
import tools.jackson.core.ObjectWriteContext
import tools.jackson.core.PrettyPrinter
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.KeyDeserializer
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

internal object EmbeddedConceptKeySerializer : ValueSerializer<ConceptAs<*>>() {
    override fun serialize(value: ConceptAs<*>, generator: JsonGenerator, context: SerializationContext) {
        val writer = StringWriter()
        // Preserve mapper features and escaping, disabling only indentation for the embedded JSON.
        val compact = object : ObjectWriteContext by context {
            override fun getPrettyPrinter(): PrettyPrinter? = null
            override fun hasPrettyPrinter(): Boolean = false
        }
        context.tokenStreamFactory().createGenerator(compact, writer).use { nested ->
            context.writeValue(nested, value)
        }
        generator.writeName(writer.toString())
    }
}

internal class EmbeddedConceptKeyDeserializer(private val conceptType: JavaType) : KeyDeserializer() {
    private val legacy = ConceptKeyDeserializer(conceptType)

    override fun deserializeKey(key: String, context: DeserializationContext): Any =
        embedded(key, context) ?: legacy.deserializeKey(key, context)

    private fun embedded(key: String, context: DeserializationContext): Any? = try {
        context.createParser(key).use { parser ->
            parser.nextToken()
            val value = context.readValue<Any?>(parser, conceptType)
            if (parser.nextToken() == null) value else null
        }
    } catch (_: Exception) {
        null
    }
}
