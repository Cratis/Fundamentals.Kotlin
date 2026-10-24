// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GenerationContextTest {
    @Test
    fun `default tenant and value semantics preserve captured operation`() {
        assertEquals("00000000-0000-0000-0000-000000000000", GenerationContext.DEFAULT_TENANT)
        val context = GenerationContext(Instant.EPOCH)
        assertEquals(GenerationContext.DEFAULT_TENANT, context.tenant)
        assertEquals(Instant.EPOCH, context.now)
        assertEquals(context, GenerationContext(Instant.EPOCH, context.tenant))
        assertEquals(context.hashCode(), GenerationContext(Instant.EPOCH).hashCode())
        assertNotEquals(context, GenerationContext(Instant.EPOCH, "other"))
        assertNotEquals(context, GenerationContext(Instant.EPOCH.plusSeconds(1)))
        assertNotEquals(context, null)
        assertEquals("GenerationContext(now=1970-01-01T00:00:00Z, tenant=${context.tenant})", context.toString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["+10000-01-01T00:00:00Z", "0000-12-31T23:59:59Z"])
    fun `rejects years outside the C sharp range`(value: String) {
        assertThrows(IllegalArgumentException::class.java) { GenerationContext(Instant.parse(value)) }
    }
}
