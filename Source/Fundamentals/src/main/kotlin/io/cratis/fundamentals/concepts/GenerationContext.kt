// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant
import java.time.ZoneOffset

/** The operation's captured time and portable tenant identifier. */
public class GenerationContext @JvmOverloads public constructor(
    public val now: Instant,
    public val tenant: String = DEFAULT_TENANT,
) {
    init {
        if (now.atOffset(ZoneOffset.UTC).year !in 1..9999) throw GenerationErrors.yearRange(now)
    }

    public override fun equals(other: Any?): Boolean =
        other is GenerationContext && now == other.now && tenant == other.tenant

    public override fun hashCode(): Int = 31 * now.hashCode() + tenant.hashCode()

    public override fun toString(): String = "GenerationContext(now=$now, tenant=$tenant)"

    public companion object {
        /** The zero-UUID identifier of the default tenant. */
        public const val DEFAULT_TENANT: String = "00000000-0000-0000-0000-000000000000"

        internal val MISSING_TIME: Instant = Instant.parse("0001-01-01T00:00:00Z")
    }
}

internal fun validateGenerationTime(context: GenerationContext) {
    if (context.now == GenerationContext.MISSING_TIME) throw GenerationErrors.missingTime()
    if (context.now < Instant.EPOCH) throw GenerationErrors.beforeEpoch(context.now)
}
