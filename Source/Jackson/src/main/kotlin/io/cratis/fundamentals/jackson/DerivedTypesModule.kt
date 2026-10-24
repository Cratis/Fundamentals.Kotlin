// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.jackson

import io.cratis.fundamentals.serialization.DerivedTypes
import tools.jackson.databind.module.SimpleModule

/**
 * Opt-in derived-type JSON, never ServiceLoader-discovered. Combine all classes into one immutable
 * [DerivedTypes] registry per mapper: a second instance replaces the first rather than stacking.
 * Registered runtime classes write the id last; registered targets read it in any position.
 * A missing id reads as null; unknown, null and non-string ids fail.
 */
public class DerivedTypesModule(private val derivedTypes: DerivedTypes) : SimpleModule("Cratis.Fundamentals.DerivedTypes") {
    public override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.addSerializerModifier(DerivedTypeSerializerModifier(derivedTypes))
        context.addDeserializerModifier(DerivedTypeDeserializerModifier(derivedTypes))
    }

    public companion object {
        public const val DERIVED_TYPE_ID_PROPERTY: String = "_derivedTypeId"
    }
}
