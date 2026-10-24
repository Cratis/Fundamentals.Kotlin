// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

/**
 * Discovers concepts following the static `create(GenerationContext): T` convention.
 * The JVM cannot enforce static abstract members; this marker has no members.
 */
public interface GeneratableInContext<T : GeneratableInContext<T>>
