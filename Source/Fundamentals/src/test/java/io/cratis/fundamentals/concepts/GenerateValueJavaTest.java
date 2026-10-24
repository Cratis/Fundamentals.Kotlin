// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class GenerateValueJavaTest {
    @Test
    void everyGeneratorOverloadIsStaticAndJavaFriendly() {
        var context = new GenerationContext(Instant.EPOCH, "tenant-a");
        assertEquals(Instant.EPOCH, context.getNow());
        assertEquals("tenant-a", context.getTenant());
        assertEquals(GenerationContext.DEFAULT_TENANT, new GenerationContext(Instant.EPOCH).getTenant());
        assertEquals(context, new GenerationContext(Instant.EPOCH, "tenant-a"));
        assertEquals(context.hashCode(), new GenerationContext(Instant.EPOCH, "tenant-a").hashCode());
        assertTrue(context.toString().contains("tenant-a"));
        assertEquals(4, GenerateValue.uuid().version());
        assertEquals(7, GenerateValue.uuidV7().version());
        assertEquals(7, GenerateValue.uuidV7(context).version());
        assertEquals(26, GenerateValue.ulid().length());
        assertTrue(GenerateValue.ulid(context).startsWith("00000000"));
        assertTrue(GenerateValue.typeId("order").startsWith("order_"));
        assertTrue(GenerateValue.typeId("order", context).startsWith("order_00000000"));
        assertEquals(21, GenerateValue.nanoId().length());
        assertEquals(12, GenerateValue.nanoId(12).length());
        assertTrue(GenerateValue.nanoId(12, "ab").matches("[ab]{12}"));
        assertEquals(2, GenerateValue.bytes(1).length());
        for (var encoding : ValueEncoding.values()) {
            assertNotNull(GenerateValue.bytes(1, ValueEncoding.valueOf(encoding.name())));
        }
        assertEquals(1L, GenerateValue.integer(1L, 1L));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new GenerationContext(null, "t"));
        assertThrows(NullPointerException.class, () -> new GenerationContext(Instant.EPOCH, null));
        assertThrows(NullPointerException.class, () -> GenerateValue.typeId(null));
        assertThrows(NullPointerException.class, () -> GenerateValue.bytes(1, null));
    }
}
