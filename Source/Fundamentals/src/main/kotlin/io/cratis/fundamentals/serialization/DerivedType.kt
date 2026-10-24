// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.serialization

import kotlin.reflect.KClass

/** A stable string wire id, independent of Chronicle event types. [targetType] defaults to inference. */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class DerivedType(public val value: String, public val targetType: KClass<*> = Any::class)
