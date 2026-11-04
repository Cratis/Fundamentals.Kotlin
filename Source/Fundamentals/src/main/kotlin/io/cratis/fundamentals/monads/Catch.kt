// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

import java.lang.Runnable
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/** Success without a value or a captured exception. Mirrors Monads/Catch.cs@6ae7eb7. */
public sealed class Catch {
    /** The operation succeeded. */
    public data object Success : Catch()

    /** The operation produced [exception]. */
    public data class Caught(public val exception: Exception) : Catch()

    /** Whether the operation succeeded. */
    public val isSuccess: Boolean get() = this is Success

    /** Returns the exception, or an empty optional on success. */
    public fun tryGetException(): Optional<Exception> = when (this) {
        is Success -> Optional.empty()
        is Caught -> Optional.of(exception)
    }

    /** Returns the selected branch's result. */
    public fun <R> match(success: Supplier<out R>, caught: Function<in Exception, out R>): R = when (this) {
        is Success -> success.get()
        is Caught -> caught.apply(exception)
    }

    /** Invokes only the selected branch. */
    public fun switchOn(success: Runnable, caught: Consumer<in Exception>): Unit = when (this) {
        is Success -> success.run()
        is Caught -> caught.accept(exception)
    }

    /** Rethrows the original exception unchanged; does nothing on success. */
    @Throws(Exception::class)
    public fun rethrowError() {
        if (this is Caught) throw exception
    }

    public companion object {
        /** Creates a successful operation without a value. */
        @JvmStatic
        public fun success(): Catch = Success

        /** Creates an operation carrying the original exception. */
        @JvmStatic
        public fun failed(exception: Exception): Catch = Caught(exception)
    }
}
