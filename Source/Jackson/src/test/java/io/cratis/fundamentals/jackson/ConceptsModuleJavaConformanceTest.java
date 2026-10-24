// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson;

import io.cratis.fundamentals.concepts.ConceptAs;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConceptsModuleJavaConformanceTest {
    record OrderId(UUID value) implements ConceptAs<UUID> {}
    record Quantity(Integer value) implements ConceptAs<Integer> {}
    record Order(OrderId id, Quantity quantity) {}

    static final class Sku implements ConceptAs<String> {
        private final String value;

        private Sku(String value) {
            this.value = value;
        }

        public static Sku of(String value) {
            return new Sku(value);
        }

        @Override
        public String value() {
            return value;
        }
    }

    // The canonical constructor must precede a compatible public static factory.
    record Preferred(Integer value) implements ConceptAs<Integer> {
        public static Preferred of(Integer value) {
            return new Preferred(value + 1);
        }
    }

    private final JsonMapper mapper = JsonMapper.builder().addModule(new ConceptsModule()).build();
    private final UUID uuid = UUID.fromString("6f9619ff-8b86-d011-b42d-00c04fc964ff");

    @Test
    void recordsRoundTripAsScalarProperties() {
        var order = new Order(new OrderId(uuid), new Quantity(3));
        var json = "{\"id\":\"6f9619ff-8b86-d011-b42d-00c04fc964ff\",\"quantity\":3}";
        assertEquals(json, mapper.writeValueAsString(order));
        assertEquals(order, mapper.readValue(json, Order.class));
    }

    @Test
    void nullRecordPropertiesRemainNull() {
        var order = new Order(null, null);
        var json = "{\"id\":null,\"quantity\":null}";
        assertEquals(json, mapper.writeValueAsString(order));
        assertEquals(order, mapper.readValue(json, Order.class));
    }

    @Test
    void staticFactoryBuildsAClassWithAPrivateConstructor() {
        assertEquals("sku", mapper.readValue("\"sku\"", Sku.class).value());
        assertEquals("\"sku\"", mapper.writeValueAsString(Sku.of("sku")));
    }

    @Test
    void recordCanonicalConstructorPrecedesStaticFactory() {
        assertEquals(new Preferred(42), mapper.readValue("42", Preferred.class));
    }

    @Test
    void conceptMapKeysRoundTripWithoutEmbeddedJsonQuotes() {
        Map<OrderId, Integer> values = Map.of(new OrderId(uuid), 1);
        var json = "{\"6f9619ff-8b86-d011-b42d-00c04fc964ff\":1}";
        assertEquals(json, mapper.writeValueAsString(values));
        assertEquals(values, mapper.readValue(json, new TypeReference<Map<OrderId, Integer>>() {}));
    }
}
