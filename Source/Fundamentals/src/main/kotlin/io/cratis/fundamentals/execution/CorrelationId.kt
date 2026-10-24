// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution

import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.concepts.Generatable
import io.cratis.fundamentals.concepts.GenerateValue
import java.util.UUID

/** An immutable correlation identifier backed by a UUID. */
public class CorrelationId(private val id: UUID) : ConceptAs<UUID>, Generatable<CorrelationId> {
    public override fun value(): UUID = id

    public override fun equals(other: Any?): Boolean = other is CorrelationId && id == other.id

    public override fun hashCode(): Int = id.hashCode()

    public override fun toString(): String = id.toString()

    public companion object {
        /** Identifier used when no correlation has been assigned. */
        @JvmField
        public val NOT_SET: CorrelationId = CorrelationId(UUID(0L, 0L))

        /** Generates a random UUID v4 identifier, matching .NET New(). */
        @JvmStatic
        public fun create(): CorrelationId = CorrelationId(GenerateValue.uuid())
    }
}
