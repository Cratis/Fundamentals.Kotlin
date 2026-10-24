// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ConceptFactoryJavaTest {
    record OrderId(UUID value) implements ConceptAs<UUID> {}
    record Count(Integer value) implements ConceptAs<Integer> {
        public static Count of(Integer value) { return new Count(value + 1); }
    }
    static final class Sku implements ConceptAs<String> {
        private final String value;
        private Sku(String value) { this.value = value; }
        public static Sku of(String value) { return new Sku(value); }
        @Override public String value() { return value; }
    }

    @Test
    void descriptorsExposeTypedCreationAndExtraction() {
        var descriptor = ConceptDescriptor.of(OrderId.class);
        var uuid = UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff");
        var id = descriptor.create(uuid.toString());
        assertEquals(OrderId.class, descriptor.getConceptType());
        assertEquals(UUID.class, descriptor.getValueType());
        assertEquals(new OrderId(uuid), id);
        assertEquals(uuid, descriptor.valueOf(id));
    }

    @Test
    void recordCanonicalConstructorPrecedesStaticFactoryAndPrivateConstructorsUseOf() {
        assertEquals(new Count(42), ConceptFactory.create(Count.class, "42"));
        assertEquals("sku", ConceptFactory.create(Sku.class, "sku").value());
    }

    @Test
    void nullInputMapsArgumentNullExceptionToNullPointerException() {
        assertThrows(NullPointerException.class, () -> ConceptFactory.create(Count.class, null));
    }
}
