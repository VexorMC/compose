/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3.internal

import kotlinx.atomicfu.atomic

internal actual class InternalAtomicReference<V> actual constructor(value: V) {
    private val delegate = atomic(value)
    actual fun get() = delegate.value
    actual fun set(value: V) {
        delegate.value = value
    }
    actual fun getAndSet(value: V) = delegate.getAndSet(value)
    actual fun compareAndSet(expect: V, newValue: V) = delegate.compareAndSet(expect, newValue)
}