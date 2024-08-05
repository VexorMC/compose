/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.dom

import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.key.internal
import org.w3c.dom.events.KeyboardEvent


/**
 * The original raw native KeyboardEvent event
 *
 * Null if:
 * - the native event is sent by another framework (when Compose UI is embed into it)
 * - there is no native event (in tests, for example, or when Compose sends a synthetic event)
 *
 */
val InternalKeyEvent.domEventOrNull: KeyboardEvent?
    get() = internal.nativeEvent as? KeyboardEvent?
