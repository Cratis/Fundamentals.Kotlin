// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Pins what Evo Inflector actually produces for the nouns most likely to appear as read-model
 * class names. A library upgrade that silently changes a plural would move a collection name and
 * cause queries to return empty results; these tests fail instead.
 *
 * Every expectation was measured by running both libraries over the same word list. Evo Inflector
 * and the kernel's Humanizer agreed on Child, Status, Series, Category, Entry, Task, Mouse and
 * Datum, and disagreed on two: Evo produced `Persons` and `Indexes` where Humanizer produced
 * `People` and `Indices`. The policy corrects those two toward the kernel.
 */
class DefaultNamingPolicyTest {
    private class Person
    private class Child
    private class Index
    private class Status
    private class Series
    private class Category
    private class Entry
    private class Task

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pluralCases")
    fun `pluralises read model names`(type: Class<*>, expected: String) {
        assertEquals(expected, DefaultNamingPolicy().getReadModelName(type))
    }

    @Test
    fun `returns the simple class name unchanged when pluralizeReadModelNames is false`() {
        val policy = DefaultNamingPolicy(pluralizeReadModelNames = false)
        assertEquals("Person", policy.getReadModelName(Person::class.java))
        assertEquals("Category", policy.getReadModelName(Category::class.java))
        assertEquals("Task", policy.getReadModelName(Task::class.java))
    }

    @Test
    fun `getPropertyName returns the declared member name unchanged`() {
        val policy = DefaultNamingPolicy()
        assertEquals("firstName", policy.getPropertyName("firstName"))
        assertEquals("_id", policy.getPropertyName("_id"))
        assertEquals("some_field", policy.getPropertyName("some_field"))
    }

    @Test
    fun `getPropertyName is the same with pluralizeReadModelNames false`() {
        assertEquals("title", DefaultNamingPolicy(pluralizeReadModelNames = false).getPropertyName("title"))
    }

    @Test
    fun `a lambda style implementation keeps the default getPropertyName`() {
        val policy = object : NamingPolicy {
            override fun getReadModelName(readModelType: Class<*>): String = "x"
        }
        assertEquals("Name", policy.getPropertyName("Name"))
    }

    companion object {
        @JvmStatic
        fun pluralCases(): List<Arguments> = listOf(
            Arguments.of(Person::class.java, "People"),
            Arguments.of(Child::class.java, "Children"),
            Arguments.of(Index::class.java, "Indices"),
            Arguments.of(Status::class.java, "Statuses"),
            Arguments.of(Series::class.java, "Series"),
            Arguments.of(Category::class.java, "Categories"),
            Arguments.of(Entry::class.java, "Entries"),
            Arguments.of(Task::class.java, "Tasks")
        )
    }
}
