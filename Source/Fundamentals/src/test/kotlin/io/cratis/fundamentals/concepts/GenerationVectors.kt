// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

internal object GenerationVectors {
    fun read(file: String, section: String): List<JsonNode> {
        val stream = checkNotNull(javaClass.getResourceAsStream("/concepts/generation/$file"))
        val node = stream.use { JsonMapper.builder().build().readTree(it).get(section) }
        return (0 until node.size()).map { node.get(it) }
    }

    fun text(node: JsonNode, name: String): String = node.get(name).asString()

    fun algorithm(node: JsonNode, property: String = "checksum"): Checksum = when (node.get(property)?.asString()) {
        null -> Checksum.NONE
        "luhn" -> Checksum.LUHN
        "mod11" -> Checksum.MOD11
        "mod97" -> Checksum.MOD97
        else -> error("Unknown fixture algorithm")
    }

    fun date(node: JsonNode): LocalDate? = node.get("date")?.asString()?.let(LocalDate::parse)
    fun instant(node: JsonNode): Instant? = node.get("instant")?.asString()?.let { OffsetDateTime.parse(it).toInstant() }
}
