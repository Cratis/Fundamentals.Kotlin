// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.geospatial

/**
 * A geographic point. Longitude comes first, matching the GeoJSON coordinate order.
 * Mirrors Geospatial/Point.cs@f87af602.
 *
 * @property longitude The longitude of the point.
 * @property latitude The latitude of the point.
 */
public data class Point(public val longitude: Double, public val latitude: Double)
