// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import io.cratis.fundamentals.concepts.ConceptAs

/** The string identity of a derived type, unrelated to Chronicle event identity. */
public class DerivedTypeId(private val id: String) : ConceptAs<String> {
    public override fun value(): String = id

    public override fun equals(other: Any?): Boolean = other is DerivedTypeId && id == other.id

    public override fun hashCode(): Int = id.hashCode()

    public override fun toString(): String = id
}
