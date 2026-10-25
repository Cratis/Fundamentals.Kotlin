// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution

/** Modifies the correlation identifier bound to the current execution context. */
public interface CorrelationIdModifier {
    /** Binds [id] as the current correlation identifier of the calling thread. Any value, including [CorrelationId.NOT_SET], is accepted. */
    public fun set(id: CorrelationId)

    /** Removes the binding of the calling thread; the next read generates a new identifier. */
    public fun clear()
}
