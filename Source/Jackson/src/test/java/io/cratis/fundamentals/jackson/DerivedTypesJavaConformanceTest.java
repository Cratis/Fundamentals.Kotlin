// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson;

import io.cratis.fundamentals.serialization.DerivedType;
import io.cratis.fundamentals.serialization.DerivedTypes;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class DerivedTypesJavaConformanceTest {
    sealed interface Shape permits Circle, Square {}
    @DerivedType("circle") record Circle(double radius) implements Shape {}
    @DerivedType(value = "square", targetType = Shape.class) record Square(double side) implements Shape {}

    @Test
    void sealedRecordFamiliesRoundTripWithPlainJavaConstruction() {
        var mapper = JsonMapper.builder()
            .addModule(new DerivedTypesModule(DerivedTypes.of(Circle.class, Square.class))).build();
        var type = new TypeReference<List<Shape>>() {};
        List<Shape> shapes = List.of(new Circle(2), new Square(3));
        var json = "[{\"radius\":2.0,\"_derivedTypeId\":\"circle\"},{\"side\":3.0,\"_derivedTypeId\":\"square\"}]";
        assertEquals(json, mapper.writeValueAsString(shapes));
        assertEquals(shapes, mapper.readValue(json, type));
        assertEquals("_derivedTypeId", DerivedTypesModule.DERIVED_TYPE_ID_PROPERTY);
    }
}
