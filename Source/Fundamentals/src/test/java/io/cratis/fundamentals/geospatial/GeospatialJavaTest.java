// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.geospatial;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeospatialJavaTest {
    private final Point a = new Point(10.0, 59.9);
    private final Point b = new Point(11.0, 60.0);
    private final Point c = new Point(12.0, 61.0);

    @Test
    void pointExposesGetters() {
        assertEquals(10.0, a.getLongitude());
        assertEquals(59.9, a.getLatitude());
    }

    @Test
    void varargAndListConstructorsAreEqual() {
        assertEquals(new LineString(a, b), new LineString(List.of(a, b)));
        assertEquals(new LinearRing(a, b, c, a), new LinearRing(List.of(a, b, c, a)));
    }

    @Test
    void polygonOverloadsSupportOptionalHoles() {
        var ring = new LinearRing(a, b, c, a);
        var hole = new LinearRing(a, c, b, a);
        assertTrue(new Polygon(ring).getHoles().isEmpty());
        assertEquals(List.of(hole), new Polygon(ring, List.of(hole)).getHoles());
        assertEquals(ring, new Polygon(ring).getShell());
    }
}
