// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function

/** A non-null result or typed error. Mirrors Monads/Result{TResult,TError}.cs@6ae7eb7. */
public sealed class ResultWithValue<TResult : Any, TError : Any> {
    /** The operation succeeded with [value]. */
    public data class Success<TResult : Any, TError : Any>(public val value: TResult) : ResultWithValue<TResult, TError>()

    /** The operation failed with [error]. */
    public data class Failure<TResult : Any, TError : Any>(public val error: TError) : ResultWithValue<TResult, TError>()

    /** Whether the operation succeeded. */
    public val isSuccess: Boolean get() = this is Success

    /** Returns the result, or an empty optional on failure. */
    public fun tryGetResult(): Optional<TResult> = when (this) {
        is Success -> Optional.of(value)
        is Failure -> Optional.empty()
    }

    /** Returns the error, or an empty optional on success. */
    public fun tryGetError(): Optional<TError> = when (this) {
        is Success -> Optional.empty()
        is Failure -> Optional.of(error)
    }

    /** Returns the selected branch's result. */
    public fun <R> match(success: Function<in TResult, out R>, failure: Function<in TError, out R>): R = when (this) {
        is Success -> success.apply(value)
        is Failure -> failure.apply(error)
    }

    /** Invokes only the selected branch. */
    public fun switchOn(success: Consumer<in TResult>, failure: Consumer<in TError>): Unit = when (this) {
        is Success -> success.accept(value)
        is Failure -> failure.accept(error)
    }

    public companion object {
        /** Creates a result carrying a non-null value. */
        @JvmStatic
        public fun <TResult : Any, TError : Any> success(value: TResult): ResultWithValue<TResult, TError> = Success(value)

        /** Creates a result carrying a non-null error. */
        @JvmStatic
        public fun <TResult : Any, TError : Any> failed(error: TError): ResultWithValue<TResult, TError> = Failure(error)
    }
}
