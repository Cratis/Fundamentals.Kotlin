// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Executors

class CorrelationIdContextTest {
    private val context = CorrelationIdContext()

    @AfterEach
    fun cleanup() {
        context.clear()
        CorrelationIdContext.shared().clear()
    }

    @Test
    fun `first read generates a stable nonzero identifier`() {
        val first = context.current
        assertNotEquals(CorrelationId.NOT_SET, first)
        assertEquals(first, context.current)
    }

    @Test
    fun `set including not set is retained`() {
        val id = CorrelationId(UUID.randomUUID())
        context.set(id)
        assertEquals(id, context.current)
        context.set(CorrelationId.NOT_SET)
        assertEquals(CorrelationId.NOT_SET, context.current)
    }

    @Test
    fun `clear removes the binding so the next read generates a new identifier`() {
        val original = context.current
        context.clear()
        assertNotEquals(original, context.current)
    }

    @Test
    fun `instances are isolated and shared is a singleton`() {
        val other = CorrelationIdContext()
        val id = CorrelationId(UUID.randomUUID())
        context.set(id)
        assertNotEquals(id, other.current)
        assertNotEquals(id, CorrelationIdContext.shared().current)
        assertSame(CorrelationIdContext.shared(), CorrelationIdContext.shared())
        assertNotSame(context, CorrelationIdContext.shared())
        other.clear()
    }

    @Test
    fun `worker threads do not inherit and are fresh after clear on reuse`() {
        val id = CorrelationId(UUID.randomUUID())
        context.set(id)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val seen = executor.submit<CorrelationId> { context.current }.get()
            assertNotEquals(id, seen)
            val reused = executor.submit<CorrelationId> {
                val bound = CorrelationId(UUID.randomUUID())
                context.set(bound)
                context.clear()
                context.current
            }.get()
            assertNotEquals(seen, reused)
        } finally {
            executor.shutdownNow()
        }
        assertEquals(id, context.current)
    }

    @Test
    fun `accessor and modifier views share the same storage`() {
        val accessor: CorrelationIdAccessor = context
        val modifier: CorrelationIdModifier = context
        val id = CorrelationId(UUID.randomUUID())
        modifier.set(id)
        assertEquals(id, accessor.current)
    }
}
