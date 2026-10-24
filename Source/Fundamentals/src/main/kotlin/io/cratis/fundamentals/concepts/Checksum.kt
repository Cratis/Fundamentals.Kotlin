// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

/** Check digits appended over all ASCII digits in a generated payload. */
public enum class Checksum {
    /** No appended check digits; not supported for validation. */
    NONE,
    /** One Luhn check digit. */
    LUHN,
    /** One Mod11 check digit, excluding payloads whose check is 10. */
    MOD11,
    /** Two Mod97 check digits. */
    MOD97;

    public companion object {
        /** Validates exact trailing check digits, ignoring printable nondigits in the payload. */
        @JvmStatic
        public fun hasValidChecksum(value: String, checksum: Checksum): Boolean = GenerationChecksum.matches(value, checksum)
    }
}
