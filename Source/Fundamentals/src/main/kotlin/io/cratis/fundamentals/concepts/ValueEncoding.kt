// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

/** Unpadded encodings for generated random bytes. */
public enum class ValueEncoding {
    /** URL-safe base64 without padding. */
    BASE64_URL,
    /** Lowercase hexadecimal. */
    HEX,
    /** Uppercase RFC 4648 base32 without padding. */
    BASE32,
}
