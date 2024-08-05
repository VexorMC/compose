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

package dev.lunasa

import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import org.lwjgl.glfw.GLFW.*
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType

@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
fun ComposeScene.subscribeToGLFWEvents(windowHandle: Long) {
    glfwSetMouseButtonCallback(windowHandle) { _, _, action, _ ->
        sendPointerEvent(
            position = glfwGetCursorPos(windowHandle),
            eventType = when (action) {
                GLFW_PRESS -> PointerEventType.Press
                GLFW_RELEASE -> PointerEventType.Release
                else -> PointerEventType.Unknown
            },
            nativeEvent =  MouseEvent(getAwtMods(windowHandle))
        )
    }

    glfwSetCursorPosCallback(windowHandle) { _, xpos, ypos ->
        sendPointerEvent(
            position = Offset(xpos.toFloat(), ypos.toFloat()),
            eventType = PointerEventType.Move,
            nativeEvent =  MouseEvent(getAwtMods(windowHandle))
        )
    }

    glfwSetCursorEnterCallback(windowHandle) { _, entered ->
        sendPointerEvent(
            position = glfwGetCursorPos(windowHandle),
            eventType = if (entered) PointerEventType.Enter else PointerEventType.Exit,
            nativeEvent =  MouseEvent(getAwtMods(windowHandle))
        )
    }

    glfwSetScrollCallback(windowHandle) { _, xOffset, yOffset ->
        sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = glfwGetCursorPos(windowHandle),
            scrollDelta = Offset(xOffset.toFloat(), -yOffset.toFloat()),
            nativeEvent =  MouseWheelEvent(getAwtMods(windowHandle))
        )
    }

    glfwSetKeyCallback(windowHandle) { _, key, _, action, _ ->
        val awtId = when (action) {
            GLFW_PRESS -> KeyEventType.KeyDown
            GLFW_REPEAT -> KeyEventType.KeyUp
            GLFW_RELEASE -> KeyEventType.KeyUp
            else -> error("Unknown type")
        }
        val awtKey = glfwToAwtKeyCode(key)
        val time = System.nanoTime() / 1_000_000

        // Note that we don't distinguish between Left/Right Shift, Del from numpad or not, etc.
        // To distinguish we should change `location` parameter
        sendKeyEvent(KeyEvent(Key(awtKey), awtId, 0,
            isCtrlPressed = isCtrlPressed(windowHandle),
            isShiftPressed = isShiftPressed(windowHandle),
            isAltPressed = isAltPressed(windowHandle),
        ))
    }

    glfwSetCharCallback(windowHandle) { _, codepoint ->
        for (char in Character.toChars(codepoint)) {
            val time = System.nanoTime() / 1_000_000

            sendKeyEvent(KeyEvent(
                Key.Unknown,
                KeyEventType.KeyDown, codepoint,
                isCtrlPressed = isCtrlPressed(windowHandle),
                isShiftPressed = isShiftPressed(windowHandle),
                isAltPressed = isAltPressed(windowHandle),
                nativeEvent = AwtKeyEvent(
                    awtComponent,
                    AwtKeyEvent.KEY_TYPED,
                    time,
                    0,
                    0,
                    char,
                    AwtKeyEvent.KEY_LOCATION_UNKNOWN
                )
            ))
        }
    }

    glfwSetWindowContentScaleCallback(windowHandle) { _, xscale, _ ->
        density = Density(xscale)
    }
}

private fun glfwGetCursorPos(window: Long): Offset {
    val x = DoubleArray(1)
    val y = DoubleArray(1)
    glfwGetCursorPos(window, x, y)
    return Offset(x[0].toFloat(), y[0].toFloat())
}

// in the future versions of Compose we plan to get rid of the need of AWT events/components
val awtComponent = object : Component() {}

private fun MouseEvent(awtMods: Int) = MouseEvent(
    awtComponent, 0, 0, awtMods, 0, 0, 1, false
)

private fun MouseWheelEvent(awtMods: Int) = MouseWheelEvent(
    awtComponent, 0, 0, awtMods, 0, 0, 1, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1
)

private fun isCtrlPressed(windowHandle: Long): Boolean {
    return glfwGetKey(windowHandle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS;
}

private fun isShiftPressed(windowHandle: Long): Boolean {
    return glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
}

private fun isAltPressed(windowHandle: Long): Boolean {
    return glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS;
}

private fun getAwtMods(windowHandle: Long): Int {
    var awtMods = 0
    if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS)
        awtMods = awtMods or InputEvent.BUTTON1_DOWN_MASK
    if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS)
        awtMods = awtMods or InputEvent.BUTTON2_DOWN_MASK
    if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_3) == GLFW_PRESS)
        awtMods = awtMods or InputEvent.BUTTON3_DOWN_MASK
    if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_4) == GLFW_PRESS)
        awtMods = awtMods or (1 shl 14)
    if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_5) == GLFW_PRESS)
        awtMods = awtMods or (1 shl 15)
    if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS)
        awtMods = awtMods or InputEvent.CTRL_DOWN_MASK
    if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
        awtMods = awtMods or InputEvent.SHIFT_DOWN_MASK
    if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS)
        awtMods = awtMods or InputEvent.ALT_DOWN_MASK
    return awtMods
}