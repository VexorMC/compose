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
package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.UndecoratedWindowResizer
import androidx.compose.ui.window.WindowExceptionHandler
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Frame
import java.awt.GraphicsConfiguration
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import java.util.*
import javax.swing.JDialog
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * ComposeDialog is a dialog for building UI using Compose for Desktop.
 * ComposeDialog inherits javax.swing.JDialog.
 */
class ComposeDialog : JDialog {
    private val skiaLayerAnalytics: SkiaLayerAnalytics
    private val composePanel: ComposeWindowPanel

    internal var rootForTestListener
        get() = composePanel.rootForTestListener
        set(value) { composePanel.rootForTestListener = value }

    private fun createComposePanel() = ComposeWindowPanel(
        window = this,
        isUndecorated = ::isUndecorated,
        skiaLayerAnalytics = skiaLayerAnalytics,
    )

    constructor(
        owner: Window?,
        modalityType: ModalityType = ModalityType.MODELESS,
        graphicsConfiguration: GraphicsConfiguration? = null
    ) : super(owner, "", modalityType, graphicsConfiguration) {
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty
        composePanel = createComposePanel()
        contentPane.add(composePanel)
    }

    /**
     * ComposeDialog is a dialog for building UI using Compose for Desktop.
     * ComposeDialog inherits javax.swing.JDialog.
     *
     * @param skiaLayerAnalytics Analytics that helps to know more about SkiaLayer behaviour.
     * SkiaLayer is underlying class used internally to draw Compose content.
     * Implementation usually uses third-party solution to send info to some centralized analytics gatherer.
     */
    @ExperimentalComposeUiApi
    constructor(
        owner: Window?,
        modalityType: ModalityType = ModalityType.MODELESS,
        graphicsConfiguration: GraphicsConfiguration? = null,
        skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty
    ) : super(owner, "", modalityType, graphicsConfiguration) {
        this.skiaLayerAnalytics = skiaLayerAnalytics
        composePanel = createComposePanel()
        contentPane.add(composePanel)
    }

    /**
     * ComposeDialog is a dialog for building UI using Compose for Desktop.
     * ComposeDialog inherits javax.swing.JDialog.
     *
     * @param skiaLayerAnalytics Analytics that helps to know more about SkiaLayer behaviour.
     * SkiaLayer is underlying class used internally to draw Compose content.
     * Implementation usually uses third-party solution to send info to some centralized analytics gatherer.
     */
    @ExperimentalComposeUiApi
    constructor(
        skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty
    ) : super() {
        this.skiaLayerAnalytics = skiaLayerAnalytics
        composePanel = createComposePanel()
        contentPane.add(composePanel)
    }

    @Deprecated("Use the constructor with setting owner explicitly. Will be removed in 1.3")
    constructor(
        modalityType: ModalityType = ModalityType.MODELESS
    ) : super(null, modalityType) {
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty
        composePanel = createComposePanel()
        contentPane.add(composePanel)
    }

    constructor(graphicsConfiguration: GraphicsConfiguration? = null) :
        super(null as Frame?, "", false, graphicsConfiguration) {
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty
        composePanel = createComposePanel()
        contentPane.add(composePanel)
    }

    // don't replace super() by super(null, ModalityType.MODELESS), because
    // this constructor creates an icon in the taskbar.
    // Dialog's shouldn't be appeared in the taskbar.
    constructor() : super() {
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty
        composePanel = createComposePanel()
        contentPane.add(composePanel)
    }

    private val undecoratedWindowResizer = UndecoratedWindowResizer(this)

    override fun add(component: Component) = composePanel.add(component)

    override fun remove(component: Component) = composePanel.remove(component)

    override fun setComponentOrientation(o: ComponentOrientation?) {
        super.setComponentOrientation(o)

        composePanel.onChangeLayoutDirection(this)
    }

    override fun setLocale(l: Locale?) {
        super.setLocale(l)

        // setLocale is called from JFrame constructor, before ComposeDialog has been initialized
        @Suppress("UNNECESSARY_SAFE_CALL")
        composePanel?.onChangeLayoutDirection(this)
    }

    /**
     * Composes the given composable into the ComposeDialog.
     *
     * @param content Composable content of the ComposeDialog.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun setContent(
        content: @Composable DialogWindowScope.() -> Unit
    ) = setContent(
        onPreviewKeyEvent = { false },
        onKeyEvent = { false },
        content = content
    )

    /**
     * Handler to catch uncaught exceptions during rendering frames, handling events,
     * or processing background Compose operations. If null, then exceptions throw
     * further up the call stack.
     */
    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler?
        get() = composePanel.exceptionHandler
        set(value) {
            composePanel.exceptionHandler = value
        }

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext?
        get() = composePanel.compositionLocalContext
        set(value) {
            composePanel.compositionLocalContext = value
        }

    /**
     * Composes the given composable into the ComposeDialog.
     *
     * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. It gives ancestors of a focused component the chance to intercept a [InternalKeyEvent].
     * Return true to stop propagation of this event. If you return false, the key event will be
     * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
     * it will be sent back up to the root using the onKeyEvent callback.
     * @param onKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. While implementing this callback, return true to stop propagation of this event.
     * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
     * @param content Composable content of the ComposeWindow.
     */
    @ExperimentalComposeUiApi
    fun setContent(
        onPreviewKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
        onKeyEvent: ((InternalKeyEvent) -> Boolean) = { false },
        content: @Composable DialogWindowScope.() -> Unit
    ) {
        val scope = object : DialogWindowScope {
            override val window: ComposeDialog get() = this@ComposeDialog
        }
        composePanel.setContent(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
            modifier = Modifier.semantics { dialog() },
        ) {
            scope.content()
            undecoratedWindowResizer.Content(
                modifier = Modifier.layoutId("UndecoratedWindowResizer")
            )
        }
    }

    override fun dispose() {
        composePanel.dispose()
        super.dispose()
    }

    override fun setUndecorated(value: Boolean) {
        super.setUndecorated(value)
        undecoratedWindowResizer.enabled = isUndecorated && isResizable
    }

    override fun setResizable(value: Boolean) {
        super.setResizable(value)
        undecoratedWindowResizer.enabled = isUndecorated && isResizable
    }

    /**
     * `true` if background of the window is transparent, `false` otherwise
     * Transparency should be set only if window is not showing and `isUndecorated` is set to
     * `true`, otherwise AWT will throw an exception.
     */
    var isTransparent: Boolean
        get() = composePanel.isWindowTransparent
        set(value) {
            composePanel.isWindowTransparent = value
        }

    /**
     * Registers a task to run when the rendering API changes.
     */
    fun onRenderApiChanged(action: () -> Unit) {
        composePanel.onRenderApiChanged(action)
    }

    /**
     * Retrieve underlying platform-specific operating system handle for the root window where
     * ComposeDialog is rendered. Currently returns HWND on Windows, Window on X11 and NSWindow
     * on macOS.
     */
    val windowHandle: Long get() = composePanel.windowHandle

    /**
     * Returns low-level rendering API used for rendering in this ComposeDialog. API is
     * automatically selected based on operating system, graphical hardware and `SKIKO_RENDER_API`
     * environment variable.
     */
    val renderApi: GraphicsApi get() = composePanel.renderApi

    // We need overridden listeners because we mix Swing and AWT components in the
    // org.jetbrains.skiko.SkiaLayer, they don't work well together.
    // TODO(demin): is it possible to fix that without overriding?

    override fun addMouseListener(listener: MouseListener) =
        composePanel.addMouseListener(listener)

    override fun removeMouseListener(listener: MouseListener) =
        composePanel.removeMouseListener(listener)

    override fun addMouseMotionListener(listener: MouseMotionListener) =
        composePanel.addMouseMotionListener(listener)

    override fun removeMouseMotionListener(listener: MouseMotionListener) =
        composePanel.removeMouseMotionListener(listener)

    override fun addMouseWheelListener(listener: MouseWheelListener) =
        composePanel.addMouseWheelListener(listener)

    override fun removeMouseWheelListener(listener: MouseWheelListener) =
        composePanel.removeMouseWheelListener(listener)
}
