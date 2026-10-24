// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import io.cratis.fundamentals.concepts.ConceptAs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DerivedTypesTest {
    interface Family
    interface Other
    interface SubFamily : Family
    abstract class Base : Family
    @DerivedType("known") class Known : Base()
    @DerivedType("known") class Duplicate : Other
    @DerivedType("ambiguous") class Ambiguous : Family, Other
    @DerivedType("transitive") class Transitive : SubFamily
    @DerivedType("mismatch", targetType = Other::class) class Mismatch : Family
    @DerivedType("explicit", targetType = Family::class) class Explicit : Family, Other
    @DerivedType("none") class NoTarget
    @DerivedType(" ") class Blank : Family
    @DerivedType("interface") interface InvalidInterface : Family
    @DerivedType("abstract") abstract class InvalidAbstract : Family
    @DerivedType("enum") enum class InvalidEnum : Family { VALUE }
    @DerivedType("concept") class InvalidConcept : ConceptAs<String> {
        override fun value(): String = "x"
    }
    class Unannotated : Family
    @DerivedType("ancestor") open class Annotated : Family
    class Inherited : Annotated()
    open class SealedBase
    @DerivedType("sealed") class SealedChild : SealedBase()

    @Test
    fun `indexes primary target and every non JDK base class`() {
        val registry = DerivedTypes.of(Known::class.java, Known::class.java)
        assertEquals(Family::class.java, registry.targetTypeFor(Known::class.java))
        for (target in listOf(Family::class.java, Base::class.java)) {
            assertEquals(Known::class.java, registry.derivedTypeFor(target, DerivedTypeId("known")))
            assertTrue(registry.hasDerivatives(target))
        }
        assertEquals(setOf(Family::class.java, Base::class.java), registry.typesWithDerivatives)
        assertTrue(registry.isDerivedType(Known::class.java))
        assertFalse(registry.isDerivedType(Base::class.java))
        assertFalse(registry.hasDerivatives(Other::class.java))
        assertThrows(UnsupportedOperationException::class.java) {
            (registry.typesWithDerivatives as MutableSet).clear()
        }
    }

    @Test
    fun `rejects global duplicate ids and ambiguous transitive interfaces`() {
        rejects("ambiguous derived type identifier", Known::class.java, Duplicate::class.java)
        rejects("ambiguous target type", Ambiguous::class.java)
        rejects("ambiguous target type", Transitive::class.java)
        rejects("target type mismatch", Mismatch::class.java)
        rejects("missing target type", NoTarget::class.java)
        assertEquals(Family::class.java, DerivedTypes.of(Explicit::class.java).targetTypeFor(Explicit::class.java))
    }

    @Test
    fun `rejects invalid registrations and does not inherit annotation`() {
        for (type in listOf(Unannotated::class.java, Inherited::class.java, Blank::class.java,
            InvalidInterface::class.java, InvalidAbstract::class.java, InvalidEnum::class.java, InvalidConcept::class.java)) {
            val error = assertThrows(IllegalArgumentException::class.java) { DerivedTypes.of(type) }
            assertTrue(checkNotNull(error.message).contains(type.name))
        }
    }

    @Test
    fun `supports superclass inference and inherited interfaces`() {
        val registry = DerivedTypes.of(listOf(SealedChild::class.java, Known::class.java))
        assertEquals(SealedBase::class.java, registry.targetTypeFor(SealedChild::class.java))
        assertEquals(Family::class.java, registry.targetTypeFor(Known::class.java))
    }

    @Test
    fun `unknown lookups fail and empty has no types`() {
        val registry = DerivedTypes.of(Known::class.java)
        assertThrows(IllegalArgumentException::class.java) { registry.derivedTypeFor(Family::class.java, DerivedTypeId("missing")) }
        assertThrows(IllegalArgumentException::class.java) { registry.targetTypeFor(Unannotated::class.java) }
        assertTrue(DerivedTypes.EMPTY.typesWithDerivatives.isEmpty())
        assertFalse(DerivedTypes.EMPTY.isDerivedType(Known::class.java))
        assertFalse(DerivedTypes.EMPTY.hasDerivatives(Family::class.java))
    }

    @Test
    fun `id has string value equality without validation`() {
        assertEquals(DerivedTypeId("x"), DerivedTypeId("x"))
        assertEquals("x".hashCode(), DerivedTypeId("x").hashCode())
        assertEquals("x", DerivedTypeId("x").toString())
        assertEquals("", DerivedTypeId("").value())
        assertNotEquals(DerivedTypeId("x"), DerivedTypeId("y"))
    }

    private fun rejects(message: String, vararg types: Class<*>) {
        val error = assertThrows(IllegalArgumentException::class.java) { DerivedTypes.of(*types) }
        assertTrue(checkNotNull(error.message).contains(message), error.message)
    }
}
