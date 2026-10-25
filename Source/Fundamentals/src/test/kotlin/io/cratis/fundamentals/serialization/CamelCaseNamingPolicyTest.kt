// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CamelCaseNamingPolicyTest {
    private class Person
    private class Task
    private class IPAddress

    @Test
    fun `pluralises and camel cases read model names`() {
        val policy = CamelCaseNamingPolicy()
        assertEquals("people", policy.getReadModelName(Person::class.java))
        assertEquals("tasks", policy.getReadModelName(Task::class.java))
    }

    @Test
    fun `camel cases the simple name when pluralizeReadModelNames is false`() {
        val policy = CamelCaseNamingPolicy(pluralizeReadModelNames = false)
        assertEquals("person", policy.getReadModelName(Person::class.java))
        assertEquals("task", policy.getReadModelName(Task::class.java))
    }

    @Test
    fun `preserves leading acronyms in read model names`() {
        assertEquals("IPAddress", CamelCaseNamingPolicy(false).getReadModelName(IPAddress::class.java))
    }

    @Test
    fun `camel cases property names`() {
        val policy = CamelCaseNamingPolicy()
        assertEquals("firstName", policy.getPropertyName("FirstName"))
        assertEquals("isCIA", policy.getPropertyName("IsCIA"))
        assertEquals("URLValue", policy.getPropertyName("URLValue"))
    }
}
