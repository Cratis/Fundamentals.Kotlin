// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DerivedTypesJavaTest {
    interface Shape {}
    @DerivedType("x") record Circle(double radius) implements Shape {}
    @DerivedType(value = "y", targetType = Shape.class) record Square(double side) implements Shape {}

    @Test
    void recordsAndStaticFactoriesWorkFromJava() {
        var registry = DerivedTypes.of(Circle.class, Square.class);
        var iterable = DerivedTypes.of(List.of(Circle.class, Square.class));
        assertEquals("x", new DerivedTypeId("x").value());
        assertEquals(Circle.class, registry.derivedTypeFor(Shape.class, new DerivedTypeId("x")));
        assertEquals(Shape.class, registry.targetTypeFor(Square.class));
        assertTrue(registry.isDerivedType(Circle.class));
        assertTrue(registry.hasDerivatives(Shape.class));
        assertEquals(registry.getTypesWithDerivatives(), iterable.getTypesWithDerivatives());
        assertTrue(DerivedTypes.EMPTY.getTypesWithDerivatives().isEmpty());
    }
}
