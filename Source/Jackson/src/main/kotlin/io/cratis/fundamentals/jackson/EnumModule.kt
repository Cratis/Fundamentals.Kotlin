// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.DeserializationConfig
import tools.jackson.databind.JavaType
import tools.jackson.databind.SerializationConfig
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.ValueSerializerModifier

/**
 * Opt-in ordinal enum values, following C# `Json/EnumConverter.cs` and `Json/EnumJson.cs`
 * and Chronicle.Kotlin's lenient enum reads. Not ServiceLoader-discovered; map keys are unchanged.
 *
 * Reads accept defined ordinals, numeric strings, names, JsonProperty/JsonAlias and toString text.
 * Exact matches precede case-insensitive matches. JsonValue/JsonFormat do not override this wire.
 * JVM ordinals only match C# values when declarations are ordered from zero without gaps;
 * explicit values, non-int backing types and flags are not supported.
 */
public class EnumModule : SimpleModule("Cratis.Fundamentals.Enums") {
    public override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.addSerializerModifier(EnumSerializerModifier)
        context.addDeserializers(EnumDeserializers)
    }
}

private object EnumSerializerModifier : ValueSerializerModifier() {
    override fun modifyEnumSerializer(
        config: SerializationConfig, type: JavaType, beanDesc: BeanDescription.Supplier, serializer: ValueSerializer<*>
    ): ValueSerializer<*> = OrdinalSerializer

    override fun modifySerializer(
        config: SerializationConfig, beanDesc: BeanDescription.Supplier, serializer: ValueSerializer<*>
    ): ValueSerializer<*> = if (isEnumType(beanDesc.beanClass)) OrdinalSerializer else serializer
}

private object OrdinalSerializer : ValueSerializer<Enum<*>>() {
    override fun serialize(value: Enum<*>, generator: JsonGenerator, context: SerializationContext) {
        generator.writeNumber(value.ordinal)
    }
}

private object EnumDeserializers : Deserializers.Base() {
    override fun hasDeserializerFor(config: DeserializationConfig, valueType: Class<*>): Boolean = isEnumType(valueType)

    override fun findEnumDeserializer(
        type: JavaType, config: DeserializationConfig, beanDesc: BeanDescription.Supplier
    ): ValueDeserializer<*>? = if (isEnumType(type.rawClass)) {
        val raw = type.rawClass
        EnumOrdinalDeserializer(if (raw.isEnum) raw else raw.superclass)
    } else null
}

private fun isEnumType(type: Class<*>): Boolean = Enum::class.java.isAssignableFrom(type) && type != Enum::class.java
