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

import org.lwjgl.glfw.GLFW.*
import java.awt.event.KeyEvent

fun glfwToAwtKeyCode(glfwKeyCode: Int): Int = when (glfwKeyCode) {
    GLFW_KEY_SPACE -> KeyEvent.VK_SPACE
    GLFW_KEY_APOSTROPHE -> KeyEvent.VK_QUOTE
    GLFW_KEY_COMMA -> KeyEvent.VK_COMMA
    GLFW_KEY_MINUS -> KeyEvent.VK_MINUS
    GLFW_KEY_PERIOD -> KeyEvent.VK_PERIOD
    GLFW_KEY_SLASH -> KeyEvent.VK_SLASH
    GLFW_KEY_0 -> KeyEvent.VK_0
    GLFW_KEY_1 -> KeyEvent.VK_1
    GLFW_KEY_2 -> KeyEvent.VK_2
    GLFW_KEY_3 -> KeyEvent.VK_3
    GLFW_KEY_4 -> KeyEvent.VK_4
    GLFW_KEY_5 -> KeyEvent.VK_5
    GLFW_KEY_6 -> KeyEvent.VK_6
    GLFW_KEY_7 -> KeyEvent.VK_7
    GLFW_KEY_8 -> KeyEvent.VK_8
    GLFW_KEY_9 -> KeyEvent.VK_9
    GLFW_KEY_SEMICOLON -> KeyEvent.VK_SEMICOLON
    GLFW_KEY_EQUAL -> KeyEvent.VK_EQUALS
    GLFW_KEY_A -> KeyEvent.VK_A
    GLFW_KEY_B -> KeyEvent.VK_B
    GLFW_KEY_C -> KeyEvent.VK_C
    GLFW_KEY_D -> KeyEvent.VK_D
    GLFW_KEY_E -> KeyEvent.VK_E
    GLFW_KEY_F -> KeyEvent.VK_F
    GLFW_KEY_G -> KeyEvent.VK_G
    GLFW_KEY_H -> KeyEvent.VK_H
    GLFW_KEY_I -> KeyEvent.VK_I
    GLFW_KEY_J -> KeyEvent.VK_J
    GLFW_KEY_K -> KeyEvent.VK_K
    GLFW_KEY_L -> KeyEvent.VK_L
    GLFW_KEY_M -> KeyEvent.VK_M
    GLFW_KEY_N -> KeyEvent.VK_N
    GLFW_KEY_O -> KeyEvent.VK_O
    GLFW_KEY_P -> KeyEvent.VK_P
    GLFW_KEY_Q -> KeyEvent.VK_Q
    GLFW_KEY_R -> KeyEvent.VK_R
    GLFW_KEY_S -> KeyEvent.VK_S
    GLFW_KEY_T -> KeyEvent.VK_T
    GLFW_KEY_U -> KeyEvent.VK_U
    GLFW_KEY_V -> KeyEvent.VK_V
    GLFW_KEY_W -> KeyEvent.VK_W
    GLFW_KEY_X -> KeyEvent.VK_X
    GLFW_KEY_Y -> KeyEvent.VK_Y
    GLFW_KEY_Z -> KeyEvent.VK_Z
    GLFW_KEY_LEFT_BRACKET -> KeyEvent.VK_OPEN_BRACKET
    GLFW_KEY_BACKSLASH -> KeyEvent.VK_BACK_SLASH
    GLFW_KEY_RIGHT_BRACKET -> KeyEvent.VK_CLOSE_BRACKET
    GLFW_KEY_GRAVE_ACCENT -> KeyEvent.VK_BACK_QUOTE
    GLFW_KEY_ESCAPE -> KeyEvent.VK_ESCAPE
    GLFW_KEY_ENTER -> KeyEvent.VK_ENTER
    GLFW_KEY_TAB -> KeyEvent.VK_TAB
    GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE
    GLFW_KEY_INSERT -> KeyEvent.VK_INSERT
    GLFW_KEY_DELETE -> KeyEvent.VK_DELETE
    GLFW_KEY_RIGHT -> KeyEvent.VK_RIGHT
    GLFW_KEY_LEFT -> KeyEvent.VK_LEFT
    GLFW_KEY_DOWN -> KeyEvent.VK_DOWN
    GLFW_KEY_UP -> KeyEvent.VK_UP
    GLFW_KEY_PAGE_UP -> KeyEvent.VK_PAGE_UP
    GLFW_KEY_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN
    GLFW_KEY_HOME -> KeyEvent.VK_HOME
    GLFW_KEY_END -> KeyEvent.VK_END
    GLFW_KEY_CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK
    GLFW_KEY_SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK
    GLFW_KEY_NUM_LOCK -> KeyEvent.VK_NUM_LOCK
    GLFW_KEY_PRINT_SCREEN -> KeyEvent.VK_PRINTSCREEN
    GLFW_KEY_PAUSE -> KeyEvent.VK_PAUSE
    GLFW_KEY_F1 -> KeyEvent.VK_F1
    GLFW_KEY_F2 -> KeyEvent.VK_F2
    GLFW_KEY_F3 -> KeyEvent.VK_F3
    GLFW_KEY_F4 -> KeyEvent.VK_F4
    GLFW_KEY_F5 -> KeyEvent.VK_F5
    GLFW_KEY_F6 -> KeyEvent.VK_F6
    GLFW_KEY_F7 -> KeyEvent.VK_F7
    GLFW_KEY_F8 -> KeyEvent.VK_F8
    GLFW_KEY_F9 -> KeyEvent.VK_F9
    GLFW_KEY_F10 -> KeyEvent.VK_F10
    GLFW_KEY_F11 -> KeyEvent.VK_F11
    GLFW_KEY_F12 -> KeyEvent.VK_F12
    GLFW_KEY_F13 -> KeyEvent.VK_F13
    GLFW_KEY_F14 -> KeyEvent.VK_F14
    GLFW_KEY_F15 -> KeyEvent.VK_F15
    GLFW_KEY_F16 -> KeyEvent.VK_F16
    GLFW_KEY_F17 -> KeyEvent.VK_F17
    GLFW_KEY_F18 -> KeyEvent.VK_F18
    GLFW_KEY_F19 -> KeyEvent.VK_F19
    GLFW_KEY_F20 -> KeyEvent.VK_F20
    GLFW_KEY_F21 -> KeyEvent.VK_F21
    GLFW_KEY_F22 -> KeyEvent.VK_F22
    GLFW_KEY_F23 -> KeyEvent.VK_F23
    GLFW_KEY_F24 -> KeyEvent.VK_F24
    GLFW_KEY_KP_0 -> KeyEvent.VK_NUMPAD0
    GLFW_KEY_KP_1 -> KeyEvent.VK_NUMPAD1
    GLFW_KEY_KP_2 -> KeyEvent.VK_NUMPAD2
    GLFW_KEY_KP_3 -> KeyEvent.VK_NUMPAD3
    GLFW_KEY_KP_4 -> KeyEvent.VK_NUMPAD4
    GLFW_KEY_KP_5 -> KeyEvent.VK_NUMPAD5
    GLFW_KEY_KP_6 -> KeyEvent.VK_NUMPAD6
    GLFW_KEY_KP_7 -> KeyEvent.VK_NUMPAD7
    GLFW_KEY_KP_8 -> KeyEvent.VK_NUMPAD8
    GLFW_KEY_KP_9 -> KeyEvent.VK_NUMPAD9
    GLFW_KEY_LEFT_SHIFT -> KeyEvent.VK_SHIFT
    GLFW_KEY_LEFT_CONTROL -> KeyEvent.VK_CONTROL
    GLFW_KEY_LEFT_ALT -> KeyEvent.VK_ALT
    GLFW_KEY_RIGHT_SHIFT -> KeyEvent.VK_SHIFT
    GLFW_KEY_RIGHT_CONTROL -> KeyEvent.VK_CONTROL
    GLFW_KEY_RIGHT_ALT -> KeyEvent.VK_ALT
    else -> KeyEvent.VK_UNDEFINED
}