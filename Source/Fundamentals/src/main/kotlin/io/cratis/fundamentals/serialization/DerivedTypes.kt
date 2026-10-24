// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import io.cratis.fundamentals.concepts.ConceptAs
import java.lang.reflect.Modifier

/** Explicit, immutable derived-type registrations. No classpath scanning or discovery is performed. */
public class DerivedTypes private constructor(
    private val byTargetAndId: Map<Pair<Class<*>, DerivedTypeId>, Class<*>>,
    private val primaryTargets: Map<Class<*>, Class<*>>
) {
    /** All primary targets and non-JDK superclasses with registered derivatives. */
    public val typesWithDerivatives: Set<Class<*>> = java.util.Set.copyOf(byTargetAndId.keys.map { it.first })

    /** Resolves [id] within [targetType], or throws [IllegalArgumentException]. */
    public fun derivedTypeFor(targetType: Class<*>, id: DerivedTypeId): Class<*> =
        byTargetAndId[targetType to id]
            ?: throw IllegalArgumentException("missing derived type for target type ${targetType.name}, id '$id'")

    /** Returns the primary target, or throws [IllegalArgumentException] for an unregistered type. */
    public fun targetTypeFor(derivedType: Class<*>): Class<*> = primaryTargets[derivedType]
        ?: throw IllegalArgumentException("missing target type for derived type ${derivedType.name}")

    public fun isDerivedType(type: Class<*>): Boolean = primaryTargets.containsKey(type)

    public fun hasDerivatives(type: Class<*>): Boolean = type in typesWithDerivatives

    public companion object {
        @JvmField
        public val EMPTY: DerivedTypes = DerivedTypes(emptyMap(), emptyMap())

        /** Builds registrations from annotated concrete classes; duplicate classes are idempotent. */
        @JvmStatic
        public fun of(vararg types: Class<*>): DerivedTypes = of(types.asIterable())

        /** Copies [types], rejecting invalid registrations and globally ambiguous ids. */
        @JvmStatic
        public fun of(types: Iterable<Class<*>>): DerivedTypes {
            val registrations = types.toSet().associateWith(::annotationFor)
            val ids = mutableMapOf<String, Class<*>>()
            val index = mutableMapOf<Pair<Class<*>, DerivedTypeId>, Class<*>>()
            val primaryTargets = mutableMapOf<Class<*>, Class<*>>()
            for ((type, annotation) in registrations) {
                val previous = ids.putIfAbsent(annotation.value, type)
                require(previous == null) {
                    "ambiguous derived type identifier '${annotation.value}' for ${previous?.name} and ${type.name}"
                }
                val bases = generateSequence(type.superclass) { it.superclass }.filterNot(::isJdk).toList()
                val primary = primaryTarget(type, annotation, bases)
                primaryTargets[type] = primary
                for (target in bases + primary) index[target to DerivedTypeId(annotation.value)] = type
            }
            return DerivedTypes(java.util.Map.copyOf(index), java.util.Map.copyOf(primaryTargets))
        }
    }
}

private fun annotationFor(type: Class<*>): DerivedType {
    val annotation = requireNotNull(type.getDeclaredAnnotation(DerivedType::class.java)) {
        "missing @DerivedType for ${type.name}"
    }
    require(!type.isInterface && !Modifier.isAbstract(type.modifiers) && !type.isEnum && !ConceptAs.isConcept(type)) {
        "invalid derived type ${type.name}, id '${annotation.value}': expected a concrete non-enum, non-concept class"
    }
    require(annotation.value.isNotBlank()) { "blank derived type identifier for ${type.name}" }
    return annotation
}

private fun primaryTarget(type: Class<*>, annotation: DerivedType, bases: List<Class<*>>): Class<*> {
    val explicit = annotation.targetType.java
    if (explicit != Any::class.java) {
        require(explicit.isAssignableFrom(type)) { "target type mismatch for ${type.name}, id '${annotation.value}': ${explicit.name}" }
        return explicit
    }
    val interfaces = allInterfaces(type).filterNot(::isJdk)
    require(interfaces.size <= 1) { "ambiguous target type for ${type.name}, id '${annotation.value}'" }
    return interfaces.singleOrNull() ?: bases.firstOrNull()
        ?: throw IllegalArgumentException("missing target type for ${type.name}, id '${annotation.value}'")
}

private fun allInterfaces(type: Class<*>): Set<Class<*>> = buildSet {
    for (parent in type.interfaces) {
        add(parent)
        addAll(allInterfaces(parent))
    }
    type.superclass?.let { addAll(allInterfaces(it)) }
}

private fun isJdk(type: Class<*>): Boolean =
    listOf("java.", "javax.", "jdk.", "sun.", "kotlin.").any(type.name::startsWith)
