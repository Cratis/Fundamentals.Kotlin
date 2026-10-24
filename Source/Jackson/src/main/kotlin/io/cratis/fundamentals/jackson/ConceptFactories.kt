// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.concepts.ConceptAs
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal object ConceptFactories {
    private val factories = object : ClassValue<(Any?) -> Any>() {
        override fun computeValue(type: Class<*>): (Any?) -> Any {
            recordConstructor(type)?.let { return constructorFactory(it) }
            val valueType = boxed(ConceptAs.valueTypeOf(type))
            staticFactory(type, valueType)?.let { method ->
                check(method.trySetAccessible()) { "Cannot access ${type.name}.of" }
                return { value -> method.invoke(null, value) }
            }
            type.constructors.firstOrNull {
                it.parameterCount == 1 && boxed(it.parameterTypes[0]).isAssignableFrom(valueType)
            }?.let { return constructorFactory(it) }
            return {
                throw IllegalArgumentException(
                    "${type.name} must expose a one-component record canonical constructor, " +
                        "a public static of(value) factory, or a public single-arg constructor"
                )
            }
        }
    }

    fun create(type: Class<*>, value: Any?): Any = factories.get(type)(value)

    private fun recordConstructor(type: Class<*>): Constructor<*>? {
        if (!type.isRecord || type.recordComponents.size != 1) return null
        return type.getDeclaredConstructor(type.recordComponents[0].type)
    }

    private fun staticFactory(type: Class<*>, valueType: Class<*>): Method? = type.methods.firstOrNull {
        it.name == "of" && Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) &&
            it.parameterCount == 1 && it.returnType == type &&
            boxed(it.parameterTypes[0]).isAssignableFrom(valueType)
    }

    private fun constructorFactory(constructor: Constructor<*>): (Any?) -> Any {
        check(constructor.trySetAccessible()) { "Cannot access ${constructor.declaringClass.name} constructor" }
        return { value -> constructor.newInstance(value) }
    }

    private fun boxed(type: Class<*>): Class<*> = when (type) {
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        else -> type
    }
}
