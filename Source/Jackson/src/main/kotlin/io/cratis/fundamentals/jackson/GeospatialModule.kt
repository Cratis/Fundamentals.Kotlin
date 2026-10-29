// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.geospatial.LineString
import io.cratis.fundamentals.geospatial.LinearRing
import io.cratis.fundamentals.geospatial.Point
import io.cratis.fundamentals.geospatial.Polygon
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

/**
 * Opt-in GeoJSON (`{"type":…,"coordinates":…}`) for [Point], [LineString] and [Polygon].
 * Based on Json/{Point,LineString,Polygon}JsonConverter.cs@f87af602, which C# registers globally in
 * Json/Globals.cs:63-65; here the module is explicit and never ServiceLoader-discovered.
 * Property names are read case-insensitively and unknown members are skipped. Validation messages
 * follow the C# converters. Unlike .NET, whole doubles are written as `1.0`. A [LinearRing] has no
 * standalone converter, as in C#; it is only read and written inside a [Polygon].
 */
public class GeospatialModule : SimpleModule("Cratis.Fundamentals.Geospatial") {
    init {
        addSerializer(Point::class.java, GeoJsonSerializer("Point") { generator, value -> GeoJsonWireFormat.writePoint(generator, value) })
        addSerializer(LineString::class.java, GeoJsonSerializer("LineString") { generator, value ->
            GeoJsonWireFormat.writePoints(generator, value.coordinates)
        })
        addSerializer(Polygon::class.java, GeoJsonSerializer("Polygon") { generator, value ->
            generator.writeStartArray()
            (listOf(value.shell) + value.holes).forEach { GeoJsonWireFormat.writePoints(generator, it.coordinates) }
            generator.writeEndArray()
        })
        addDeserializer(Point::class.java, GeoJsonDeserializer(Point::class.java, "Point", GeoJsonWireFormat::readPointGeometry))
        addDeserializer(LineString::class.java, GeoJsonDeserializer(LineString::class.java, "LineString", GeoJsonWireFormat::readLineString))
        addDeserializer(Polygon::class.java, GeoJsonDeserializer(Polygon::class.java, "Polygon", GeoJsonWireFormat::readPolygon))
    }
}

private class GeoJsonSerializer<T : Any>(
    private val type: String,
    private val coordinates: (JsonGenerator, T) -> Unit
) : ValueSerializer<T>() {
    override fun serialize(value: T, generator: JsonGenerator, context: SerializationContext) {
        generator.writeStartObject()
        generator.writeStringProperty("type", type)
        generator.writeName("coordinates")
        coordinates(generator, value)
        generator.writeEndObject()
    }
}

private class GeoJsonDeserializer<T : Any>(
    private val target: Class<T>,
    private val type: String,
    private val read: (JsonNode) -> T
) : ValueDeserializer<T>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
        val node = context.readTree(parser)
        return try {
            if (!node.isObject) throw GeoJsonException("Expected start of object")
            var declaredType: String? = null
            var coordinates: JsonNode? = null
            node.properties().forEach { (name, value) ->
                when {
                    name.equals("type", ignoreCase = true) -> declaredType = if (value.isString) value.asString() else null
                    name.equals("coordinates", ignoreCase = true) -> coordinates = value
                }
            }
            if (declaredType != type) throw GeoJsonException("Expected type to be '$type'")
            read(coordinates ?: throw GeoJsonException("Expected coordinates array for the $type"))
        } catch (exception: GeoJsonException) {
            target.cast(context.reportInputMismatch(target, exception.message ?: "Invalid GeoJSON"))
        }
    }
}

internal class GeoJsonException(message: String) : RuntimeException(message)

internal object GeoJsonWireFormat {
    private const val EPSILON = 1e-9

    fun writePoint(generator: JsonGenerator, point: Point) {
        generator.writeStartArray()
        generator.writeNumber(point.longitude)
        generator.writeNumber(point.latitude)
        generator.writeEndArray()
    }

    fun writePoints(generator: JsonGenerator, points: List<Point>) {
        generator.writeStartArray()
        points.forEach { writePoint(generator, it) }
        generator.writeEndArray()
    }

    fun readPointGeometry(node: JsonNode): Point {
        val numbers = numbers(node)
        if (numbers.size != 2) throw GeoJsonException("Expected coordinates array with 2 elements [longitude, latitude]")
        return Point(numbers[0], numbers[1])
    }

    fun readLineString(node: JsonNode): LineString {
        val points = points(node)
        if (points.size < 2) throw GeoJsonException("LineString must have at least 2 points")
        return LineString(points)
    }

    fun readPolygon(node: JsonNode): Polygon {
        val rings = array(node).filter { it.isArray }.map(::readRing)
        if (rings.isEmpty()) throw GeoJsonException("Polygon must have at least one ring (shell)")
        return Polygon(rings.first(), rings.drop(1))
    }

    private fun readRing(node: JsonNode): LinearRing {
        val points = points(node)
        if (points.size < 4) throw GeoJsonException("LinearRing must have at least 4 points")
        val first = points.first()
        val last = points.last()
        if (Math.abs(first.longitude - last.longitude) > EPSILON || Math.abs(first.latitude - last.latitude) > EPSILON) {
            throw GeoJsonException("LinearRing must be closed (first and last points must be identical)")
        }
        return LinearRing(points)
    }

    private fun points(node: JsonNode): List<Point> = array(node).filter { it.isArray }.map { pair ->
        val numbers = numbers(pair)
        if (numbers.size != 2) throw GeoJsonException("Each coordinate must have exactly 2 elements [longitude, latitude]")
        Point(numbers[0], numbers[1])
    }

    private fun numbers(node: JsonNode): List<Double> = array(node).filter { it.isNumber }.map { it.doubleValue() }

    private fun array(node: JsonNode): List<JsonNode> {
        if (!node.isArray) throw GeoJsonException("Expected start of array for coordinates")
        return node.values().toList()
    }
}
