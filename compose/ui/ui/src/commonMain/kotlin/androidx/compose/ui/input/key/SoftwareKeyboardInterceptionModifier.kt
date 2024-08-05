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

package androidx.compose.ui.input.key

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events before they are sent to the software keyboard. This
 * can be used to intercept key input from a DPad, or physical keyboard connected to the device and
 * is not applicable to input that is sent to the soft keyboard via spell check or autocomplete.
 *
 * @param onInterceptKeyBeforeSoftKeyboard This callback is invoked when the user interacts with
 * the hardware keyboard. While implementing this callback, return true to stop propagation of this
 * event. If you return false, the key event will be sent to this
 * [SoftKeyboardInterceptionModifierNode]'s parent, and ultimately to the software keyboard.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
@ExperimentalComposeUiApi
fun Modifier.onInterceptKeyBeforeSoftKeyboard(
    onInterceptKeyBeforeSoftKeyboard: (InternalKeyEvent) -> Boolean
): Modifier = this then SoftKeyboardInterceptionElement(
    onKeyEvent = onInterceptKeyBeforeSoftKeyboard,
    onPreKeyEvent = null
)

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events before they are sent to the software keyboard. This
 * can be used to intercept key input from a DPad, or physical keyboard connected to the device and
 * is not applicable to input that is sent to the soft keyboard via spell check or autocomplete.
 * This modifier is similar to [onInterceptKeyBeforeSoftKeyboard], but allows a parent composable
 * to intercept the hardware key event before any child.
 *
 * @param onPreInterceptKeyBeforeSoftKeyboard This callback is invoked when the user interacts
 * with the hardware keyboard. It gives ancestors of a focused component the chance to intercept a
 * [InternalKeyEvent]. Return true to stop propagation of this event. If you return false, the key event
 * will be sent to this [SoftKeyboardInterceptionModifierNode]'s child. If none of the children
 * consume the event, it will be sent back up to the root [KeyInputModifierNode] using the
 * onKeyEvent callback, and ultimately to the software keyboard.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
@ExperimentalComposeUiApi
fun Modifier.onPreInterceptKeyBeforeSoftKeyboard(
    onPreInterceptKeyBeforeSoftKeyboard: (InternalKeyEvent) -> Boolean,
): Modifier = this then SoftKeyboardInterceptionElement(
    onKeyEvent = null,
    onPreKeyEvent = onPreInterceptKeyBeforeSoftKeyboard
)

private data class SoftKeyboardInterceptionElement(
    val onKeyEvent: ((InternalKeyEvent) -> Boolean)?,
    val onPreKeyEvent: ((InternalKeyEvent) -> Boolean)?
) : ModifierNodeElement<InterceptedKeyInputNode>() {
    override fun create() = InterceptedKeyInputNode(
        onEvent = onKeyEvent,
        onPreEvent = onPreKeyEvent
    )

    override fun update(node: InterceptedKeyInputNode) {
        node.onEvent = onKeyEvent
        node.onPreEvent = onPreKeyEvent
    }

    override fun InspectorInfo.inspectableProperties() {
        onKeyEvent?.let {
            name = "onKeyToSoftKeyboardInterceptedEvent"
            properties["onKeyToSoftKeyboardInterceptedEvent"] = it
        }
        onPreKeyEvent?.let {
            name = "onPreKeyToSoftKeyboardInterceptedEvent"
            properties["onPreKeyToSoftKeyboardInterceptedEvent"] = it
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class InterceptedKeyInputNode(
    var onEvent: ((InternalKeyEvent) -> Boolean)?,
    var onPreEvent: ((InternalKeyEvent) -> Boolean)?
) : SoftKeyboardInterceptionModifierNode, Modifier.Node() {
    override fun onInterceptKeyBeforeSoftKeyboard(event: InternalKeyEvent): Boolean =
        onEvent?.invoke(event) ?: false
    override fun onPreInterceptKeyBeforeSoftKeyboard(event: InternalKeyEvent): Boolean =
        onPreEvent?.invoke(event) ?: false
}
