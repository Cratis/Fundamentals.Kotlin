// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatchTest {
    private enum class TheErrorType { SOME_TYPE, SOME_OTHER_TYPE }
    private val error = IllegalStateException("original")

    @Test
    fun `no value success and exception mirror C sharp specs`() {
        val success = Catch.success()
        val caught = Catch.failed(error)
        assertTrue(success.isSuccess)
        assertTrue(success.tryGetException().isEmpty)
        assertEquals("none", success.match({ "none" }, { it.message }))
        assertSame(Catch.Success, success)
        assertFalse(caught.isSuccess)
        assertSame(error, caught.tryGetException().orElseThrow())
        assertSame(error, caught.match({ null }, { it }))
        assertEquals(caught, Catch.failed(error))
        assertEquals(caught.hashCode(), Catch.failed(error).hashCode())
        val calls = mutableListOf<String>()
        success.switchOn({ calls.add("none") }, { calls.add("exception") })
        caught.switchOn({ calls.add("none") }, { calls.add("exception") })
        assertEquals(listOf("none", "exception"), calls)
    }

    @Test
    fun `value success and exception mirror C sharp specs`() {
        val success: CatchWithValue<Int> = CatchWithValue.success(42)
        val caught: CatchWithValue<Int> = CatchWithValue.failed(error)
        assertTrue(success.isSuccess)
        assertEquals(Optional.of(42), success.tryGetResult())
        assertTrue(success.tryGetException().isEmpty)
        assertEquals(42, success.match({ it }, { -1 }))
        assertFalse(caught.isSuccess)
        assertTrue(caught.tryGetResult().isEmpty)
        assertSame(error, caught.tryGetException().orElseThrow())
        assertSame(error, caught.match({ null }, { it }))
        assertEquals(success, CatchWithValue.success(42))
        assertEquals(success.hashCode(), CatchWithValue.success(42).hashCode())
        assertEquals(caught, CatchWithValue.failed<Int>(error))
        assertEquals(caught.hashCode(), CatchWithValue.failed<Int>(error).hashCode())
        val calls = mutableListOf<String>()
        success.switchOn({ calls.add(it.toString()) }, { calls.add("exception") })
        caught.switchOn({ calls.add(it.toString()) }, { calls.add("exception") })
        assertEquals(listOf("42", "exception"), calls)
    }

    @Test
    fun `three way cases mirror C sharp specs and infer overloaded factories`() {
        val success: CatchWithError<Int, TheErrorType> = CatchWithError.success(42)
        val failure: CatchWithError<Int, TheErrorType> = CatchWithError.failed(TheErrorType.SOME_OTHER_TYPE)
        val caught: CatchWithError<Int, TheErrorType> = CatchWithError.failed(error)
        assertTrue(success.isSuccess)
        assertEquals(Optional.of(42), success.tryGetResult())
        assertTrue(success.tryGetError().isEmpty)
        assertTrue(success.tryGetException().isEmpty)
        assertEquals(42, success.match({ it }, { -1 }, { -2 }))
        assertFalse(failure.isSuccess)
        assertTrue(failure.tryGetResult().isEmpty)
        assertEquals(Optional.of(TheErrorType.SOME_OTHER_TYPE), failure.tryGetError())
        assertTrue(failure.tryGetException().isEmpty)
        assertEquals(TheErrorType.SOME_OTHER_TYPE, failure.match({ null }, { it }, { null }))
        assertFalse(caught.isSuccess)
        assertTrue(caught.tryGetResult().isEmpty)
        assertTrue(caught.tryGetError().isEmpty)
        assertSame(error, caught.tryGetException().orElseThrow())
        assertSame(error, caught.match({ null }, { null }, { it }))
        assertTrue(CatchWithError.failed<Int, TheErrorType>(error) is CatchWithError.Caught)
        assertTrue(CatchWithError.failed<Int, TheErrorType>(TheErrorType.SOME_TYPE) is CatchWithError.Failure)
    }

    @Test
    fun `three way switch and exhaustive when select only the active case`() {
        val cases: List<CatchWithError<Int, TheErrorType>> = listOf(
            CatchWithError.success(42), CatchWithError.failed(TheErrorType.SOME_TYPE), CatchWithError.failed(error)
        )
        val calls = mutableListOf<String>()
        cases.forEach { result ->
            result.switchOn({ calls.add(it.toString()) }, { calls.add(it.name) }, { calls.add(it.message!!) })
            val description = when (result) {
                is CatchWithError.Success -> result.value.toString()
                is CatchWithError.Failure -> result.error.name
                is CatchWithError.Caught -> result.exception.message
            }
            assertEquals(calls.last(), description)
        }
        assertEquals(listOf("42", "SOME_TYPE", "original"), calls)
        assertEquals(cases, listOf(
            CatchWithError.success<Int, TheErrorType>(42),
            CatchWithError.failed<Int, TheErrorType>(TheErrorType.SOME_TYPE),
            CatchWithError.failed<Int, TheErrorType>(error)
        ))
        assertEquals(cases.map { it.hashCode() }, listOf(
            CatchWithError.Success<Int, TheErrorType>(42).hashCode(),
            CatchWithError.Failure<Int, TheErrorType>(TheErrorType.SOME_TYPE).hashCode(),
            CatchWithError.Caught<Int, TheErrorType>(error).hashCode()
        ))
    }

    @Test
    fun `all caught variants rethrow the original instance and unchanged stack trace`() {
        val stack = error.stackTrace.copyOf()
        val rethrows: List<() -> Unit> = listOf(
            { Catch.failed(error).rethrowError() },
            { CatchWithValue.failed<Int>(error).rethrowError() },
            { CatchWithError.failed<Int, TheErrorType>(error).rethrowError() }
        )
        rethrows.forEach { rethrow ->
            assertSame(error, assertThrows(IllegalStateException::class.java) { rethrow() })
            assertArrayEquals(stack, error.stackTrace)
        }
    }

    @Test
    fun `success and typed error do not rethrow including exception typed errors`() {
        assertDoesNotThrow {
            Catch.success().rethrowError()
            CatchWithValue.success(42).rethrowError()
            CatchWithError.success<Int, TheErrorType>(42).rethrowError()
            CatchWithError.failed<Int, TheErrorType>(TheErrorType.SOME_TYPE).rethrowError()
            CatchWithError.Failure<Int, IllegalStateException>(error).rethrowError()
        }
    }
}
