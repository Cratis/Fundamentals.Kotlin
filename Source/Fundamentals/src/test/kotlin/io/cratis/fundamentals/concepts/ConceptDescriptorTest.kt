// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ConceptDescriptorTest {
    private data class Text(private val raw: String) : ConceptAs<String> {
        override fun value(): String = raw
    }
    private class Preferred private constructor(private val raw: String, val path: String) : ConceptAs<String> {
        constructor(raw: String) : this(raw, "constructor")
        override fun value(): String = raw
        companion object {
            @JvmStatic fun of(raw: String): Preferred = Preferred(raw, "factory")
        }
    }
    private class FactoryOnly private constructor(private val raw: String) : ConceptAs<String> {
        override fun value(): String = raw
        companion object {
            @JvmStatic fun of(raw: String): FactoryOnly = FactoryOnly(raw)
        }
    }
    private open class LongConcept(private val raw: Long) : ConceptAs<Long> {
        override fun value(): Long = raw
    }
    private open class Inherited(raw: Long) : LongConcept(raw)
    private class MultiLevel(raw: Long) : Inherited(raw)
    private class Missing private constructor() : ConceptAs<String> {
        override fun value(): String = "missing"
    }
    private class Validated(private val raw: Int) : ConceptAs<Int> {
        init { if (raw < 0) throw validationFailure }
        override fun value(): Int = raw
    }

    @ParameterizedTest(name = "ConceptMap / ConceptFactory inheritance: {0}")
    @MethodSource("inheritedTypes")
    fun `value type and construction resolve through inheritance`(type: Class<out ConceptAs<Long>>) {
        val descriptor = ConceptDescriptor.of(type)
        assertEquals(Long::class.javaObjectType, descriptor.valueType)
        assertEquals(42L, descriptor.create("42").value())
    }

    @Test
    fun `descriptors cache construction and extract the underlying value`() {
        val descriptor = ConceptDescriptor.of(Text::class.java)
        assertSame(descriptor, ConceptDescriptor.of(Text::class.java))
        assertEquals(Text::class.java, descriptor.conceptType)
        assertEquals("text", descriptor.valueOf(descriptor.create("text")))
        assertEquals("factory", ConceptDescriptor.of(Preferred::class.java).create("text").path)
        assertEquals("text", ConceptDescriptor.of(FactoryOnly::class.java).create("text").value())
    }

    @Test
    fun `non concepts and missing shapes fail eagerly with useful diagnostics`() {
        assertThrows(IllegalArgumentException::class.java) { ConceptDescriptor.of(String::class.java) }
        val error = assertThrows(IllegalArgumentException::class.java) { ConceptDescriptor.of(Missing::class.java) }
        for (text in listOf(Missing::class.java.name, "record", "static of", "single-arg constructor")) {
            assertTrue(checkNotNull(error.message).contains(text), error.message)
        }
    }

    @Test
    fun `domain validation propagates the original exception`() {
        assertSame(validationFailure, assertThrows(IllegalStateException::class.java) {
            ConceptFactory.create(Validated::class.java, -1)
        })
    }

    companion object {
        private val validationFailure = IllegalStateException("negative value")
        @JvmStatic
        fun inheritedTypes(): List<Class<out ConceptAs<Long>>> =
            listOf(LongConcept::class.java, Inherited::class.java, MultiLevel::class.java)
    }
}
