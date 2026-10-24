// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.execution;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class CorrelationIdJavaTest {
    @Test
    void staticFactoriesAndConstructorAreJavaFriendly() {
        var value = UUID.randomUUID();
        assertEquals(value, new CorrelationId(value).value());
        assertEquals(new UUID(0L, 0L), CorrelationId.NOT_SET.value());
        assertNotEquals(CorrelationId.NOT_SET, CorrelationId.create());
        assertThrows(NullPointerException.class, () -> new CorrelationId(null));
    }
}
