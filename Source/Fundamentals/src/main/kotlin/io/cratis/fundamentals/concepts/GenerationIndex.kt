// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigInteger

internal object GenerationIndex {
    fun draw(count: BigInteger): BigInteger {
        if (count == BigInteger.ONE) return BigInteger.ZERO
        val bits = (count - BigInteger.ONE).bitLength()
        var index: BigInteger
        do {
            index = BigInteger(bits, GenerationRandom.source)
        } while (index >= count)
        return index
    }
}
