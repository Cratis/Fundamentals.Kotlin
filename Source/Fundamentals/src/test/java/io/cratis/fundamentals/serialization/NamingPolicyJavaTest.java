// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NamingPolicyJavaTest {
    static final class Person {}
    static final class Task {}

    @Test
    void javaLambdaImplementsNamingPolicyAndKeepsDefaultPropertyName() {
        NamingPolicy policy = type -> "custom-" + type.getSimpleName();
        assertEquals("custom-Person", policy.getReadModelName(Person.class));
        assertEquals("Name", policy.getPropertyName("Name"));
    }

    @Test
    void defaultNamingPolicyConstructorsWorkFromJava() {
        assertEquals("People", new DefaultNamingPolicy().getReadModelName(Person.class));
        assertEquals("Tasks", new DefaultNamingPolicy(true).getReadModelName(Task.class));
        assertEquals("Person", new DefaultNamingPolicy(false).getReadModelName(Person.class));
        assertEquals("firstName", new DefaultNamingPolicy().getPropertyName("firstName"));
    }

    @Test
    void camelCaseNamingPolicyConstructorsWorkFromJava() {
        assertEquals("people", new CamelCaseNamingPolicy().getReadModelName(Person.class));
        assertEquals("person", new CamelCaseNamingPolicy(false).getReadModelName(Person.class));
        assertEquals("firstName", new CamelCaseNamingPolicy().getPropertyName("FirstName"));
    }
}
