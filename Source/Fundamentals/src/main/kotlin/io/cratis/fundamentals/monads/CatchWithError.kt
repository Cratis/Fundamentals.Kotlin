// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function

/**
 * A non-null result, typed error or captured exception. Mirrors Monads/Catch{TResult,TError}.cs@6ae7eb7.
 * When TError is itself an Exception type, use [Failure] directly to represent a typed error.
 */
public sealed class CatchWithError<TResult : Any, TError : Any> {
    /** The operation succeeded with [value]. */
    public data class Success<TResult : Any, TError : Any>(public val value: TResult) : CatchWithError<TResult, TError>()

    /** The operation failed with a typed [error], not a captured exception. */
    public data class Failure<TResult : Any, TError : Any>(public val error: TError) : CatchWithError<TResult, TError>()

    /** The operation produced [exception]. */
    public data class Caught<TResult : Any, TError : Any>(public val exception: Exception) : CatchWithError<TResult, TError>()

    /** True only for a successful result, not for either kind of failure. */
    public val isSuccess: Boolean get() = this is Success

    /** Returns the result when successful. */
    public fun tryGetResult(): Optional<TResult> = when (this) {
        is Success -> Optional.of(value)
        is Failure, is Caught -> Optional.empty()
    }

    /** Returns the typed error when present. */
    public fun tryGetError(): Optional<TError> = when (this) {
        is Failure -> Optional.of(error)
        is Success, is Caught -> Optional.empty()
    }

    /** Returns the captured exception when present. */
    public fun tryGetException(): Optional<Exception> = when (this) {
        is Caught -> Optional.of(exception)
        is Success, is Failure -> Optional.empty()
    }

    /** Returns the selected branch's result. */
    public fun <R> match(
        success: Function<in TResult, out R>,
        failure: Function<in TError, out R>,
        caught: Function<in Exception, out R>
    ): R = when (this) {
        is Success -> success.apply(value)
        is Failure -> failure.apply(error)
        is Caught -> caught.apply(exception)
    }

    /** Invokes only the selected branch. */
    public fun switchOn(
        success: Consumer<in TResult>,
        failure: Consumer<in TError>,
        caught: Consumer<in Exception>
    ): Unit = when (this) {
        is Success -> success.accept(value)
        is Failure -> failure.accept(error)
        is Caught -> caught.accept(exception)
    }

    /** Rethrows the original exception unchanged; does nothing for success or a typed error. */
    @Throws(Exception::class)
    public fun rethrowError() {
        if (this is Caught) throw exception
    }

    public companion object {
        /** Creates an operation carrying a non-null value. */
        @JvmStatic
        public fun <TResult : Any, TError : Any> success(value: TResult): CatchWithError<TResult, TError> = Success(value)

        /** Creates an operation carrying a non-null typed error. Use [Failure] for exception-typed errors. */
        @JvmStatic
        public fun <TResult : Any, TError : Any> failed(error: TError): CatchWithError<TResult, TError> = Failure(error)

        /** Creates an operation carrying the original exception; preferred for exception arguments. */
        @JvmStatic
        public fun <TResult : Any, TError : Any> failed(exception: Exception): CatchWithError<TResult, TError> = Caught(exception)
    }
}
