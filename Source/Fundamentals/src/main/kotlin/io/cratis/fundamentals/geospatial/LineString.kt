// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.geospatial

/**
 * A line string composed of points. Mirrors Geospatial/LineString.cs@f87af602; no validation is performed.
 *
 * @property coordinates The points that make up the line string.
 */
public data class LineString(public val coordinates: List<Point>) {
    /**
     * Creates a line string from the given points.
     *
     * @param coordinates The points that make up the line string.
     */
    public constructor(vararg coordinates: Point) : this(coordinates.toList())
}
