// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.nio.ByteBuffer
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

/** Generates values using cryptographically secure randomness. */
public object GenerateValue {
    private const val DEFAULT_NANO_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-"

    /** Creates an RFC version 4 UUID with the RFC variant. */
    @JvmStatic
    public fun uuid(): UUID = UUID.randomUUID()

    /** Creates a version 7 UUID using the current time, without a monotonic counter. */
    @JvmStatic
    public fun uuidV7(): UUID = uuidV7(GenerationContext(Instant.now()))

    /** Creates a version 7 UUID containing the operation's UTC Unix-epoch milliseconds. */
    @JvmStatic
    public fun uuidV7(context: GenerationContext): UUID {
        val buffer = ByteBuffer.wrap(uuidV7Bytes(context))
        return UUID(buffer.long, buffer.long)
    }

    internal fun uuidV7Bytes(context: GenerationContext): ByteArray {
        validateGenerationTime(context)
        val bytes = timestampBytes(context)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        return bytes
    }

    /** Creates a ULID using the current time, without a monotonic counter. */
    @JvmStatic
    public fun ulid(): String = ulid(GenerationContext(Instant.now()))

    /** Creates a ULID containing captured UTC milliseconds and 80 random bits. */
    @JvmStatic
    public fun ulid(context: GenerationContext): String {
        validateGenerationTime(context)
        return Base32Encoder.encode(timestampBytes(context), Base32Encoder.CROCKFORD_UPPER, true)
    }

    /** Creates a TypeID with a validated prefix and a current-time UUID v7 suffix. */
    @JvmStatic
    public fun typeId(prefix: String): String = typeId(prefix, GenerationContext(Instant.now()))

    /** Creates a TypeID with a validated prefix and the operation's UUID v7 suffix. */
    @JvmStatic
    public fun typeId(prefix: String, context: GenerationContext): String {
        validateTypeIdPrefix(prefix)
        val suffix = Base32Encoder.encode(uuidV7Bytes(context), Base32Encoder.CROCKFORD_LOWER, true)
        return if (prefix.isEmpty()) suffix else "${prefix}_$suffix"
    }

    /** Creates 1–256 uniformly random characters from a distinct printable ASCII alphabet. */
    @JvmStatic
    @JvmOverloads
    public fun nanoId(size: Int = 21, alphabet: String = DEFAULT_NANO_ID_ALPHABET): String {
        if (size !in 1..256) throw GenerationErrors.nanoIdSize(size)
        if (alphabet.length !in 2..256) throw GenerationErrors.nanoIdAlphabet()
        if (alphabet.any { it !in ' '..'~' }) throw GenerationErrors.nanoIdAlphabet()
        if (alphabet.toSet().size != alphabet.length) throw GenerationErrors.nanoIdAlphabet()
        return CharArray(size) { alphabet[GenerationRandom.source.nextInt(alphabet.length)] }.concatToString()
    }

    /** Creates 1–512 random bytes encoded without padding characters. */
    @JvmStatic
    @JvmOverloads
    public fun bytes(count: Int, encoding: ValueEncoding = ValueEncoding.BASE64_URL): String {
        if (count !in 1..512) throw GenerationErrors.byteCount(count)
        val bytes = ByteArray(count).also(GenerationRandom.source::nextBytes)
        return when (encoding) {
            ValueEncoding.BASE64_URL -> Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            ValueEncoding.HEX -> HexFormat.of().formatHex(bytes)
            ValueEncoding.BASE32 -> Base32Encoder.encode(bytes, Base32Encoder.RFC4648)
        }
    }

    /** Creates an unbiased integer between the inclusive signed 64-bit bounds. */
    @JvmStatic
    public fun integer(minimum: Long, maximum: Long): Long {
        if (minimum > maximum) throw GenerationErrors.integerRange(minimum, maximum)
        val span = (maximum - minimum + 1).toULong()
        val threshold = if (span == 0uL) 0uL else (0uL - span) % span
        var random: ULong
        do {
            random = GenerationRandom.source.nextLong().toULong()
        } while (random < threshold)
        return (minimum.toULong() + if (span == 0uL) random else random % span).toLong()
    }

    private fun timestampBytes(context: GenerationContext): ByteArray {
        val bytes = ByteArray(16).also(GenerationRandom.source::nextBytes)
        val timestamp = context.now.toEpochMilli()
        for (index in 0 until 6) bytes[index] = (timestamp ushr (40 - index * 8)).toByte()
        return bytes
    }

    private fun validateTypeIdPrefix(prefix: String) {
        if (prefix.length > 63) throw GenerationErrors.typeIdPrefix(prefix)
        if (prefix.isEmpty()) return
        if (prefix.first() !in 'a'..'z') throw GenerationErrors.typeIdPrefix(prefix)
        if (prefix.last() !in 'a'..'z') throw GenerationErrors.typeIdPrefix(prefix)
        if (prefix.any { it !in 'a'..'z' && it != '_' }) throw GenerationErrors.typeIdPrefix(prefix)
    }
}
