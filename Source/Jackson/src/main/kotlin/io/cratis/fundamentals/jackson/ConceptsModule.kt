// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import com.fasterxml.jackson.annotation.JsonFormat
import io.cratis.fundamentals.concepts.ConceptAs
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.DeserializationConfig
import tools.jackson.databind.JavaType
import tools.jackson.databind.KeyDeserializer
import tools.jackson.databind.SerializationConfig
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.KeyDeserializers
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.Serializers

/**
 * Jackson 3 scalar and text-key support for [ConceptAs] implementations.
 * Register explicitly or discover through `JsonMapper.builder().findAndAddModules()`.
 * Construction uses core `ConceptDescriptor`: a one-component record, then public static
 * `of`, then a public single-argument constructor. LocalTime concepts write `HH:mm:ss.fffffff`,
 * truncating finer nanoseconds. Raw date/time overrides require explicit [TemporalModule].
 * Keys use value text, not C# embedded-JSON property names.
 */
public class ConceptsModule : SimpleModule("Cratis.Fundamentals.Concepts") {
    public override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.addSerializers(ConceptSerializers(ConceptValueSerializer))
        context.addDeserializers(ConceptDeserializers)
        context.addKeySerializers(ConceptSerializers(ConceptKeySerializer))
        context.addKeyDeserializers(ConceptKeyDeserializers)
    }
}

private class ConceptSerializers(private val serializer: ValueSerializer<*>) : Serializers.Base() {
    override fun findSerializer(
        config: SerializationConfig,
        type: JavaType,
        beanDesc: BeanDescription.Supplier,
        format: JsonFormat.Value?
    ): ValueSerializer<*>? = if (ConceptAs.isConcept(type.rawClass)) serializer else null
}

private object ConceptDeserializers : Deserializers.Base() {
    override fun hasDeserializerFor(config: DeserializationConfig, valueType: Class<*>): Boolean =
        ConceptAs.isConcept(valueType)

    override fun findBeanDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription.Supplier
    ): ValueDeserializer<*>? = if (ConceptAs.isConcept(type.rawClass)) ConceptValueDeserializer(type) else null
}

private object ConceptKeyDeserializers : KeyDeserializers {
    override fun findKeyDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription.Supplier
    ): KeyDeserializer? = if (ConceptAs.isConcept(type.rawClass)) ConceptKeyDeserializer(type) else null
}
