// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

internal class GenerationCompletionCounts(
    private val automaton: GenerationPatternAutomaton,
    private val conditioned: Boolean,
    private val budget: GenerationWorkBudget,
) {
    private data class State(val automaton: Int, val residue: Int, val phase: Int)
    private val completions = mutableMapOf<State, GenerationCompletion>()

    fun build(state: Int, residue: Int = 0, phase: Int = 0): GenerationCompletion = build(State(state, residue, phase))

    private fun build(state: State): GenerationCompletion {
        completions[state]?.let { return it }
        if (completions.size == GenerationWorkBudget.STATE_LIMIT) {
            budget.exceeded("state budget (10000 distinct DFA/product states)")
        }
        val completion = GenerationCompletion(automaton.accepts(state.automaton) && (!conditioned || state.residue != 1))
        completions[state] = completion
        // Acyclic graph: recursion is bounded by the 256-character limit.
        for (character in ' '..'~') {
            budget.spend()
            val target = automaton.move(state.automaton, character)
            if (automaton.isEmpty(target)) continue
            val next = build(nextState(state, target, character))
            completion.branches.add(character to next)
            completion.count += next.count
        }
        return completion
    }

    private fun nextState(state: State, target: Int, character: Char): State {
        val digit = conditioned && character in '0'..'9'
        val residue = if (digit) (state.residue + (character - '0') * (2 + state.phase)) % 11 else state.residue
        val phase = if (digit) (state.phase + 1) % 6 else state.phase
        return State(target, residue, phase)
    }
}
