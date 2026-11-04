// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function

/** A non-null result or a captured exception. Mirrors Monads/Catch{TResult}.cs@6ae7eb7. */
public sealed class CatchWithValue<TResult : Any> {
    /** The operation succeeded with [value]. */
    public data class Success<TResult : Any>(public val value: TResult) : CatchWithValue<TResult>()

    /** The operation produced [exception]. */
    public data class Caught<TResult : Any>(public val exception: Exception) : CatchWithValue<TResult>()

    /** Whether the operation succeeded. */
    public val isSuccess: Boolean get() = this is Success

    /** Returns the result, or an empty optional for a captured exception. */
    public fun tryGetResult(): Optional<TResult> = when (this) {
        is Success -> Optional.of(value)
        is Caught -> Optional.empty()
    }

    /** Returns the exception, or an empty optional on success. */
    public fun tryGetException(): Optional<Exception> = when (this) {
        is Success -> Optional.empty()
        is Caught -> Optional.of(exception)
    }

    /** Returns the selected branch's result. */
    public fun <R> match(success: Function<in TResult, out R>, caught: Function<in Exception, out R>): R = when (this) {
        is Success -> success.apply(value)
        is Caught -> caught.apply(exception)
    }

    /** Invokes only the selected branch. */
    public fun switchOn(success: Consumer<in TResult>, caught: Consumer<in Exception>): Unit = when (this) {
        is Success -> success.accept(value)
        is Caught -> caught.accept(exception)
    }

    /** Rethrows the original exception unchanged; does nothing on success. */
    @Throws(Exception::class)
    public fun rethrowError() {
        if (this is Caught) throw exception
    }

    public companion object {
        /** Creates an operation carrying a non-null value. */
        @JvmStatic
        public fun <TResult : Any> success(value: TResult): CatchWithValue<TResult> = Success(value)

        /** Creates an operation carrying the original exception. */
        @JvmStatic
        public fun <TResult : Any> failed(exception: Exception): CatchWithValue<TResult> = Caught(exception)
    }
}
