// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.Nulls
import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.serialization.DerivedType
import io.cratis.fundamentals.serialization.DerivedTypes
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

class DerivedTypesModuleTest {
    interface ITargetType
    interface IParent
    interface IChild
    interface INode
    @DerivedType("my-derived-type")
    data class Known @JsonCreator constructor(@param:JsonProperty("someValue") val someValue: Int) : ITargetType
    @DerivedType("parent")
    data class Parent @JsonCreator constructor(@param:JsonProperty("child") val child: IChild?) : IParent
    @DerivedType("child")
    data class Child @JsonCreator constructor(@param:JsonProperty("someValue") val someValue: String) : IChild
    @DerivedType("node")
    data class Node @JsonCreator constructor(
        @param:JsonProperty("name") val name: String, @param:JsonProperty("child") val child: INode?
    ) : INode
    @DerivedType("parent")
    data class ListParent @JsonCreator constructor(@param:JsonProperty("children") val children: List<IChild>) : IParent
    @DerivedType("parent")
    class ArrayParent @JsonCreator constructor(@param:JsonProperty("children") val children: Array<IChild>) : IParent {
        override fun equals(other: Any?): Boolean = other is ArrayParent && children.contentEquals(other.children)
        override fun hashCode(): Int = children.contentHashCode()
    }
    @DerivedType("parent")
    data class MapParent @JsonCreator constructor(@param:JsonProperty("children") val children: Map<String, IChild>) : IParent
    @DerivedType("parent")
    data class MapOfListsParent @JsonCreator constructor(
        @param:JsonProperty("children") val children: Map<String, List<IChild>>
    ) : IParent
    @DerivedType("parent")
    data class NestedParent @JsonCreator constructor(@param:JsonProperty("children") val children: List<List<IChild>>) : IParent

    abstract class Base
    @DerivedType("base") open class Middle : Base() { var name: String = "middle" }
    @DerivedType("leaf") class Leaf : Middle()
    @DerivedType("unregistered") class Unregistered : ITargetType { val value = 1 }
    class Plain : ITargetType { val value = 1 }
    data class Name(private val text: String) : ConceptAs<String> { override fun value(): String = text }
    enum class Rank { ONE, TWO }
    @DerivedType("combined")
    data class Combined(val name: Name, val rank: Rank, val span: Duration) : ITargetType
    @DerivedType("annotations")
    class Annotated : ITargetType {
        @get:JsonIgnore val ignored = "secret"
        @get:JsonProperty("renamed") val original = "visible"
    }
    interface GenericFamily<T>
    @DerivedType("generic")
    data class GenericLeaf<T> @JsonCreator constructor(@param:JsonProperty("payload") val payload: T) : GenericFamily<T>
    @DerivedType("fixed")
    data class FixedLeaf<U> @JsonCreator constructor(
        @param:JsonProperty("payload") val payload: String, @param:JsonProperty("extra") val extra: U?
    ) : GenericFamily<String>
    class ContextualHolder {
        @JsonIgnoreProperties("leftOnly") var left: GenericFamily<String>? = null
        @JsonIgnoreProperties("rightOnly") var right: GenericFamily<Int>? = null
    }
    @DerivedType("scalar")
    class ScalarBody : ITargetType { @get:JsonValue val text = "scalar" }
    data class DecimalAmount(private val amount: BigDecimal) : ConceptAs<BigDecimal> {
        override fun value(): BigDecimal = amount
    }
    @DerivedType("precise")
    data class Precise @JsonCreator constructor(
        @param:JsonProperty("decimal") val decimal: BigDecimal,
        @param:JsonProperty("concept") val concept: DecimalAmount,
        @param:JsonProperty("integer") val integer: BigInteger
    ) : ITargetType
    @DerivedType("ordinary")
    data class NonDecimal(
        val number: Double, val single: Float, val enabled: Boolean, val text: String, val absent: String?
    ) : ITargetType
    @DerivedType("collision")
    class DiscriminatorBody : ITargetType {
        @get:JsonProperty("_derivedTypeId") val discriminator = "wrong"
        val number = 42
    }
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonSubTypes(JsonSubTypes.Type(value = NativeBody::class, name = "native"))
    interface NativeFamily
    @DerivedType("native")
    data class NativeBody @JsonCreator constructor(@param:JsonProperty("number") val number: Int) : NativeFamily
    class EmptyHolder {
        @JsonSetter(nulls = Nulls.AS_EMPTY) var value: Base? = Middle()
    }

    private val classes = listOf(ITargetType::class.java, IParent::class.java, IChild::class.java, INode::class.java,
        Known::class.java, Parent::class.java, Child::class.java, Node::class.java, ListParent::class.java,
        ArrayParent::class.java, MapParent::class.java, MapOfListsParent::class.java, NestedParent::class.java)
        .associateBy { it.simpleName }
    private val expected = mapOf(
        "known_derived_type" to Known(42), "id_first" to Known(42), "missing_id" to null,
        "differently_cased_id" to null, "json_null" to null, "direct_nested" to Parent(Child("nested")),
        "null_entry" to Parent(null), "recursive" to Node("root", Node("branch", Node("leaf", null))),
        "list_children" to ListParent(listOf(Child("nested"))), "array_children" to ArrayParent(arrayOf(Child("nested"))),
        "map_children" to MapParent(mapOf("nested" to Child("value"))),
        "map_of_lists" to MapOfListsParent(mapOf("nested" to listOf(Child("value")))),
        "nested_lists" to NestedParent(listOf(listOf(Child("nested")))),
        "unknown_id" to null, "non_string_id" to null, "null_id" to null, "array_value" to null
    )

    @TestFactory
    fun `derived types follow every pinned CSharp row`(): List<DynamicTest> =
        checkNotNull(javaClass.getResourceAsStream("/golden/derived-types.json")).use { stream ->
            val rows = JsonMapper.shared().readTree(stream).get("cases").toList()
            assertEquals(expected.keys, rows.map { it.get("name").asString() }.toSet())
            assertEquals(expected.size, rows.size)
            rows.map { row ->
                val name = row.get("name").asString()
                val direction = row.get("direction").asString()
                DynamicTest.dynamicTest("$name $direction ${row.get("source").asString()}") {
                    val registry = DerivedTypes.of(row.get("registers").toList().map { classes.getValue(it.asString()) })
                    val mapper = mapper(registry)
                    val type = classes.getValue(row.get("declared").asString())
                    val json = mapper.writeValueAsString(row.get("json"))
                    if (direction == "reject") {
                        assertThrows(DatabindException::class.java) { mapper.readValue(json, type) }
                    } else {
                        assertEquals(expected.getValue(name), mapper.readValue(json, type))
                        if (direction == "both") {
                            val written = mapper.writeValueAsString(expected.getValue(name))
                            assertEquals(row.get("json"), mapper.readTree(written))
                            assertIdLast(mapper.readTree(written))
                        }
                    }
                }
            }
        }

    @Test
    fun `empty registry is byte identical and discovery still only finds concepts`() {
        val graph = Combined(Name("name"), Rank.TWO, Duration.ofSeconds(1))
        val plain = JsonMapper.builder().addModule(ConceptsModule()).addModule(EnumModule()).addModule(TimeSpanModule()).build()
        val empty = JsonMapper.builder().addModule(ConceptsModule()).addModule(EnumModule()).addModule(TimeSpanModule())
            .addModule(DerivedTypesModule(DerivedTypes.EMPTY)).build()
        assertArrayEquals(plain.writeValueAsBytes(graph), empty.writeValueAsBytes(graph))
        val discovered = JsonMapper.builder().findAndAddModules().build()
        val conceptsOnly = JsonMapper.builder().addModule(ConceptsModule()).build()
        assertArrayEquals(conceptsOnly.writeValueAsBytes(graph), discovered.writeValueAsBytes(graph))
        assertEquals(listOf(ConceptsModule::class.java),
            java.util.ServiceLoader.load(tools.jackson.databind.JacksonModule::class.java).map { it.javaClass })
    }

    @Test
    fun `module order does not change concept enum or timespan bodies`() {
        val registry = DerivedTypes.of(Combined::class.java)
        val graph = Combined(Name("name"), Rank.TWO, Duration.ofSeconds(1))
        val modules = listOf(ConceptsModule(), EnumModule(), TimeSpanModule())
        val outputs = (0..modules.size).map { position ->
            val ordered = modules.toMutableList().apply { add(position, DerivedTypesModule(registry)) }
            JsonMapper.builder().addModules(ordered).build().writeValueAsString(graph)
        }
        assertEquals(1, outputs.toSet().size)
        assertEquals("{\"name\":\"name\",\"rank\":1,\"span\":\"00:00:01\",\"_derivedTypeId\":\"combined\"}", outputs.first())
    }

    @Test
    fun `abstract and multilevel targets dispatch without consuming nested ids twice`() {
        val mapper = mapper(DerivedTypes.of(Middle::class.java, Leaf::class.java))
        assertInstanceOf(Middle::class.java, mapper.readValue(mapper.writeValueAsString(Middle()), Base::class.java))
        val leaf = Leaf().apply { name = "leaf" }
        val json = mapper.writeValueAsString(leaf)
        assertEquals("leaf", mapper.readValue(json, Middle::class.java).name)
        assertInstanceOf(Leaf::class.java, mapper.readValue(json, Base::class.java))
        assertInstanceOf(Leaf::class.java, mapper.readValue(json, Middle::class.java))
        assertNull(mapper.readValue("null", Middle::class.java))
        assertNull(mapper.readValue("{\"value\":null}", EmptyHolder::class.java).value)
    }

    @Test
    fun `id is stripped before strict bean delegation and missing ids remain null`() {
        val mapper = JsonMapper.builder().addModule(DerivedTypesModule(DerivedTypes.of(Known::class.java)))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()
        assertEquals(Known(42), mapper.readValue("{\"_derivedTypeId\":\"my-derived-type\",\"someValue\":42}", ITargetType::class.java))
        assertNull(mapper.readValue("{}", ITargetType::class.java))
        assertThrows(DatabindException::class.java) {
            mapper.readValue("{\"_derivedTypeId\":\"my-derived-type\",\"someValue\":42,\"extra\":1}", ITargetType::class.java)
        }
    }

    @Test
    fun `registered concrete and Object declarations write ids but unannotated implementations do not`() {
        val mapper = mapper(DerivedTypes.of(Known::class.java))
        val expected = mapper.readTree("{\"someValue\":42,\"_derivedTypeId\":\"my-derived-type\"}")
        assertEquals(expected, mapper.readTree(mapper.writerFor(Known::class.java).writeValueAsString(Known(42))))
        assertEquals(expected, mapper.readTree(mapper.writerFor(Any::class.java).writeValueAsString(Known(42))))
        assertEquals("{\"value\":1}", mapper.writeValueAsString(Plain()))
        val error = assertThrows(DatabindException::class.java) { mapper.writeValueAsString(Unregistered()) }
        assertTrue(checkNotNull(error.message).contains("unregistered"))
        assertTrue(checkNotNull(error.message).contains(Unregistered::class.java.name))
    }

    @Test
    fun `body annotations and pretty printing follow Jackson`() {
        val mapper = JsonMapper.builder().addModule(DerivedTypesModule(DerivedTypes.of(Annotated::class.java)))
            .enable(SerializationFeature.INDENT_OUTPUT).build()
        val json = mapper.writeValueAsString(Annotated())
        assertTrue(json.contains('\n'))
        assertEquals(mapper.readTree("{\"renamed\":\"visible\",\"_derivedTypeId\":\"annotations\"}"), mapper.readTree(json))
        assertIdLast(mapper.readTree(json))
    }

    @Test
    fun `second module replaces first rather than stacking`() {
        val mapper = JsonMapper.builder().addModule(DerivedTypesModule(DerivedTypes.of(Known::class.java)))
            .addModule(DerivedTypesModule(DerivedTypes.EMPTY)).build()
        assertEquals("{\"someValue\":42}", mapper.writeValueAsString(Known(42)))
        assertEquals("{\"someValue\":42,\"_derivedTypeId\":\"my-derived-type\"}", JsonMapper.builder()
            .addModule(DerivedTypesModule(DerivedTypes.EMPTY))
            .addModule(DerivedTypesModule(DerivedTypes.of(Known::class.java))).build().writeValueAsString(Known(42)))
    }

    @Test
    fun `specialization keeps generic payload bindings`() {
        val mapper = mapper(DerivedTypes.of(GenericLeaf::class.java))
        val type = object : TypeReference<GenericFamily<List<Known>>>() {}
        val result = mapper.readValue("{\"payload\":[{\"someValue\":42}],\"_derivedTypeId\":\"generic\"}", type)
        assertEquals(GenericLeaf(listOf(Known(42))), result)
    }

    @Test
    fun `contextual properties preserve independent generic and annotation bindings`() {
        val mapper = JsonMapper.builder().addModule(DerivedTypesModule(DerivedTypes.of(GenericLeaf::class.java)))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()
        val json = """{"left":{"_derivedTypeId":"generic","payload":"text","leftOnly":1},
            "right":{"_derivedTypeId":"generic","payload":42,"rightOnly":2}}"""
        val result = mapper.readValue(json, ContextualHolder::class.java)
        assertEquals(GenericLeaf("text"), result.left)
        assertEquals(GenericLeaf(42), result.right)
        assertThrows(DatabindException::class.java) {
            mapper.readValue(json.replace("rightOnly", "leftOnly"), ContextualHolder::class.java)
        }
    }

    @Test
    fun `updating reads replace values and validate ids`() {
        val mapper = mapper(DerivedTypes.of(Middle::class.java, Leaf::class.java))
        val original = Middle()
        val reader = mapper.readerFor(Base::class.java).withValueToUpdate(original)
        val replacement = reader.readValue<Middle>("""{"_derivedTypeId":"base","name":"replacement"}""")
        assertNotSame(original, replacement)
        assertEquals("middle", original.name)
        assertEquals("replacement", replacement.name)
        assertNull(reader.readValue<Any>("{}"))
        assertThrows(DatabindException::class.java) { reader.readValue<Any>("""{"_derivedTypeId":"unknown"}""") }
    }

    @Test
    fun `incompatible generic specialization is a mapping failure with cause`() {
        val mapper = mapper(DerivedTypes.of(FixedLeaf::class.java))
        val error = assertThrows(DatabindException::class.java) {
            mapper.readValue("""{"_derivedTypeId":"fixed","payload":"text"}""",
                object : TypeReference<GenericFamily<Known>>() {})
        }
        assertInstanceOf(IllegalArgumentException::class.java, error.cause)
        assertTrue(checkNotNull(error.message).contains(FixedLeaf::class.java.name))
    }

    @Test
    fun `non object custom bodies fail without a parallel serializer`() {
        val mapper = mapper(DerivedTypes.of(ScalarBody::class.java))
        val error = assertThrows(DatabindException::class.java) { mapper.writeValueAsString(ScalarBody()) }
        assertTrue(checkNotNull(error.message).contains("did not serialize as an object"))
    }

    @Test
    fun `decimal properties concepts and oversized integers retain exact values and bytes`() {
        val plain = JsonMapper.builder().addModule(ConceptsModule()).build()
        val mapper = JsonMapper.builder().addModule(ConceptsModule())
            .addModule(DerivedTypesModule(DerivedTypes.of(Precise::class.java))).build()
        val decimal = BigDecimal("12345678901234567890.123456789")
        val integer = BigInteger("1234567890123456789012345678901234567890")
        val expected = Precise(decimal, DecimalAmount(decimal), integer)
        val body = """{"decimal":12345678901234567890.123456789,"concept":12345678901234567890.123456789,"integer":1234567890123456789012345678901234567890}"""
        val json = body.dropLast(1) + ""","_derivedTypeId":"precise"}"""
        assertEquals(body, plain.writeValueAsString(expected))
        assertEquals(json, mapper.writeValueAsString(expected))
        assertEquals(expected, mapper.readValue(json, ITargetType::class.java))
        val idFirst = """{"_derivedTypeId":"precise",""" + body.drop(1)
        assertEquals(expected, mapper.readValue(idFirst, ITargetType::class.java))
        assertEquals(expected, mapper.readValue(mapper.writeValueAsBytes(expected), ITargetType::class.java))
        assertFalse(mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS))
    }

    @Test
    fun `non decimal body tokens keep ordinary Jackson bytes`() {
        val plain = JsonMapper.builder().build()
        val mapper = mapper(DerivedTypes.of(NonDecimal::class.java))
        val value = NonDecimal(1.25e-17, 0.1f, true, "quoted \"text\" and \u00e6", null)
        val expected = plain.writeValueAsString(value).dropLast(1) + ""","_derivedTypeId":"ordinary"}"""
        assertArrayEquals(expected.toByteArray(Charsets.UTF_8), mapper.writeValueAsBytes(value))
    }

    @Test
    fun `existing discriminator is replaced and moved after all body fields`() {
        val mapper = mapper(DerivedTypes.of(DiscriminatorBody::class.java))
        assertEquals("""{"number":42,"_derivedTypeId":"collision"}""", mapper.writeValueAsString(DiscriminatorBody()))
    }

    @Test
    fun `native type serializer keeps its prefix and suffix around the derived body`() {
        val mapper = mapper(DerivedTypes.of(NativeBody::class.java))
        val expected = """["native",{"number":7,"_derivedTypeId":"native"}]"""
        assertEquals(expected, mapper.writeValueAsString(NativeBody(7)))
        assertEquals(NativeBody(7), mapper.readValue(expected, NativeFamily::class.java))
    }

    private fun mapper(registry: DerivedTypes): JsonMapper =
        JsonMapper.builder().addModule(DerivedTypesModule(registry)).build()

    private fun assertIdLast(node: JsonNode) {
        if (node.isObject && node.has(DerivedTypesModule.DERIVED_TYPE_ID_PROPERTY)) {
            assertEquals(DerivedTypesModule.DERIVED_TYPE_ID_PROPERTY, node.propertyNames().last())
        }
        if (node.isContainer) node.toList().forEach(::assertIdLast)
    }
}
