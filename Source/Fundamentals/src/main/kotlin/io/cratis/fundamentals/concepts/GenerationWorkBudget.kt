// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

internal class GenerationWorkBudget(private val pattern: String, private val limit: Long = OPERATION_LIMIT) {
    var used: Long = 0
        private set

    fun spend(operations: Long = 1) {
        if (operations < 0 || operations > limit - used) exceeded("operation budget ($limit operations)")
        used += operations
    }

    fun exceeded(budget: String): Nothing = throw GenerationErrors.invalidPattern(pattern, "Generation support $budget exceeded.")

    companion object {
        const val POSITION_LIMIT = 2_048
        const val STATE_LIMIT = 10_000
        const val OPERATION_LIMIT = 100_000_000L
    }
}
