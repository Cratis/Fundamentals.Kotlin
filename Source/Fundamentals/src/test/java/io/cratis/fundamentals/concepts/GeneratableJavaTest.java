// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts;

import io.cratis.fundamentals.execution.CorrelationId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class GeneratableJavaTest {
    record SomeId(UUID value) implements ConceptAs<UUID>, Generatable<SomeId> {
        public static SomeId create() { return new SomeId(GenerateValue.uuid()); }
    }

    record OrderId(String value) implements ConceptAs<String>, GeneratableInContext<OrderId> {
        public static OrderId create(GenerationContext context) {
            return new OrderId(GenerateValue.typeId("order", context));
        }
    }

    @Test
    void recordsFollowTheStaticFactoryConvention() {
        Generatable<SomeId> id = SomeId.create();
        assertInstanceOf(SomeId.class, id);
        assertInstanceOf(Generatable.class, CorrelationId.create());
        GeneratableInContext<OrderId> order = OrderId.create(new GenerationContext(Instant.EPOCH));
        assertTrue(((OrderId) order).value().startsWith("order_00000000"));
        assertEquals(String.class, ConceptAs.valueTypeOf(OrderId.class));
    }
}
