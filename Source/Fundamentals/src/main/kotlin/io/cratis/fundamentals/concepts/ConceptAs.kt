// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

/**
 * A strongly typed concept backed by a single value. Implementations own null rejection,
 * equality, comparison, validation and string representation.
 *
 * Java records implement it as `record OrderId(UUID value) implements ConceptAs<UUID> {}`.
 * Primitive-backed records must use boxed components: `record Count(Integer value) implements
 * ConceptAs<Integer> {}`, since `int value()` cannot override `Integer value()`.
 */
public interface ConceptAs<T> {
    /** Returns the underlying value. */
    public fun value(): T

    public companion object {
        /** Whether [type] implements the concept contract, excluding the contract itself. */
        @JvmStatic
        public fun isConcept(type: Class<*>): Boolean =
            type != ConceptAs::class.java && ConceptAs::class.java.isAssignableFrom(type)

        /** Resolves the concept's value class through generic interfaces and base classes. */
        @JvmStatic
        public fun valueTypeOf(type: Class<*>): Class<*> {
            require(isConcept(type)) { "${type.name} is not a concept" }
            return findValueType(type, emptyMap())
                ?: throw IllegalArgumentException("${type.name} does not declare a resolved ConceptAs value type")
        }
    }
}

/** Kotlin property view of the scalar value carried by this concept. */
@get:JvmSynthetic
public val <T> ConceptAs<T>.value: T
    get() = value()

private fun resolve(type: Type, bindings: Map<TypeVariable<*>, Type>): Type =
    if (type is TypeVariable<*> && bindings[type] != null && bindings[type] != type) {
        resolve(checkNotNull(bindings[type]), bindings)
    } else {
        type
    }

private fun findValueType(type: Type, inherited: Map<TypeVariable<*>, Type>): Class<*>? {
    val raw = when (type) {
        is Class<*> -> type
        is ParameterizedType -> type.rawType as? Class<*>
        else -> null
    } ?: return null
    val bindings = if (type is ParameterizedType) {
        inherited + raw.typeParameters.zip(type.actualTypeArguments.map { resolve(it, inherited) })
    } else {
        inherited
    }
    if (raw == ConceptAs::class.java) {
        return when (val value = resolve(raw.typeParameters[0], bindings)) {
            is Class<*> -> value
            is ParameterizedType -> value.rawType as? Class<*>
            else -> null
        }
    }
    for (parent in raw.genericInterfaces + listOfNotNull(raw.genericSuperclass)) {
        findValueType(parent, bindings)?.let { return it }
    }
    return null
}
