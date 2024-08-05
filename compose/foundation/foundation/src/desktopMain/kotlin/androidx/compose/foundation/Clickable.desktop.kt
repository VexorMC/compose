/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny

// TODO: b/168524931 - should this depend on the input device?
internal actual val TapIndicationDelay: Long = 0L

@Immutable @ExperimentalFoundationApi
@Deprecated(
    message = "Modifier.mouseClickable is deprecated and this MouseClickScope too. " +
        "See Modifier.mouseClickable for replacement"
)
class MouseClickScope constructor(
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers
)

@Suppress("DEPRECATION")
@ExperimentalFoundationApi
internal val EmptyClickContext = MouseClickScope(
    PointerButtons(0), PointerKeyboardModifiers(0)
)

/**
 * Creates modifier similar to [Modifier.clickable] but provides additional context with
 * information about pressed buttons and keyboard modifiers
 *
 */
@ExperimentalFoundationApi
@Deprecated(
    message = "Consider using Modifier.onClick to distinguish clicks with different buttons and keyboardModifiers",
    replaceWith = ReplaceWith(
        expression =
        "Modifier.onClick(" +
            "enabled = enabled,\n" +
            "matcher = PointerMatcher.mouse(PointerButton.Primary), // add onClick for every required PointerButton\n" +
            "keyboardModifiers = { true }, // e.g { isCtrlPressed }; Remove it to ignore keyboardModifiers\n" +
            "onClick = { onClick() }\n" +
            ")",
        imports = arrayOf(
            "androidx.compose.foundation.onClick",
            "androidx.compose.foundation.PointerMatcher",
            "androidx.compose.ui.input.pointer.PointerButton"
        )
    )
)
fun Modifier.mouseClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    @Suppress("DEPRECATION") onClick: MouseClickScope.() -> Unit
) = composed(
    factory = {
        val onClickState = rememberUpdatedState(onClick)
        val centreOffset = remember { mutableStateOf(Offset.Zero) }
        val currentKeyPressInteractions = remember { mutableMapOf<Key, PressInteraction.Press>() }
        val focusRequester = remember { FocusRequester() }
        val gesture = if (enabled) {
            Modifier.pointerInput(Unit) {
                centreOffset.value = size.center.toOffset()
                detectTapWithContext(
                    onTap = { down, _ ->
                        focusRequester.requestFocus()
                        onClickState.value.invoke(
                            MouseClickScope(
                                down.buttons,
                                down.keyboardModifiers
                            )
                        )
                    }
                )
            }
        } else {
            Modifier
        }
        Modifier
            .focusRequester(focusRequester)
            .genericClickableWithoutGesture(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                indicationScope = rememberCoroutineScope(),
                keyClickOffset = centreOffset,
                enabled = enabled,
                onClickLabel = onClickLabel,
                currentKeyPressInteractions = currentKeyPressInteractions,
                role = role,
                onLongClickLabel = null,
                onLongClick = null,
                onClick = { onClick(EmptyClickContext) }
            )
            .then(gesture)
    },
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
)

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PointerInputScope.detectTapWithContext(
    onTap: ((PointerEvent, PointerEvent) -> Unit)? = null
) {
    awaitEachGesture {
        val down = awaitEventFirstDown().also {
            it.changes.forEach { it.consume() }
        }

        val up = waitForFirstInboundUpOrCancellation()
        if (up != null) {
            up.changes.forEach { it.consume() }
            onTap?.invoke(down, up)
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (
        !event.changes.fastAll { it.changedToDown() }
    )
    return event
}

private suspend fun AwaitPointerEventScope.waitForFirstInboundUpOrCancellation(): PointerEvent? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.fastAll { it.changedToUp() }) {
            // All pointers are up
            return event
        }

        if (event.changes.fastAny {
                it.consumed.downChange || it.isOutOfBounds(size, extendedTouchPadding)
            }
        ) {
            return null // Canceled
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the Main pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.positionChangeConsumed() }) {
            return null
        }
    }
}
