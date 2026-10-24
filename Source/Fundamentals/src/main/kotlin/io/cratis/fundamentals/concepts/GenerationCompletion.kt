// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigInteger

/** Counts and branches are populated before publication; sampling never mutates them. */
internal class GenerationCompletion(val accepts: Boolean) {
    var count: BigInteger = if (accepts) BigInteger.ONE else BigInteger.ZERO
    val branches: MutableList<Pair<Char, GenerationCompletion>> = mutableListOf()
}
