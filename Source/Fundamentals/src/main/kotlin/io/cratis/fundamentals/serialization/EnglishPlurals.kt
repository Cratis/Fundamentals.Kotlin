// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import org.atteo.evo.inflector.English

/**
 * English pluralisation shared by the naming policies.
 *
 * Pluralisation is performed by [Evo Inflector](https://github.com/atteo/evo-inflector)
 * (`org.atteo:evo-inflector`). The Chronicle Kernel is .NET and pluralises with Humanizer, so this
 * is a *different implementation chosen for compatibility*, not the same library. The collection
 * name a query reads from must match the one the kernel's projection writes to; where the two
 * algorithms disagree on a noun, the kernel is authoritative and the application must supply its
 * own [NamingPolicy] for that type.
 */
internal object EnglishPlurals {
    /**
     * Words where Evo Inflector and Humanizer (C# `DefaultNamingPolicy` calls `Pluralize()`, which
     * the Chronicle kernel also uses) disagree, corrected toward Humanizer.
     *
     * Measured, not assumed: both libraries were run over the same word list. Humanizer produced
     * `People` and `Indices` where Evo Inflector produced `Persons` and `Indexes`. The kernel
     * writes the projection, so its spelling decides which collection a query must read, and an
     * uncorrected divergence would silently read from a collection nothing ever writes to.
     *
     * The remaining words checked agreed, including the irregular `Child` -> `Children`,
     * `Mouse` -> `Mice`, `Datum` -> `Data` and the invariant `Series`.
     *
     * This list is empirical and therefore incomplete. A read model whose plural differs from the
     * kernel's must override the policy for that type rather than relying on this map.
     */
    private val HUMANIZER_ALIGNED_PLURALS = mapOf(
        "person" to "People",
        "index" to "Indices"
    )

    /**
     * Returns the plural of [word].
     *
     * @param word The singular word.
     * @return The plural form.
     */
    fun plural(word: String): String = HUMANIZER_ALIGNED_PLURALS[word.lowercase()] ?: English.plural(word)
}
