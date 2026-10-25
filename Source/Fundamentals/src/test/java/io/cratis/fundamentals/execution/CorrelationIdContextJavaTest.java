// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class CorrelationIdContextJavaTest {
    @AfterEach
    void cleanup() {
        CorrelationIdContext.shared().clear();
    }

    @Test
    void sharedInstanceIsJavaCallableAndImplementsBothInterfaces() {
        CorrelationIdAccessor accessor = CorrelationIdContext.shared();
        CorrelationIdModifier modifier = CorrelationIdContext.shared();
        var id = new CorrelationId(UUID.randomUUID());
        modifier.set(id);
        assertEquals(id, accessor.getCurrent());
        modifier.clear();
        assertNotEquals(id, accessor.getCurrent());
        assertSame(CorrelationIdContext.shared(), CorrelationIdContext.shared());
    }

    @Test
    void constructedInstancesAreIsolated() {
        var context = new CorrelationIdContext();
        var id = new CorrelationId(UUID.randomUUID());
        context.set(id);
        assertEquals(id, context.getCurrent());
        assertNotEquals(id, new CorrelationIdContext().getCurrent());
        assertNotEquals(id, CorrelationIdContext.shared().getCurrent());
    }
}
