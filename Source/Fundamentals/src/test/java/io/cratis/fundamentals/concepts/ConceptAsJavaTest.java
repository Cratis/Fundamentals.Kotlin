// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConceptAsJavaTest {
    record OrderId(UUID value) implements ConceptAs<UUID> {}

    @Test
    void recordImplementsValueWithoutBoilerplate() {
        var value = UUID.randomUUID();
        ConceptAs<UUID> concept = new OrderId(value);
        assertEquals(value, concept.value());
    }

    @Test
    void contractHasExactlyOneAbstractMethodAndSyntheticPropertyAccessor() throws Exception {
        var abstractMethods = Arrays.stream(ConceptAs.class.getDeclaredMethods())
            .filter(method -> Modifier.isAbstract(method.getModifiers())).toList();
        assertEquals(1, abstractMethods.size());
        assertEquals("value", abstractMethods.get(0).getName());
        assertFalse(abstractMethods.get(0).isDefault());
        Method accessor = Class.forName("io.cratis.fundamentals.concepts.ConceptAsKt")
            .getDeclaredMethod("getValue", ConceptAs.class);
        assertTrue(accessor.isSynthetic());
    }
}
