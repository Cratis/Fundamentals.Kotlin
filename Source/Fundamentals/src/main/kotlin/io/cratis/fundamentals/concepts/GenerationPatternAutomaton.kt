// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.LocalDate

/** Lazily determinizes a bounded position automaton over printable ASCII. */
internal class GenerationPatternAutomaton(
    pattern: GenerationPattern,
    date: LocalDate?,
    reverse: Boolean,
    private val budget: GenerationWorkBudget,
) {
    private data class Edge(val characters: String?, val target: Int)
    private val edges = mutableListOf(mutableListOf<Edge>())
    private val states = mutableListOf<List<Int>>()
    private val identities = mutableMapOf<List<Int>, Int>()
    private val moves = mutableMapOf<Pair<Int, Char>, Int>()
    private val final: Int
    val start: Int

    init {
        if (pattern.tokens.sumOf { it.width * it.maximum } > GenerationWorkBudget.POSITION_LIMIT) {
            budget.exceeded("position budget (2048 character positions)")
        }
        val end = buildPositions(pattern, date)
        final = if (reverse) 0 else end
        if (reverse) reverseEdges()
        start = intern(closure(listOf(if (reverse) end else 0)))
    }

    fun accepts(state: Int): Boolean = final in states[state]
    fun isEmpty(state: Int): Boolean = states[state].isEmpty()

    fun move(state: Int, character: Char): Int {
        val key = state to character
        moves[key]?.let { return it }
        val next = mutableSetOf<Int>()
        for (position in states[state]) {
            for (edge in edges[position]) {
                budget.spend()
                if (edge.characters?.contains(character) == true) next.add(edge.target)
            }
        }
        return intern(closure(next)).also { moves[key] = it }
    }

    private fun buildPositions(pattern: GenerationPattern, date: LocalDate?): Int {
        var current = 0
        var positions = 0
        for (token in pattern.tokens) {
            val finish = node()
            val characters = token.expand(date)
            for (repetition in 1..token.maximum) {
                for (alphabet in characters) {
                    checkPositionLimit(positions)
                    positions++
                    val next = node()
                    edges[current].add(Edge(alphabet, next))
                    current = next
                }
                if (repetition >= token.minimum) edges[current].add(Edge(null, finish))
            }
            current = finish
        }
        return current
    }

    private fun checkPositionLimit(positions: Int) {
        if (positions == GenerationWorkBudget.POSITION_LIMIT) {
            budget.exceeded("position budget (2048 character positions)")
        }
    }

    private fun node(): Int {
        edges.add(mutableListOf())
        return edges.size - 1
    }

    private fun reverseEdges() {
        val reversed = List(edges.size) { mutableListOf<Edge>() }
        for (position in edges.indices) {
            for (edge in edges[position]) {
                budget.spend()
                reversed[edge.target].add(Edge(edge.characters, position))
            }
        }
        for (position in edges.indices) edges[position] = reversed[position]
    }

    private fun closure(positions: Iterable<Int>): List<Int> {
        val reached = positions.toMutableSet()
        val pending = ArrayDeque(reached)
        while (pending.isNotEmpty()) {
            val position = pending.removeLast()
            for (edge in edges[position]) {
                budget.spend()
                if (edge.characters == null && reached.add(edge.target)) pending.addLast(edge.target)
            }
        }
        return reached.sorted()
    }

    private fun intern(positions: List<Int>): Int {
        identities[positions]?.let { return it }
        if (states.size == GenerationWorkBudget.STATE_LIMIT) {
            budget.exceeded("state budget (10000 distinct DFA/product states)")
        }
        val state = states.size
        identities[positions] = state
        states.add(positions)
        return state
    }
}
