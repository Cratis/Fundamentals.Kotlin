// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

/** Property-name encoding for concept map keys registered by [ConceptsModule]. */
public enum class ConceptKeyFormat {
    /** Underlying value text, preserving the default JVM wire format. */
    PLAIN,

    /** Compact embedded JSON, as in C# ComplexKeyDictionaryJsonConverterFactory; also reads plain keys. */
    EMBEDDED_JSON
}
