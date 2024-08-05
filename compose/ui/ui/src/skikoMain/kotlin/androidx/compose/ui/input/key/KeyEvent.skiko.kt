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

@file:JvmName("KeyEvent_desktopKt")
@file:JvmMultifileClass

package androidx.compose.ui.input.key

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltGraphPressed
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

actual data class NativeKeyEvent(
    val key: Key,
    val type: KeyEventType,
    val codePoint: Int,
    val modifiers: PointerKeyboardModifiers, // Reuse pointer modifiers
    val nativeEvent: Any? = null
)

internal val InternalKeyEvent.internal: NativeKeyEvent
    get() = nativeKeyEvent as NativeKeyEvent

actual val InternalKeyEvent.key: Key
    get() = internal.key

actual val InternalKeyEvent.utf16CodePoint: Int
    get() = internal.codePoint

actual val InternalKeyEvent.type: KeyEventType
    get() = internal.type

actual val InternalKeyEvent.isAltPressed: Boolean
    get() = internal.modifiers.isAltPressed || internal.modifiers.isAltGraphPressed

actual val InternalKeyEvent.isCtrlPressed: Boolean
    get() = internal.modifiers.isCtrlPressed

actual val InternalKeyEvent.isMetaPressed: Boolean
    get() = internal.modifiers.isMetaPressed

actual val InternalKeyEvent.isShiftPressed: Boolean
    get() = internal.modifiers.isShiftPressed

@InternalComposeUiApi
fun KeyEvent(
    key: Key,
    type: KeyEventType,
    codePoint: Int = 0,
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isAltPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    nativeEvent: Any? = null
) = InternalKeyEvent(
    nativeKeyEvent = NativeKeyEvent(
        key = key,
        type = type,
        codePoint = codePoint,
        modifiers = PointerKeyboardModifiers(
            isCtrlPressed = isCtrlPressed,
            isMetaPressed = isMetaPressed,
            isAltPressed = isAltPressed,
            isShiftPressed = isShiftPressed
        ),
        nativeEvent = nativeEvent
    )
)

internal fun InternalKeyEvent.copy(
    key: Key = this.internal.key,
    type: KeyEventType = this.internal.type,
    codePoint: Int = this.internal.codePoint,
    modifiers: PointerKeyboardModifiers = this.internal.modifiers,
    nativeEvent: Any? = this.internal.nativeEvent
) = InternalKeyEvent(
    nativeKeyEvent = NativeKeyEvent(
        key = key,
        type = type,
        codePoint = codePoint,
        modifiers = modifiers,
        nativeEvent = nativeEvent
    )
)
