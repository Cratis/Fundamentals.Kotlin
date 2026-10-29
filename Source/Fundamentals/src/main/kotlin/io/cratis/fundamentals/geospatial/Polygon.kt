// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.geospatial

/**
 * A polygon with an outer shell and optional holes. Mirrors Geospatial/Polygon.cs@f87af602.
 *
 * @property shell The outer boundary of the polygon.
 * @property holes Inner boundaries within the polygon; empty when solid.
 */
public data class Polygon @JvmOverloads public constructor(
    public val shell: LinearRing,
    public val holes: List<LinearRing> = emptyList()
)
