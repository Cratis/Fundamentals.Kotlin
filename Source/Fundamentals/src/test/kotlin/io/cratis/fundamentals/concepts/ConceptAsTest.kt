// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConceptAsTest {
    private data class Name(private val underlying: String) : ConceptAs<String> {
        override fun value(): String = underlying
    }

    private abstract class Base<T>(private val underlying: T) : ConceptAs<T> {
        override fun value(): T = underlying
    }
    private class GenericName : Base<String>("Ada")
    private class Id : Base<UUID>(UUID.randomUUID())
    private class Count : ConceptAs<Int> {
        override fun value(): Int = 42
    }
    private class Names : Base<List<String>>(emptyList())

    @Test
    fun `concept detection excludes unrelated classes and the interface itself`() {
        assertTrue(ConceptAs.isConcept(Name::class.java))
        assertTrue(ConceptAs.isConcept(GenericName::class.java))
        assertFalse(ConceptAs.isConcept(String::class.java))
        assertFalse(ConceptAs.isConcept(ConceptAs::class.java))
    }

    @Test
    fun `value types resolve through generic bases and box primitives`() {
        assertEquals(UUID::class.java, ConceptAs.valueTypeOf(Id::class.java))
        assertEquals(String::class.java, ConceptAs.valueTypeOf(GenericName::class.java))
        assertEquals(Int::class.javaObjectType, ConceptAs.valueTypeOf(Count::class.java))
        assertEquals(List::class.java, ConceptAs.valueTypeOf(Names::class.java))
    }

    @Test
    fun `unresolved and non concept types fail naming the class`() {
        for (type in listOf(String::class.java, Base::class.java)) {
            val error = assertThrows(IllegalArgumentException::class.java) { ConceptAs.valueTypeOf(type) }
            assertTrue(checkNotNull(error.message).contains(type.name))
        }
    }

    @Test
    fun `value property returns the underlying value`() {
        val concept = Name("Ada")
        assertEquals(concept.value(), concept.value)
    }
}
