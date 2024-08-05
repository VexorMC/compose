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

package androidx.compose.ui.test

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test


/**
 * Tests the key-event sending functionality of the test framework.
 */
@OptIn(ExperimentalTestApi::class)
class KeyInputTest {

    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun FocusedBox(
        onKeyEvent: ((InternalKeyEvent) -> Unit)? = null
    ) {
        val focusRequester = remember { FocusRequester() }
        Box(
            Modifier
                .testTag("tag")
                .size(100.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    onKeyEvent?.invoke(it)
                    true
                }
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    @Test
    fun testKeyDownAndUp() {
        var internalKeyEvent: InternalKeyEvent? = null

        rule.setContent {
            FocusedBox {
                internalKeyEvent = it
            }
        }

        with(rule.onNodeWithTag("tag")) {
            performKeyInput {
                keyDown(Key.C)
            }
            assertNotNull(internalKeyEvent, "Key event not detected")
            internalKeyEvent!!.let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key event type")
            }
            internalKeyEvent = null

            performKeyInput {
                keyUp(Key.C)
            }
            assertNotNull(internalKeyEvent, "Key event not detected")
            internalKeyEvent!!.let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key event type")
            }
        }
    }

    @Test
    fun testIsKeyDown() {
        rule.setContent {
            FocusedBox()
        }

        with(rule.onNodeWithTag("tag")) {
            performKeyInput {
                keyDown(Key.C)
                assertTrue(isKeyDown(Key.C), "Key is not down")
                assertFalse(isKeyDown(Key.A), "Unexpected key is down")
                keyUp(Key.C)
                assertFalse(isKeyDown(Key.C), "Key is down")
            }
        }
    }

    @Test
    fun testPressKey() {
        val internalKeyEvents = mutableListOf<InternalKeyEvent>()

        rule.setContent {
            FocusedBox {
                internalKeyEvents.add(it)
            }
        }

        with(rule.onNodeWithTag("tag")) {
            performKeyInput {
                pressKey(Key.C)
            }
            assertEquals(2, internalKeyEvents.size)
            internalKeyEvents[0].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
            }
            internalKeyEvents[1].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
        }
    }

    @Test
    fun testWithKeyDown() {
        val internalKeyEvents = mutableListOf<InternalKeyEvent>()

        rule.setContent {
            FocusedBox {
                internalKeyEvents.add(it)
            }
        }

        with(rule.onNodeWithTag("tag")) {
            performKeyInput {
                withKeyDown(Key.ShiftLeft) {
                    pressKey(Key.C)
                }
                pressKey(Key.C)
            }
            assertEquals(6, internalKeyEvents.size)
            internalKeyEvents[0].let {
                assertEquals(Key.ShiftLeft, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
            }
            internalKeyEvents[1].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
                assertTrue(it.isShiftPressed, "Shift is not pressed")
                assertEquals('C'.code, it.utf16CodePoint, "Wrong code point")
            }
            internalKeyEvents[2].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
            internalKeyEvents[3].let {
                assertEquals(Key.ShiftLeft, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
            internalKeyEvents[4].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
                assertEquals('c'.code, it.utf16CodePoint, "Wrong code point")
            }
        }
    }

    @Test
    fun testWithKeyToggled() {
        val internalKeyEvents = mutableListOf<InternalKeyEvent>()

        rule.setContent {
            FocusedBox {
                internalKeyEvents.add(it)
            }
        }

        with(rule.onNodeWithTag("tag")) {
            performKeyInput {
                withKeyToggled(Key.CapsLock) {
                    assertTrue(isCapsLockOn)
                    pressKey(Key.C)
                }
                pressKey(Key.C)
            }
            assertEquals(8, internalKeyEvents.size)
            internalKeyEvents[2].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
                assertEquals('C'.code, it.utf16CodePoint, "Wrong code point")
            }
            internalKeyEvents[3].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
            internalKeyEvents[6].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
                assertEquals('c'.code, it.utf16CodePoint, "Wrong code point")
            }
        }
    }
}