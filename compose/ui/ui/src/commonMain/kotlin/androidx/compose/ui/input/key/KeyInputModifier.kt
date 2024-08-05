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

package androidx.compose.ui.input.key

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events when it (or one of its children) is focused.
 *
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware keyboard.
 * While implementing this callback, return true to stop propagation of this event. If you return
 * false, the key event will be sent to this [onKeyEvent]'s parent.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
fun Modifier.onKeyEvent(
    onKeyEvent: (InternalKeyEvent) -> Boolean
): Modifier = this then KeyInputElement(onKeyEvent = onKeyEvent, onPreKeyEvent = null)

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events when it (or one of its children) is focused.
 *
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [InternalKeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be sent
 * to this [onPreviewKeyEvent]'s child. If none of the children consume the event, it will be
 * sent back up to the root [KeyInputModifierNode] using the onKeyEvent callback.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
fun Modifier.onPreviewKeyEvent(
    onPreviewKeyEvent: (InternalKeyEvent) -> Boolean
): Modifier = this then KeyInputElement(onKeyEvent = null, onPreKeyEvent = onPreviewKeyEvent)

internal data class KeyInputElement(
    val onKeyEvent: ((InternalKeyEvent) -> Boolean)?,
    val onPreKeyEvent: ((InternalKeyEvent) -> Boolean)?
) : ModifierNodeElement<KeyInputNode>() {
    override fun create() = KeyInputNode(onKeyEvent, onPreKeyEvent)

    override fun update(node: KeyInputNode) {
        node.onEvent = onKeyEvent
        node.onPreEvent = onPreKeyEvent
    }

    override fun InspectorInfo.inspectableProperties() {
        onKeyEvent?.let {
            name = "onKeyEvent"
            properties["onKeyEvent"] = it
        }
        onPreKeyEvent?.let {
            name = "onPreviewKeyEvent"
            properties["onPreviewKeyEvent"] = it
        }
    }
}

internal class KeyInputNode(
    var onEvent: ((InternalKeyEvent) -> Boolean)?,
    var onPreEvent: ((InternalKeyEvent) -> Boolean)?
) : KeyInputModifierNode, Modifier.Node() {
    override fun onKeyEvent(event: InternalKeyEvent): Boolean = this.onEvent?.invoke(event) ?: false
    override fun onPreKeyEvent(event: InternalKeyEvent): Boolean = this.onPreEvent?.invoke(event) ?: false
}
