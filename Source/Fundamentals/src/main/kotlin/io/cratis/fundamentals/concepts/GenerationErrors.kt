// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.time.Instant

internal object GenerationErrors {
    fun invalidPattern(pattern: String, reason: String): IllegalArgumentException =
        IllegalArgumentException("Invalid value pattern '$pattern': $reason")

    fun unknownChecksum(checksum: Checksum): IllegalArgumentException =
        IllegalArgumentException("Checksum '$checksum' is not a supported checksum algorithm for this operation.")

    fun emptySupport(pattern: GenerationPattern): IllegalArgumentException =
        IllegalArgumentException("Value pattern '${pattern.text}' has no values with an emittable check digit.")

    fun requiresContext(pattern: GenerationPattern): IllegalArgumentException =
        IllegalArgumentException("Value pattern '${pattern.text}' requires a generation context for its UTC date segments.")

    fun integerRange(minimum: Long, maximum: Long): IllegalArgumentException =
        IllegalArgumentException("Integer generation minimum '$minimum' exceeds maximum '$maximum'.")

    fun nanoIdSize(size: Int): IllegalArgumentException =
        IllegalArgumentException("NanoID size '$size' must be from 1 to 256 characters.")

    fun nanoIdAlphabet(): IllegalArgumentException = IllegalArgumentException(
        "A NanoID alphabet must contain 2 to 256 distinct printable ASCII characters (0x20 through 0x7E).",
    )

    fun byteCount(count: Int): IllegalArgumentException =
        IllegalArgumentException("Random byte count '$count' must be from 1 to 512.")

    fun typeIdPrefix(prefix: String): IllegalArgumentException = IllegalArgumentException(
        "TypeID prefix '$prefix' must be empty or contain at most 63 lowercase letters or underscores, starting and ending with a letter.",
    )

    fun missingTime(): IllegalArgumentException =
        IllegalArgumentException("A generation context must contain the time the operation was received.")

    fun beforeEpoch(now: Instant): IllegalArgumentException =
        IllegalArgumentException("Generation time '$now' must be on or after the Unix epoch.")

    fun yearRange(now: Instant): IllegalArgumentException =
        IllegalArgumentException("Generation time '$now' must be from year 1 to year 9999 in UTC.")
}
