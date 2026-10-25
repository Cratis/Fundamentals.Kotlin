// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

/**
 * Default [NamingPolicy] that pluralises the simple class name using English-language inflection.
 *
 * The cases pinned by `DefaultNamingPolicyTest` record what Evo Inflector actually produces (with
 * two Humanizer-aligned corrections), so a library upgrade that changes a plural fails the build
 * instead of silently moving a collection.
 *
 * Mirrors `Cratis.Serialization.DefaultNamingPolicy`
 * (`Fundamentals/Source/DotNET/Fundamentals/Serialization/DefaultNamingPolicy.cs`), which is
 * `DefaultNamingPolicy(bool pluralizeReadModelNames = true)` and calls Humanizer's `Pluralize()`.
 *
 * Supply a custom [NamingPolicy] implementation to override the default policy.
 *
 * @param pluralizeReadModelNames Whether read-model names are pluralised; when false the simple class name is returned.
 */
public class DefaultNamingPolicy @JvmOverloads public constructor(
    private val pluralizeReadModelNames: Boolean = true
) : NamingPolicy {
    override fun getReadModelName(readModelType: Class<*>): String {
        val name = readModelType.simpleName
        if (!pluralizeReadModelNames) return name
        return EnglishPlurals.plural(name)
    }

    override fun getPropertyName(name: String): String = name
}
