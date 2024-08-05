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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.input.key.InternalKeyEvent

/**
 * Factory function to create a platform specific [TextFieldKeyEventHandler].
 */
// TODO https://youtrack.jetbrains.com/issue/COMPOSE-741/Implement-createTextFieldKeyEventHandler
internal actual fun createTextFieldKeyEventHandler() = object : TextFieldKeyEventHandler() {}

// TODO https://youtrack.jetbrains.com/issue/COMPOSE-1361/Implement-isFromSoftKeyboard
/**
 * Returns whether this key event is created by the software keyboard.
 */
internal actual val InternalKeyEvent.isFromSoftKeyboard: Boolean
    get() = false