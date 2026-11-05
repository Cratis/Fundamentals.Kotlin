// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.monads

/**
 * Success or failure without a payload. Mirrors Monads/Result.cs@6ae7eb7.
 * An explicit import shadows kotlin.Result; alias this type when both are needed.
 */
public sealed class Result {
    /** The operation succeeded. */
    public data object Success : Result()

    /** The operation failed. */
    public data object Failure : Result()

    /** Whether the operation succeeded. */
    public val isSuccess: Boolean get() = this is Success

    public companion object {
        /** Creates a successful result. */
        @JvmStatic
        public fun success(): Result = Success

        /** Creates a failed result. */
        @JvmStatic
        public fun failed(): Result = Failure
    }
}
