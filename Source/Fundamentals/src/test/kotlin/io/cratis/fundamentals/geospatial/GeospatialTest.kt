// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.geospatial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeospatialTest {
    private val a = Point(10.0, 59.9)
    private val b = Point(11.0, 60.0)
    private val c = Point(12.0, 61.0)
    private val ring = LinearRing(a, b, c, a)

    @Test
    fun `points compare by value`() {
        assertEquals(Point(10.0, 59.9), a)
        assertEquals(Point(10.0, 59.9).hashCode(), a.hashCode())
        assertNotEquals(Point(59.9, 10.0), a)
    }

    @Test
    fun `vararg and list constructors are equal`() {
        assertEquals(LineString(listOf(a, b)), LineString(a, b))
        assertEquals(LinearRing(listOf(a, b, c, a)), ring)
        assertEquals(LineString(a, b).hashCode(), LineString(listOf(a, b)).hashCode())
    }

    @Test
    fun `polygon without holes has empty holes`() {
        assertTrue(Polygon(ring).holes.isEmpty())
        assertEquals(Polygon(ring, emptyList()), Polygon(ring))
        assertEquals(listOf(ring), Polygon(ring, listOf(ring)).holes)
    }
}
