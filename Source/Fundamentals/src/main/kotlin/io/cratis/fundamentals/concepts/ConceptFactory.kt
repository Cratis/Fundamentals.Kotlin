// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

/** Creates concepts through the cached [ConceptDescriptor] construction and scalar conversion path. */
public object ConceptFactory {
    /** Converts [value] and creates an instance of [type]; null inputs are not accepted. */
    @JvmStatic
    public fun <T : Any> create(type: Class<T>, value: Any): T = ConceptDescriptor.of(type).create(value)
}
