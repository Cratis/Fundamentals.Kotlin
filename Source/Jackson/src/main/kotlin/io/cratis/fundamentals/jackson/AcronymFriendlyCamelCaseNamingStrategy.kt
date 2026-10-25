// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.strings.toCamelCase
import tools.jackson.databind.PropertyNamingStrategies

/**
 * Jackson property naming strategy that camel-cases property names while preserving acronyms,
 * following .NET's rule (`IsCIA` becomes `isCIA`, `URLValue` stays `URLValue`).
 *
 * Mirrors `AcronymFriendlyJsonCamelCaseNamingPolicy`
 * (`Fundamentals/Source/DotNET/Fundamentals/Serialization/AcronymFriendlyJsonCamelCaseNamingPolicy.cs`).
 * It is not a Jackson module and is not ServiceLoader-registered; set it explicitly on the mapper builder.
 */
public class AcronymFriendlyCamelCaseNamingStrategy : PropertyNamingStrategies.NamingBase() {
    override fun translate(propertyName: String?): String? = propertyName?.toCamelCase()
}
