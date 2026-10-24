// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution

import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.concepts.Generatable
import org.junit.jupiter.api.Assertions.assertInstanceOf
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CorrelationIdTest {
    @Test
    fun `not set wraps the empty uuid`() {
        assertEquals(UUID(0L, 0L), CorrelationId.NOT_SET.value())
        assertEquals("00000000-0000-0000-0000-000000000000", CorrelationId.NOT_SET.toString())
        assertEquals(UUID::class.java, ConceptAs.valueTypeOf(CorrelationId::class.java))
    }

    @Test
    fun `new generates distinct nonzero version four uuids`() {
        val first = CorrelationId.create()
        val second = CorrelationId.create()
        assertInstanceOf(Generatable::class.java, first)
        assertNotEquals(CorrelationId.NOT_SET, first)
        assertNotEquals(first, second)
        assertEquals(4, first.value().version())
        assertEquals(2, first.value().variant())
    }

    @Test
    fun `identifiers compare by value`() {
        val value = UUID.randomUUID()
        assertEquals(CorrelationId(value), CorrelationId(value))
        assertEquals(CorrelationId(value).hashCode(), CorrelationId(value).hashCode())
    }

    @Test
    fun `string representation is the canonical lowercase uuid`() {
        val value = UUID.fromString("A1B2C3D4-1234-4567-89AB-CDEF01234567")
        assertEquals("a1b2c3d4-1234-4567-89ab-cdef01234567", CorrelationId(value).toString())
    }
}
