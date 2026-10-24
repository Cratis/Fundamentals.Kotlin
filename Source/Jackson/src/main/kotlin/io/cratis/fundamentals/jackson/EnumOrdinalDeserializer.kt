// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

internal class EnumOrdinalDeserializer(private val enumType: Class<*>) : ValueDeserializer<Enum<*>>() {
    private val constants = enumType.enumConstants.map { it as Enum<*> }
    private val nameLookups = listOf(
        annotatedNames(), constants.associateBy { it.name }, constants.associateBy { it.toString() }
    )

    private fun annotatedNames(): Map<String, Enum<*>> = buildMap {
        constants.forEach { constant ->
            val field = enumType.getField(constant.name)
            field.getAnnotation(JsonProperty::class.java)?.value?.takeIf { it.isNotEmpty() }?.let { put(it, constant) }
            field.getAnnotation(JsonAlias::class.java)?.value?.forEach { put(it, constant) }
        }
    }

    private fun readName(value: String): Enum<*>? =
        nameLookups.firstNotNullOfOrNull { it[value] }
            ?: nameLookups.firstNotNullOfOrNull { lookup ->
                lookup.entries.firstOrNull { it.key.equals(value, ignoreCase = true) }?.value
            }

    private fun readConstant(token: JsonToken, value: String): Enum<*>? {
        val normalized = value.trim()
        val ordinal = normalized.toIntOrNull()
        if (ordinal != null) return constants.getOrNull(ordinal)
        if (token != JsonToken.VALUE_STRING) return null
        return readName(value) ?: readName(normalized)
    }

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Enum<*> {
        val token = parser.currentToken()
        when (token) {
            JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT, JsonToken.VALUE_STRING -> Unit
            else -> throw DatabindException.from(parser, "Expected an ordinal or name for enum '${enumType.name}'")
        }
        val raw = parser.string
        return readConstant(token, raw)
            ?: throw DatabindException.from(parser, "Unable to convert '$raw' to enum '${enumType.name}': value is not defined")
    }
}
