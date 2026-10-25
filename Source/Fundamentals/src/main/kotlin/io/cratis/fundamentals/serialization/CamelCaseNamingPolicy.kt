// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import io.cratis.fundamentals.strings.toCamelCase

/**
 * [NamingPolicy] that camel-cases both read-model names and member names, after optional
 * English pluralisation of the simple class name.
 *
 * Mirrors `Cratis.Serialization.CamelCaseNamingPolicy`
 * (`Fundamentals/Source/DotNET/Fundamentals/Serialization/CamelCaseNamingPolicy.cs`). Casing uses
 * [toCamelCase], which follows .NET's acronym-preserving rule.
 *
 * @param pluralizeReadModelNames Whether read-model names are pluralised before camel-casing.
 */
public class CamelCaseNamingPolicy @JvmOverloads public constructor(
    private val pluralizeReadModelNames: Boolean = true
) : NamingPolicy {
    override fun getReadModelName(readModelType: Class<*>): String {
        val name = readModelType.simpleName
        val named = if (pluralizeReadModelNames) EnglishPlurals.plural(name) else name
        return named.toCamelCase()
    }

    override fun getPropertyName(name: String): String = name.toCamelCase()
}
