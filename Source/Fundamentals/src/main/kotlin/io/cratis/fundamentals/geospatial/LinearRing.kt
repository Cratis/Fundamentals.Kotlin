// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.geospatial

/**
 * A closed ring of points. Mirrors Geospatial/LinearRing.cs@f87af602; no validation is performed.
 *
 * @property coordinates The points that make up the ring.
 */
public data class LinearRing(public val coordinates: List<Point>) {
    /**
     * Creates a ring from the given points.
     *
     * @param coordinates The points that make up the ring.
     */
    public constructor(vararg coordinates: Point) : this(coordinates.toList())
}
