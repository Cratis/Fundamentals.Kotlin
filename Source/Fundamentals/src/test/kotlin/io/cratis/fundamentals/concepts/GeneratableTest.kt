// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import io.cratis.fundamentals.execution.CorrelationId
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeneratableTest {
    @Test
    fun `concepts expose the static create convention and discoverable marker`() {
        val id: Generatable<*> = SomeId.create()
        assertTrue(id is SomeId)
        val correlation: Any = CorrelationId.create()
        assertTrue(correlation is Generatable<*>)
        assertNotEquals(SomeId.create().value(), SomeId.create().value())
    }

    @Test
    fun `contextual concepts reuse captured time and tenant`() {
        val context = GenerationContext(Instant.parse("2024-06-15T10:34:56.789Z"), "tenant-a")
        val order: GeneratableInContext<*> = OrderId.create(context)
        assertTrue(order is OrderId)
        val value = (order as OrderId).value()
        assertTrue(value.startsWith("order_"))
        assertEquals(context.now.toEpochMilli(), CrockfordDecoding.timestamp(CrockfordDecoding.decode(value.removePrefix("order_"))))
        assertTrue(TenantId.create(context).value().startsWith("tenant-a_"))
    }

    private class SomeId(private val id: UUID) : ConceptAs<UUID>, Generatable<SomeId> {
        override fun value(): UUID = id
        companion object {
            @JvmStatic fun create(): SomeId = SomeId(GenerateValue.uuid())
        }
    }

    private class OrderId(private val id: String) : ConceptAs<String>, GeneratableInContext<OrderId> {
        override fun value(): String = id
        companion object {
            @JvmStatic fun create(context: GenerationContext): OrderId = OrderId(GenerateValue.typeId("order", context))
        }
    }

    private class TenantId(private val id: String) : ConceptAs<String>, GeneratableInContext<TenantId> {
        override fun value(): String = id
        companion object {
            @JvmStatic fun create(context: GenerationContext): TenantId = TenantId(context.tenant + "_" + GenerateValue.ulid(context))
        }
    }
}
