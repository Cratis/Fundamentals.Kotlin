// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.fundamentals.concepts

import java.util.concurrent.ConcurrentHashMap

internal class GenerationCache<K : Any, V>(private val capacity: Int = 128) {
    private val entries = ConcurrentHashMap<K, Lazy<V>>()
    private val missLock = Any()

    fun get(key: K, create: (K) -> V): V {
        val entry = entries[key] ?: insert(key, create)
        try {
            return entry.value
        } catch (exception: Throwable) {
            entries.remove(key, entry)
            throw exception
        }
    }

    private fun insert(key: K, create: (K) -> V): Lazy<V> = synchronized(missLock) {
        entries[key]?.let { return@synchronized it }
        if (entries.size == capacity) entries.clear()
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) { create(key) }.also { entries[key] = it }
    }
}
