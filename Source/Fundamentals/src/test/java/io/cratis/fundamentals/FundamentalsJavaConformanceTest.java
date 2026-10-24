// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals;

import io.cratis.fundamentals.concepts.ConceptAs;
import io.cratis.fundamentals.execution.CorrelationId;
import io.cratis.fundamentals.strings.StringExtensions;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FundamentalsJavaConformanceTest {
    record OrderId(UUID value) implements ConceptAs<UUID> {}
    record Count(Integer value) implements ConceptAs<Integer> {}

    @Test
    void publicApiWorksFromJavaRecordsAndStaticCalls() {
        var uuid = UUID.randomUUID();
        assertEquals(uuid, new OrderId(uuid).value());
        assertTrue(ConceptAs.isConcept(OrderId.class));
        assertEquals(Integer.class, ConceptAs.valueTypeOf(Count.class));
        assertEquals(new UUID(0L, 0L), CorrelationId.NOT_SET.value());
        assertNotEquals(CorrelationId.NOT_SET, CorrelationId.create());
        assertEquals(uuid, new CorrelationId(uuid).value());
        assertEquals("person", StringExtensions.toCamelCase("Person"));
        assertEquals("Person", StringExtensions.toPascalCase("person"));
    }
}
