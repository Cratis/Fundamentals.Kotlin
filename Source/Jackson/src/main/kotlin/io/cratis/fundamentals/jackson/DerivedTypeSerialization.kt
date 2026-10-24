// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import io.cratis.fundamentals.serialization.DerivedType
import io.cratis.fundamentals.serialization.DerivedTypeId
import io.cratis.fundamentals.serialization.DerivedTypes
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.BeanProperty
import tools.jackson.databind.DatabindException
import tools.jackson.databind.DeserializationConfig
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.SerializationConfig
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.deser.ValueDeserializerModifier
import tools.jackson.databind.deser.std.DelegatingDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.ser.ValueSerializerModifier
import tools.jackson.databind.util.AccessPattern
import tools.jackson.databind.util.TokenBuffer

internal class DerivedTypeDeserializerModifier(private val registry: DerivedTypes) : ValueDeserializerModifier() {
    override fun modifyDeserializer(
        config: DeserializationConfig, beanDesc: BeanDescription.Supplier, deserializer: ValueDeserializer<*>
    ): ValueDeserializer<*> {
        val type = beanDesc.get().type
        if (type.isEnumType) return deserializer
        if (ConceptAs.isConcept(type.rawClass)) return deserializer
        if (!registry.hasDerivatives(type.rawClass)) return deserializer
        return DerivedTypeDeserializer(type, registry, deserializer)
    }
}

private class DerivedTypeDeserializer(
    private val baseType: JavaType,
    private val registry: DerivedTypes,
    delegate: ValueDeserializer<*>,
    private val property: BeanProperty? = null
) : DelegatingDeserializer(delegate) {
    override fun newDelegatingInstance(newDelegatee: ValueDeserializer<*>): ValueDeserializer<*> =
        DerivedTypeDeserializer(baseType, registry, newDelegatee, property)

    override fun createContextual(context: DeserializationContext, property: BeanProperty?): ValueDeserializer<*> =
        DerivedTypeDeserializer(baseType, registry, context.handleSecondaryContextualization(_delegatee, property, baseType), property)

    // Keep replacement reads, Jackson-owned native type dispatch and null/empty behavior instead of the bean delegate's.
    override fun supportsUpdate(config: DeserializationConfig): Boolean? = null

    override fun deserialize(parser: JsonParser, context: DeserializationContext, intoValue: Any): Any? {
        context.handleBadMerge(this)
        return deserialize(parser, context)
    }

    override fun deserializeWithType(
        parser: JsonParser, context: DeserializationContext, typeDeserializer: TypeDeserializer
    ): Any? = typeDeserializer.deserializeTypedFromAny(parser, context)

    override fun getNullValue(context: DeserializationContext): Any? = null

    override fun getEmptyValue(context: DeserializationContext): Any? = null

    override fun getEmptyAccessPattern(): AccessPattern = AccessPattern.ALWAYS_NULL

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Any? {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null
        val node = context.readTree(parser) as? ObjectNode
            ?: throw DatabindException.from(parser, "Expected an object for ${baseType.rawClass.name}")
        val idNode = node.remove(DerivedTypesModule.DERIVED_TYPE_ID_PROPERTY) ?: return null
        if (!idNode.isString) {
            throw DatabindException.from(parser, "Expected textual derived type identifier '$idNode' for ${baseType.rawClass.name}")
        }
        val id = idNode.stringValue()
        val targetType = specialize(context, parser, resolveId(parser, id))
        val target = context.findNonContextualValueDeserializer(targetType)
        // Consume only this wrapper's discriminator; nested values and third-party decorators retain their own handlers.
        val ordinary = if (target is DerivedTypeDeserializer) target._delegatee else target
        val contextual = context.handleSecondaryContextualization(ordinary, property, targetType)
        return context.treeAsTokens(node).use { treeParser ->
            treeParser.nextToken()
            contextual.deserialize(treeParser, context)
        }
    }

    private fun resolveId(parser: JsonParser, id: String): Class<*> = try {
        registry.derivedTypeFor(baseType.rawClass, DerivedTypeId(id))
    } catch (exception: IllegalArgumentException) {
        throw DatabindException.from(parser, "Unknown derived type identifier '$id' for ${baseType.rawClass.name}", exception)
    }

    private fun specialize(context: DeserializationContext, parser: JsonParser, derived: Class<*>): JavaType = try {
        context.constructSpecializedType(baseType, derived)
    } catch (exception: IllegalArgumentException) {
        throw DatabindException.from(parser, "Cannot specialize derived type ${derived.name} for $baseType", exception)
    }
}

internal class DerivedTypeSerializerModifier(private val registry: DerivedTypes) : ValueSerializerModifier() {
    override fun modifySerializer(
        config: SerializationConfig, beanDesc: BeanDescription.Supplier, serializer: ValueSerializer<*>
    ): ValueSerializer<*> {
        val type = beanDesc.beanClass
        val annotation = type.getDeclaredAnnotation(DerivedType::class.java) ?: return serializer
        val registered = registry.isDerivedType(type)
        val missing = !registered && registry.typesWithDerivatives.any { it != type && it.isAssignableFrom(type) }
        if (!registered && !missing) return serializer
        @Suppress("UNCHECKED_CAST")
        return DerivedTypeSerializer(serializer as ValueSerializer<Any>, annotation.value, missing)
    }
}

private class DerivedTypeSerializer(
    private val delegate: ValueSerializer<Any>, private val id: String, private val missing: Boolean
) : ValueSerializer<Any>() {
    override fun resolve(context: SerializationContext) {
        delegate.resolve(context)
    }

    override fun createContextual(context: SerializationContext, property: BeanProperty?): ValueSerializer<*> {
        val contextual = delegate.createContextual(context, property)
        if (contextual === delegate) return this
        @Suppress("UNCHECKED_CAST")
        return DerivedTypeSerializer(contextual as ValueSerializer<Any>, id, missing)
    }

    override fun serialize(value: Any, generator: JsonGenerator, context: SerializationContext) {
        if (missing) {
            throw DatabindException.from(generator, "${value.javaClass.name} carries @DerivedType('$id') but is not registered")
        }
        val node = TokenBuffer(context, false).use { buffer ->
            delegate.serialize(value, buffer, context)
            buffer.asParser().use { parser ->
                parser.nextToken()
                JsonMapper.shared().readTree(parser) as? ObjectNode
                    ?: throw DatabindException.from(generator, "Derived type ${value.javaClass.name}, id '$id', did not serialize as an object")
            }
        }
        node.put(DerivedTypesModule.DERIVED_TYPE_ID_PROPERTY, id)
        generator.objectWriteContext().writeTree(generator, node)
    }

    override fun serializeWithType(
        value: Any, generator: JsonGenerator, context: SerializationContext, typeSerializer: TypeSerializer
    ) {
        serialize(value, generator, context)
    }
}
