// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.geospatial.LineString
import io.cratis.fundamentals.geospatial.LinearRing
import io.cratis.fundamentals.geospatial.Point
import io.cratis.fundamentals.geospatial.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper

class GeospatialModuleTest {
    class Holder { var point: Point? = null }
    private val mapper = JsonMapper.builder().addModule(GeospatialModule()).build()
    private val shell = LinearRing(Point(0.5, 0.5), Point(10.5, 0.5), Point(10.5, 10.5), Point(0.5, 0.5))
    private val hole = LinearRing(Point(2.5, 2.5), Point(3.5, 2.5), Point(3.5, 3.5), Point(2.5, 2.5))
    private val expected: Map<String, Any> = mapOf(
        "point" to Point(10.5, 59.25),
        "line_string" to LineString(Point(10.5, 59.25), Point(11.5, 60.75)),
        "polygon" to Polygon(shell),
        "polygon_with_hole" to Polygon(shell, listOf(hole)),
        "whole_numbers" to Point(10.0, 60.0),
        "case_insensitive" to Point(10.5, 59.25)
    )

    @TestFactory
    fun `every golden row is consumed`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/geospatial.json")).use { stream ->
            val rows = mapper.readTree(stream).get("cases").toList()
            val names = rows.map { it.get("name").asString() }
            assertEquals(expected.keys, names.toSet())
            assertEquals(expected.size, names.size)
            rows.map { row ->
                val name = row.get("name").asString()
                DynamicTest.dynamicTest("$name ${row.get("direction").asString()}") {
                    val json = row.get("json").asString()
                    val value = expected.getValue(name)
                    assertEquals(value, mapper.readValue(json, value.javaClass))
                    if (row.get("direction").asString() == "both") assertEquals(json, mapper.writeValueAsString(value))
                }
            }
        }

    private fun rejects(message: String, json: String, type: Class<*>) {
        val exception = assertThrows(DatabindException::class.java) { mapper.readValue(json, type) }
        assertTrue(exception.message!!.startsWith(message), exception.message)
    }

    @Test
    fun `invalid geometries are rejected with the C# messages`() {
        rejects("Expected type to be 'Point'", """{"type":"LineString","coordinates":[1,2]}""", Point::class.java)
        rejects("Expected coordinates array with 2 elements", """{"type":"Point","coordinates":[1,2,3]}""", Point::class.java)
        rejects("Expected start of object", "[1,2]", Point::class.java)
        rejects("Each coordinate must have exactly 2 elements", """{"type":"LineString","coordinates":[[1,2],[3]]}""", LineString::class.java)
        rejects("LineString must have at least 2 points", """{"type":"LineString","coordinates":[[1,2]]}""", LineString::class.java)
        rejects("Polygon must have at least one ring", """{"type":"Polygon","coordinates":[]}""", Polygon::class.java)
        rejects("LinearRing must have at least 4 points", """{"type":"Polygon","coordinates":[[[0,0],[1,0],[0,0]]]}""", Polygon::class.java)
        rejects("LinearRing must be closed", """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[2,2]]]}""", Polygon::class.java)
    }

    @Test
    fun `missing coordinates report each geometry message from C#`() {
        rejects("Expected coordinates array with 2 elements [longitude, latitude]", """{"type":"Point"}""", Point::class.java)
        rejects("LineString must have at least 2 points", """{"type":"LineString"}""", LineString::class.java)
        rejects("Polygon must have at least one ring (shell)", """{"type":"Polygon"}""", Polygon::class.java)
    }

    @Test
    fun `null coordinates report the C# start-of-array message`() {
        for ((type, name) in listOf(Point::class.java to "Point", LineString::class.java to "LineString", Polygon::class.java to "Polygon")) {
            rejects("Expected start of array for coordinates", """{"type":"$name","coordinates":null}""", type)
        }
    }

    @Test
    fun `nulls stay null`() {
        assertNull(mapper.readValue("null", Point::class.java))
        assertNull(mapper.readValue("""{"point":null}""", Holder::class.java).point)
        assertEquals("""{"point":null}""", mapper.writeValueAsString(Holder()))
        assertEquals(Point(1.5, 2.5), mapper.readValue("""{"point":{"type":"Point","coordinates":[1.5,2.5]}}""", Holder::class.java).point)
    }

    @Test
    fun `module is opt-in and not discovered`() {
        val services = checkNotNull(javaClass.getResourceAsStream("/META-INF/services/tools.jackson.databind.JacksonModule")).use { it.readBytes().decodeToString() }
        assertTrue(!services.contains("GeospatialModule"), services)
        val discovered = JsonMapper.builder().findAndAddModules().build()
        assertTrue(!discovered.writeValueAsString(Point(1.5, 2.5)).contains("coordinates"))
    }
}
