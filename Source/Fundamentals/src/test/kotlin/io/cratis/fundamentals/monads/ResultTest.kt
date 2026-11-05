// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {
    private enum class TheErrorType { SOME_TYPE, SOME_OTHER_TYPE }

    @Test
    fun `plain results expose success and singleton cases`() {
        assertTrue(Result.success().isSuccess)
        assertFalse(Result.failed().isSuccess)
        assertSame(Result.Success, Result.success())
        assertSame(Result.Failure, Result.failed())
    }

    @Test
    fun `typed errors and no value successes mirror C sharp specs`() {
        val failure: ResultWithError<TheErrorType> = ResultWithError.failed(TheErrorType.SOME_TYPE)
        val success: ResultWithError<TheErrorType> = ResultWithError.success()
        assertFalse(failure.isSuccess)
        assertEquals(Optional.of(TheErrorType.SOME_TYPE), failure.tryGetError())
        assertEquals(TheErrorType.SOME_TYPE, failure.match({ null }, { it }))
        assertTrue(success.isSuccess)
        assertTrue(success.tryGetError().isEmpty)
        assertEquals("none", success.match({ "none" }, { it.name }))
        assertEquals("none", describe(success))
        assertEquals("SOME_TYPE", describe(failure))
        assertEquals(success, ResultWithError.success<TheErrorType>())
        assertEquals(success.hashCode(), ResultWithError.success<TheErrorType>().hashCode())
        assertEquals(failure, ResultWithError.failed(TheErrorType.SOME_TYPE))
        assertEquals(failure.hashCode(), ResultWithError.failed(TheErrorType.SOME_TYPE).hashCode())
    }

    @Test
    fun `value and error cases mirror C sharp specs with inferred factories`() {
        val success: ResultWithValue<Int, TheErrorType> = ResultWithValue.success(42)
        val failure = failedValue()
        assertTrue(success.isSuccess)
        assertEquals(Optional.of(42), success.tryGetResult())
        assertTrue(success.tryGetError().isEmpty)
        assertEquals(42, success.match({ it }, { -1 }))
        assertFalse(failure.isSuccess)
        assertTrue(failure.tryGetResult().isEmpty)
        assertEquals(Optional.of(TheErrorType.SOME_OTHER_TYPE), failure.tryGetError())
        assertEquals(TheErrorType.SOME_OTHER_TYPE, failure.match({ null }, { it }))
        assertEquals("42", describe(success))
        assertEquals("SOME_OTHER_TYPE", describe(failure))
        assertEquals(success, ResultWithValue.success<Int, TheErrorType>(42))
        assertEquals(success.hashCode(), ResultWithValue.success<Int, TheErrorType>(42).hashCode())
        assertEquals(failure, failedValue())
        assertEquals(failure.hashCode(), failedValue().hashCode())
    }

    @Test
    fun `switch invokes only the selected branch once`() {
        val calls = mutableListOf<String>()
        val successes: List<ResultWithError<TheErrorType>> = listOf(
            ResultWithError.success(), ResultWithError.failed(TheErrorType.SOME_TYPE)
        )
        successes.forEach { it.switchOn({ calls.add("none") }, { error -> calls.add(error.name) }) }
        val values: List<ResultWithValue<Int, TheErrorType>> = listOf(ResultWithValue.success(42), failedValue())
        values.forEach { it.switchOn({ value -> calls.add(value.toString()) }, { error -> calls.add(error.name) }) }
        assertEquals(listOf("none", "SOME_TYPE", "42", "SOME_OTHER_TYPE"), calls)
    }

    private fun failedValue(): ResultWithValue<Int, TheErrorType> = ResultWithValue.failed(TheErrorType.SOME_OTHER_TYPE)

    private fun describe(result: ResultWithError<TheErrorType>): String = when (result) {
        is ResultWithError.Success -> "none"
        is ResultWithError.Failure -> result.error.name
    }

    private fun describe(result: ResultWithValue<Int, TheErrorType>): String = when (result) {
        is ResultWithValue.Success -> result.value.toString()
        is ResultWithValue.Failure -> result.error.name
    }
}
