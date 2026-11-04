// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.lang.Runnable
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/** Success or a non-null typed error. Mirrors Monads/Result{TError}.cs@6ae7eb7. */
public sealed class ResultWithError<TError : Any> {
    /** The operation succeeded without a value. */
    public class Success<TError : Any> : ResultWithError<TError>() {
        public override fun equals(other: Any?): Boolean = other is Success<*>
        public override fun hashCode(): Int = 0
        public override fun toString(): String = "Success"
    }

    /** The operation failed with [error]. */
    public data class Failure<TError : Any>(public val error: TError) : ResultWithError<TError>()

    /** Whether the operation succeeded. */
    public val isSuccess: Boolean get() = this is Success

    /** Returns the error, or an empty optional on success. */
    public fun tryGetError(): Optional<TError> = when (this) {
        is Success -> Optional.empty()
        is Failure -> Optional.of(error)
    }

    /** Returns the selected branch's result. */
    public fun <R> match(success: Supplier<out R>, failure: Function<in TError, out R>): R = when (this) {
        is Success -> success.get()
        is Failure -> failure.apply(error)
    }

    /** Invokes only the selected branch. */
    public fun switchOn(success: Runnable, failure: Consumer<in TError>): Unit = when (this) {
        is Success -> success.run()
        is Failure -> failure.accept(error)
    }

    public companion object {
        /** Creates a successful result without a value. */
        @JvmStatic
        public fun <TError : Any> success(): ResultWithError<TError> = Success()

        /** Creates a result carrying a non-null error. */
        @JvmStatic
        public fun <TError : Any> failed(error: TError): ResultWithError<TError> = Failure(error)
    }
}
