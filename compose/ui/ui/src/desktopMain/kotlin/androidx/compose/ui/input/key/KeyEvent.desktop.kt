/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import java.awt.Toolkit
import java.awt.event.KeyEvent.KEY_LOCATION_STANDARD
import java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED

private val java.awt.event.KeyEvent.keyLocationForCompose get() =
    if (keyLocation == KEY_LOCATION_UNKNOWN) KEY_LOCATION_STANDARD else keyLocation

internal fun java.awt.event.KeyEvent.toComposeEvent() = InternalKeyEvent(
    nativeKeyEvent = NativeKeyEvent(
        key = Key(
            nativeKeyCode = keyCode,
            nativeKeyLocation = keyLocationForCompose
        ),
        type = when (id) {
            KEY_PRESSED -> KeyEventType.KeyDown
            KEY_RELEASED -> KeyEventType.KeyUp
            else -> KeyEventType.Unknown
        },
        codePoint = keyChar.code,
        modifiers = toPointerKeyboardModifiers(),
        nativeEvent = this
    )
)

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private fun java.awt.event.KeyEvent.toPointerKeyboardModifiers(): PointerKeyboardModifiers {
    return PointerKeyboardModifiers(
        isCtrlPressed = isControlDown,
        isMetaPressed = isMetaDown,
        isAltPressed = isAltDown,
        isShiftPressed = isShiftDown,
        isAltGraphPressed = isAltGraphDown,
        isSymPressed = false, // no sym in awtEvent?
        isFunctionPressed = false, // no Fn in awtEvent?
        isCapsLockOn = getLockingKeyStateSafe(java.awt.event.KeyEvent.VK_CAPS_LOCK),
        isScrollLockOn = getLockingKeyStateSafe(java.awt.event.KeyEvent.VK_SCROLL_LOCK),
        isNumLockOn = getLockingKeyStateSafe(java.awt.event.KeyEvent.VK_NUM_LOCK),
    )
}
