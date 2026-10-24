// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.strings;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StringExtensionsJavaTest {
    @Test
    void casingHasStaticJavaEntryPoints() {
        assertEquals("person", StringExtensions.toCamelCase("Person"));
        assertEquals("Person", StringExtensions.toPascalCase("person"));
        assertEquals("URLValue", StringExtensions.toCamelCase("URLValue"));
        assertThrows(NullPointerException.class, () -> StringExtensions.toCamelCase(null));
        assertThrows(NullPointerException.class, () -> StringExtensions.toPascalCase(null));
    }
}
