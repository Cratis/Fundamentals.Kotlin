// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal class GenerationPatternSupport(
    private val pattern: GenerationPattern,
    date: LocalDate? = null,
    budget: GenerationWorkBudget = GenerationWorkBudget(pattern.text),
) {
    private val start: GenerationCompletion
    val count: BigInteger get() = start.count

    init {
        if (pattern.hasDates && date == null) throw GenerationErrors.requiresContext(pattern)
        val conditioned = pattern.checksum == Checksum.MOD11
        val automaton = GenerationPatternAutomaton(pattern, date, conditioned, budget)
        start = GenerationCompletionCounts(automaton, conditioned, budget).build(automaton.start)
    }

    fun unrank(index: BigInteger): String {
        validateIndex(index)
        var node = start
        var remaining = index
        val characters = StringBuilder()
        while (true) {
            if (node.accepts) {
                if (remaining == BigInteger.ZERO) break
                remaining -= BigInteger.ONE
            }
            val branch = selectBranch(node, remaining)
            characters.append(branch.character)
            node = branch.target
            remaining = branch.index
        }
        if (pattern.checksum == Checksum.MOD11) characters.reverse()
        val payload = characters.toString()
        return payload + GenerationChecksum.calculate(payload, pattern.checksum)
    }

    private fun validateIndex(index: BigInteger) {
        if (index < BigInteger.ZERO || index >= count) {
            throw GenerationErrors.invalidPattern(pattern.text, "support index is outside the counted range")
        }
    }

    private data class Branch(val character: Char, val target: GenerationCompletion, val index: BigInteger)

    private fun selectBranch(node: GenerationCompletion, index: BigInteger): Branch {
        var remaining = index
        for ((character, target) in node.branches) {
            if (remaining < target.count) return Branch(character, target, remaining)
            remaining -= target.count
        }
        throw GenerationErrors.emptySupport(pattern)
    }

    companion object {
        /** Shape membership only; checksum arithmetic is intentionally independent. */
        fun matches(pattern: GenerationPattern, value: String, now: Instant? = null): Boolean {
            if (value.length !in pattern.minimumLength..pattern.maximumLength) return false
            val width = pattern.checksumWidth
            if (width > 0 && value.takeLast(width).any { it !in '0'..'9' }) return false
            val date = now?.atOffset(ZoneOffset.UTC)?.toLocalDate()
            val automaton = GenerationPatternAutomaton(pattern, date, false, GenerationWorkBudget(pattern.text))
            var state = automaton.start
            for (character in value.dropLast(width)) {
                state = automaton.move(state, character)
                if (automaton.isEmpty(state)) return false
            }
            return automaton.accepts(state)
        }
    }
}
