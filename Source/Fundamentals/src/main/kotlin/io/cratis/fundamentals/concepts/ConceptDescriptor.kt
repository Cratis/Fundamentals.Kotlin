// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

/**
 * Cached value type, construction and extraction for a concept.
 *
 * Construction prefers a one-component record canonical constructor, then public static
 * `of(value)`, then a public single-argument constructor. Kotlin companion factories need
 * `@JvmStatic`. Compatible scalar inputs are converted: UUID text/.NET bytes, exact integer
 * widths, floating point, decimal, boolean, string and ISO date/time values. Unsupported
 * targets accept identity only; invalid or lossy conversions throw [IllegalArgumentException].
 * Domain validation exceptions propagate unchanged. Null inputs are not accepted.
 */
public class ConceptDescriptor<T : Any> private constructor(
    public val conceptType: Class<T>,
    public val valueType: Class<*>,
    private val construct: (Any) -> Any
) {
    /** Converts [value] to the underlying type and constructs a concept. */
    public fun create(value: Any): T = conceptType.cast(
        construct(ConceptValueConversion.convert(conceptType, valueType, value))
    )

    /** Extracts the underlying value, including null if the implementation permits it. */
    public fun valueOf(concept: T): Any? = (concept as ConceptAs<*>).value()

    public companion object {
        private val descriptors = object : ClassValue<ConceptDescriptor<*>>() {
            override fun computeValue(type: Class<*>): ConceptDescriptor<*> {
                require(ConceptAs.isConcept(type)) { "${type.name} is not a concept" }
                val valueType = ConceptAs.valueTypeOf(type)
                return ConceptDescriptor(type, valueType, ConceptConstruction.resolve(type, valueType))
            }
        }

        /** Resolves and caches a descriptor, rejecting non-concepts or unusable construction shapes eagerly. */
        @JvmStatic
        public fun <T : Any> of(type: Class<T>): ConceptDescriptor<T> {
            @Suppress("UNCHECKED_CAST")
            return descriptors.get(type) as ConceptDescriptor<T>
        }
    }
}
