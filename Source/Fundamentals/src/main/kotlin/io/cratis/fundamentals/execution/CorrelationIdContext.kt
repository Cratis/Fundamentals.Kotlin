// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution

/**
 * Thread-bound correlation context implementing both [CorrelationIdAccessor] and [CorrelationIdModifier].
 *
 * Deliberately bounded JVM behaviour, not parity with the .NET `AsyncLocal` implementation:
 * - state is stored per thread in an instance-owned, non-inheritable [ThreadLocal]; worker threads
 *   never inherit a binding, and nothing is propagated across coroutine suspension or dispatcher changes;
 * - the first read on a thread without a binding generates and retains a new identifier (the .NET
 *   accessor returns [CorrelationId.NOT_SET] instead); [clear] removes the binding;
 * - there is no scope stack and no automatic restoration; callers own cleanup;
 * - [CorrelationId.NOT_SET] set explicitly is retained and read back as-is.
 *
 * Instances created with the constructor are isolated from each other and from [shared].
 */
public class CorrelationIdContext : CorrelationIdAccessor, CorrelationIdModifier {
    private val holder: ThreadLocal<CorrelationId?> = ThreadLocal()

    public override val current: CorrelationId
        get() = holder.get() ?: CorrelationId.create().also { holder.set(it) }

    public override fun set(id: CorrelationId) {
        holder.set(id)
    }

    public override fun clear() {
        holder.remove()
    }

    public companion object {
        private val sharedContext: CorrelationIdContext = CorrelationIdContext()

        /** The process-wide shared context; every caller receives the same instance and backing storage. */
        @JvmStatic
        public fun shared(): CorrelationIdContext = sharedContext
    }
}
