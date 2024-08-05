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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.PlatformMagnifierFactory
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextItem
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

internal actual fun isCopyKeyEvent(internalKeyEvent: InternalKeyEvent) =
    platformDefaultKeyMapping.map(internalKeyEvent) == KeyCommand.COPY

// We use composed{} to read a local, but don't provide inspector info because the underlying
// magnifier modifier provides more meaningful inspector info.
internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier {
    // Avoid tracking animation state on older Android versions that don't support magnifiers.
    if (!isPlatformMagnifierSupported()) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }
        animatedSelectionMagnifier(
            magnifierCenter = {
                calculateSelectionMagnifierCenterAndroid(manager, magnifierSize)
            },
            platformMagnifier = { center ->
                Modifier
                    .magnifier(
                        sourceCenter = { center() },
                        onSizeChanged = { size ->
                            magnifierSize = with(density) {
                                IntSize(size.width.roundToPx(), size.height.roundToPx())
                            }
                        },
                        useTextDefault = true,
                        platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform()
                    )
            }
        )
    }
}

internal fun SelectionManager.contextMenuBuilder(
    state: ContextMenuState,
): ContextMenuScope.() -> Unit = {
    listOf(
        TextItem(
            state = state,
            label = TextContextMenuItems.Copy,
            enabled = isNonEmptySelection(),
        ) { copy() },
        TextItem(
            state = state,
            label = TextContextMenuItems.SelectAll,
            enabled = !isEntireContainerSelected(),
        ) { selectAll() },
    )
}
