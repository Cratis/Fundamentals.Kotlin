// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.security.SecureRandom

internal object GenerationRandom {
    val source: SecureRandom = SecureRandom()
}
