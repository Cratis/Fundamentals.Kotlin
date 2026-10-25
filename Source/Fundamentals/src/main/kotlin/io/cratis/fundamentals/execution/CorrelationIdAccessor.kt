// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution

/** Reads the correlation identifier bound to the current execution context. */
public interface CorrelationIdAccessor {
    /** The current [CorrelationId] for the calling thread. */
    public val current: CorrelationId
}
