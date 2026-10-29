// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson;

import io.cratis.fundamentals.geospatial.LinearRing;
import io.cratis.fundamentals.geospatial.Point;
import io.cratis.fundamentals.geospatial.Polygon;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class GeospatialModuleJavaConformanceTest {
    record Place(Point location, Polygon area) {}

    @Test
    void recordWithPointAndPolygonRoundTrips() {
        var mapper = JsonMapper.builder().addModule(new GeospatialModule()).build();
        var ring = new LinearRing(new Point(0.5, 0.5), new Point(10.5, 0.5), new Point(10.5, 10.5), new Point(0.5, 0.5));
        var value = new Place(new Point(10.5, 59.25), new Polygon(ring));
        var json = "{\"location\":{\"type\":\"Point\",\"coordinates\":[10.5,59.25]},"
            + "\"area\":{\"type\":\"Polygon\",\"coordinates\":[[[0.5,0.5],[10.5,0.5],[10.5,10.5],[0.5,0.5]]]}}";
        assertEquals(json, mapper.writeValueAsString(value));
        assertEquals(value, mapper.readValue(json, Place.class));
    }
}
