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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.scene.LocalComposeScene
import androidx.compose.ui.scene.platformContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.ComponentUpdater
import androidx.compose.ui.util.componentListenerRef
import androidx.compose.ui.util.setIcon
import androidx.compose.ui.util.setPositionSafely
import androidx.compose.ui.util.setSizeSafely
import androidx.compose.ui.util.setUndecoratedSafely
import androidx.compose.ui.util.windowListenerRef
import java.awt.Dialog.ModalityType
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog

@Deprecated(
    message = "Replaced by DialogWindow",
    replaceWith = ReplaceWith("DialogWindow(onCloseRequest, state, visible, title, icon, undecorated, transparent, resizable, enabled, focusable, onPreviewKeyEvent, onKeyEvent, content)")
)
@Composable
fun Dialog(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    onKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit
) = DialogWindow(
    onCloseRequest,
    state,
    visible,
    title,
    icon,
    undecorated,
    transparent,
    resizable,
    enabled,
    focusable,
    alwaysOnTop = false,
    onPreviewKeyEvent,
    onKeyEvent,
    content
)

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Replaced by an overload that also takes alwaysOnTop",
)
@Composable
fun DialogWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    onKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit
) {
    DialogWindow(
        onCloseRequest,
        state,
        visible,
        title,
        icon,
        undecorated,
        transparent,
        resizable,
        enabled,
        focusable,
        alwaysOnTop = false,
        onPreviewKeyEvent,
        onKeyEvent,
        content
    )
}

/**
 * Composes platform dialog in the current composition. When Dialog enters the composition,
 * a new platform dialog will be created and receives the focus. When Dialog leaves the
 * composition, dialog will be disposed and closed.
 *
 * Dialog is a modal window. It means it blocks the parent [Window] / [DialogWindow] in which composition
 * context it was created.
 *
 * Usage:
 * ```
 * @Composable
 * fun main() = application {
 *     var isDialogOpen by remember { mutableStateOf(true) }
 *     if (isDialogOpen) {
 *         Dialog(onCloseRequest = { isDialogOpen = false }) {}
 *     }
 * }
 * ```
 * @param onCloseRequest Callback that will be called when the user closes the dialog.
 * Usually in this callback we need to manually tell Compose what to do:
 * - change `isOpen` state of the dialog (which is manually defined)
 * - close the whole application (`onCloseRequest = ::exitApplication` in [ApplicationScope])
 * - don't close the dialog on close request (`onCloseRequest = {}`)
 * @param state The state object to be used to control or observe the dialog's state
 * When size/position is changed by the user, state will be updated.
 * When size/position of the dialog is changed by the application (changing state),
 * the native dialog will update its corresponding properties.
 * If [DialogState.position] is not [WindowPosition.isSpecified], then after the first show on the
 * screen [DialogState.position] will be set to the absolute values.
 * @param visible Is [DialogWindow] visible to user.
 * If `false`:
 * - internal state of [DialogWindow] is preserved and will be restored next time the dialog
 * will be visible;
 * - native resources will not be released. They will be released only when [DialogWindow]
 * will leave the composition.
 * @param title Title in the titlebar of the dialog
 * @param icon Icon in the titlebar of the window (for platforms which support this).
 * On macOs individual windows can't have a separate icon. To change the icon in the Dock,
 * set it via `iconFile` in build.gradle
 * (https://github.com/JetBrains/compose-jb/tree/master/tutorials/Native_distributions_and_local_execution#platform-specific-options)
 * @param undecorated Disables or enables decorations for this window.
 * @param transparent Disables or enables window transparency. Transparency should be set
 * only if window is undecorated, otherwise an exception will be thrown.
 * @param resizable Can dialog be resized by the user (application still can resize the dialog
 * changing [state])
 * @param enabled Can dialog react to input events
 * @param focusable Can dialog receive focus
 * @param alwaysOnTop Should the dialog always be on top of another windows and dialogs
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [InternalKeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content content of the dialog
 */
@Composable
fun DialogWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    onKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    content: @Composable DialogWindowScope.() -> Unit
) {
    val owner = LocalWindow.current

    val currentState by rememberUpdatedState(state)
    val currentTitle by rememberUpdatedState(title)
    val currentIcon by rememberUpdatedState(icon)
    val currentUndecorated by rememberUpdatedState(undecorated)
    val currentTransparent by rememberUpdatedState(transparent)
    val currentResizable by rememberUpdatedState(resizable)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentFocusable by rememberUpdatedState(focusable)
    val currentAlwaysOnTop by rememberUpdatedState(alwaysOnTop)
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)

    val updater = remember(::ComponentUpdater)

    // the state applied to the dialog. exist to avoid races between DialogState changes and the state stored inside the native dialog
    val appliedState = remember {
        object {
            var size: DpSize? = null
            var position: WindowPosition? = null
        }
    }

    val listeners = remember {
        object {
            var windowListenerRef = windowListenerRef()
            var componentListenerRef = componentListenerRef()

            fun removeFromAndClear(window: ComposeDialog) {
                windowListenerRef.unregisterFromAndClear(window)
                componentListenerRef.unregisterFromAndClear(window)
            }
        }
    }

    DialogWindow(
        visible = visible,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        create = {
            val graphicsConfiguration = WindowLocationTracker.lastActiveGraphicsConfiguration
            val dialog = if (owner != null) {
                ComposeDialog(owner, ModalityType.DOCUMENT_MODAL, graphicsConfiguration = graphicsConfiguration)
            } else {
                ComposeDialog(graphicsConfiguration = graphicsConfiguration)
            }
            dialog.apply {
                // close state is controlled by DialogState.isOpen
                defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
                listeners.windowListenerRef.registerWithAndSet(
                    this,
                    object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent?) {
                            currentOnCloseRequest()
                        }
                    }
                )
                listeners.componentListenerRef.registerWithAndSet(
                    this,
                    object : ComponentAdapter() {
                        override fun componentResized(e: ComponentEvent) {
                            currentState.size = DpSize(width.dp, height.dp)
                            appliedState.size = currentState.size
                        }

                        override fun componentMoved(e: ComponentEvent) {
                            currentState.position = WindowPosition(x.dp, y.dp)
                            appliedState.position = currentState.position
                        }
                    }
                )
                WindowLocationTracker.onWindowCreated(this)
            }
        },
        dispose = {
            WindowLocationTracker.onWindowDisposed(it)
            // We need to remove them because AWT can still call them after dispose()
            listeners.removeFromAndClear(it)
            it.dispose()
        },
        update = { dialog ->
            updater.update {
                set(currentTitle, dialog::setTitle)
                set(currentIcon, dialog::setIcon)
                set(currentUndecorated, dialog::setUndecoratedSafely)
                set(currentTransparent, dialog::isTransparent::set)
                set(currentResizable, dialog::setResizable)
                set(currentEnabled, dialog::setEnabled)
                set(currentFocusable, dialog::setFocusableWindowState)
                set(currentAlwaysOnTop, dialog::setAlwaysOnTop)
            }
            if (state.size != appliedState.size) {
                dialog.setSizeSafely(state.size, WindowPlacement.Floating)
                appliedState.size = state.size
            }
            if (state.position != appliedState.position) {
                dialog.setPositionSafely(
                    state.position,
                    WindowPlacement.Floating,
                    platformDefaultPosition = { WindowLocationTracker.getCascadeLocationFor(dialog) }
                )
                appliedState.position = state.position
            }
        },
        content = content
    )
}

@Deprecated(
    message = "Replaced by DialogWindow",
    replaceWith = ReplaceWith("DialogWindow(visible, onPreviewKeyEvent, onKeyEvent, create, dispose, update, content)")
)
@Composable
fun Dialog(
    visible: Boolean = true,
    onPreviewKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    onKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    create: () -> ComposeDialog,
    dispose: (ComposeDialog) -> Unit,
    update: (ComposeDialog) -> Unit = {},
    content: @Composable DialogWindowScope.() -> Unit
) = DialogWindow(
    visible,
    onPreviewKeyEvent,
    onKeyEvent,
    create,
    dispose,
    update,
    content
)

// TODO(demin): fix mouse hover after opening a dialog.
//  When we open a modal dialog, ComposeLayer/mouseExited will
//  never be called for the parent window. See ./gradlew run3
/**
 * Compose [ComposeDialog] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [ComposeDialog] to be composed, and it is also guaranteed to
 * be invoked on the UI thread (Event Dispatch Thread).
 *
 * Once Dialog leaves the composition, [dispose] will be called to free resources that
 * obtained by the [ComposeDialog].
 *
 * Dialog is a modal window. It means it blocks the parent [Window] / [DialogWindow] in which composition
 * context it was created.
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [ComposeDialog] properties depending on state.
 * When state changes, the block will be reexecuted to set the new properties.
 * Note the block will also be ran once right after the [create] block completes.
 *
 * Dialog is needed for creating dialog's that still can't be created with
 * the default Compose function [androidx.compose.ui.window.DialogWindow]
 *
 * @param visible Is [ComposeDialog] visible to user.
 * If `false`:
 * - internal state of [ComposeDialog] is preserved and will be restored next time the dialog
 * will be visible;
 * - native resources will not be released. They will be released only when [DialogWindow]
 * will leave the composition.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [InternalKeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param create The block creating the [ComposeDialog] to be composed.
 * @param dispose The block to dispose [ComposeDialog] and free native resources.
 * Usually it is simple `ComposeDialog::dispose`
 * @param update The callback to be invoked after the layout is inflated.
 * @param content Composable content of the creating dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("unused")
@Composable
fun DialogWindow(
    visible: Boolean = true,
    onPreviewKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    onKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
    create: () -> ComposeDialog,
    dispose: (ComposeDialog) -> Unit,
    update: (ComposeDialog) -> Unit = {},
    content: @Composable DialogWindowScope.() -> Unit
) {
    val compositionLocalContext by rememberUpdatedState(currentCompositionLocalContext)
    val windowExceptionHandlerFactory by rememberUpdatedState(
        LocalWindowExceptionHandlerFactory.current
    )
    val parentPlatformContext = LocalComposeScene.current?.platformContext
    val layoutDirection = LocalLayoutDirection.current
    AwtWindow(
        visible = visible,
        create = {
            create().apply {
                this.rootForTestListener = parentPlatformContext?.rootForTestListener
                this.compositionLocalContext = compositionLocalContext
                this.exceptionHandler = windowExceptionHandlerFactory.exceptionHandler(this)
                setContent(onPreviewKeyEvent, onKeyEvent, content)
            }
        },
        dispose = {
            dispose(it)
        },
        update = {
            it.compositionLocalContext = compositionLocalContext
            it.exceptionHandler = windowExceptionHandlerFactory.exceptionHandler(it)
            it.componentOrientation = layoutDirection.componentOrientation

            val wasDisplayable = it.isDisplayable

            update(it)

            // If displaying for the first time, make sure we draw the first frame before making
            // the dialog visible, to avoid showing the dialog background.
            // It's the responsibility of setSizeSafely to
            // - Make the dialog displayable
            // - Size the dialog and the ComposeLayer correctly, so that we can draw it here
            if (!wasDisplayable && it.isDisplayable) {
                it.contentPane.paint(it.contentPane.graphics)
            }
        },
    )
}

/**
 * Receiver scope which is used by [androidx.compose.ui.window.DialogWindow].
 */
@Stable
interface DialogWindowScope : WindowScope {
    /**
     * [ComposeDialog] that was created inside [androidx.compose.ui.window.DialogWindow].
     */
    override val window: ComposeDialog
}