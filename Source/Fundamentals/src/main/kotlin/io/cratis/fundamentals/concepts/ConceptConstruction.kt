// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal object ConceptConstruction {
    fun resolve(type: Class<*>, valueType: Class<*>): (Any) -> Any {
        recordConstructor(type)?.let { return constructorFactory(it) }
        val boxedValueType = boxed(valueType)
        staticFactory(type, boxedValueType)?.let { method ->
            check(method.trySetAccessible()) { "Cannot access ${type.name}.of" }
            return { value -> invoke { method.invoke(null, value) } }
        }
        type.constructors.firstOrNull {
            it.parameterCount == 1 && boxed(it.parameterTypes[0]).isAssignableFrom(boxedValueType)
        }?.let { return constructorFactory(it) }
        throw IllegalArgumentException(
            "${type.name} must expose a one-component record canonical constructor, " +
                "a public static of(value) factory, or a public single-arg constructor"
        )
    }

    private fun recordConstructor(type: Class<*>): Constructor<*>? {
        if (!type.isRecord || type.recordComponents.size != 1) return null
        return type.getDeclaredConstructor(type.recordComponents[0].type)
    }

    private fun staticFactory(type: Class<*>, valueType: Class<*>): Method? = type.methods.firstOrNull {
        it.name == "of" && Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) &&
            it.parameterCount == 1 && it.returnType == type &&
            boxed(it.parameterTypes[0]).isAssignableFrom(valueType)
    }

    private fun constructorFactory(constructor: Constructor<*>): (Any) -> Any {
        check(constructor.trySetAccessible()) { "Cannot access ${constructor.declaringClass.name} constructor" }
        return { value -> invoke { constructor.newInstance(value) } }
    }

    private fun invoke(action: () -> Any): Any = try {
        action()
    } catch (exception: InvocationTargetException) {
        throw (exception.cause ?: exception)
    }

    private fun boxed(type: Class<*>): Class<*> = type.kotlin.javaObjectType
}
