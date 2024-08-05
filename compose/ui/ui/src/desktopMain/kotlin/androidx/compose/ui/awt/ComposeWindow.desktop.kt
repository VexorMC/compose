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
package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.UndecoratedWindowResizer
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowPlacement
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.GraphicsConfiguration
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import java.util.*
import javax.accessibility.Accessible
import javax.swing.JFrame
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * ComposeWindow is a window for building UI using Compose for Desktop.
 * ComposeWindow inherits javax.swing.JFrame.
 *
 * @param graphicsConfiguration the GraphicsConfiguration that is used to construct the new window.
 * If null, the system default GraphicsConfiguration is assumed.
 * @param skiaLayerAnalytics Analytics that helps to know more about SkiaLayer behaviour.
 * SkiaLayer is underlying class used internally to draw Compose content.
 * Implementation usually uses third-party solution to send info to some centralized analytics gatherer.
 */
class ComposeWindow @ExperimentalComposeUiApi constructor(
    graphicsConfiguration: GraphicsConfiguration? = null,
    skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
) : JFrame(graphicsConfiguration) {
    /**
     * ComposeWindow is a window for building UI using Compose for Desktop.
     * ComposeWindow inherits javax.swing.JFrame.
     *
     * @param graphicsConfiguration the GraphicsConfiguration that is used to construct the new window.
     * If null, the system default GraphicsConfiguration is assumed.
     */
    constructor(
        graphicsConfiguration: GraphicsConfiguration? = null
    ) : this(graphicsConfiguration, SkiaLayerAnalytics.Empty)

    private val composePanel = ComposeWindowPanel(
        window = this,
        isUndecorated = ::isUndecorated,
        skiaLayerAnalytics = skiaLayerAnalytics
    )
    private val undecoratedWindowResizer = UndecoratedWindowResizer(this)

    internal val windowContext by composePanel::windowContext
    internal var rootForTestListener by composePanel::rootForTestListener

    // Don't override the accessible context of JFrame, since accessibility work through HardwareLayer
    internal val windowAccessible: Accessible
        get() = composePanel.windowAccessible

    init {
        contentPane.add(composePanel)
    }

    override fun add(component: Component) = composePanel.add(component)

    override fun remove(component: Component) = composePanel.remove(component)

    override fun setComponentOrientation(o: ComponentOrientation?) {
        super.setComponentOrientation(o)

        composePanel.onChangeLayoutDirection(this)
    }

    override fun setLocale(l: Locale?) {
        super.setLocale(l)

        // setLocale is called from JFrame constructor, before ComposeWindow has been initialized
        @Suppress("UNNECESSARY_SAFE_CALL")
        composePanel?.onChangeLayoutDirection(this)
    }

    /**
     * Handler to catch uncaught exceptions during rendering frames, handling events,
     * or processing background Compose operations. If null, then exceptions throw
     * further up the call stack.
     */
    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler? by composePanel::exceptionHandler

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext? by composePanel::compositionLocalContext

    /**
     * Composes the given composable into the ComposeWindow.
     *
     * @param content Composable content of the ComposeWindow.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun setContent(
        content: @Composable FrameWindowScope.() -> Unit
    ) = setContent(
        onPreviewKeyEvent = { false },
        onKeyEvent = { false },
        content = content
    )

    /**
     * Composes the given composable into the ComposeWindow.
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
        onPreviewKeyEvent: (InternalKeyEvent) -> Boolean = { false },
        onKeyEvent: (InternalKeyEvent) -> Boolean = { false },
        content: @Composable FrameWindowScope.() -> Unit
    ) {
        val scope = object : FrameWindowScope {
            override val window: ComposeWindow get() = this@ComposeWindow
        }
        composePanel.setContent(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
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
    var isTransparent: Boolean by composePanel::isWindowTransparent

    var placement: WindowPlacement
        get() = when {
            isFullscreen -> WindowPlacement.Fullscreen
            isMaximized -> WindowPlacement.Maximized
            else -> WindowPlacement.Floating
        }
        set(value) {
            when (value) {
                WindowPlacement.Fullscreen -> {
                    isFullscreen = true
                }
                WindowPlacement.Maximized -> {
                    isMaximized = true
                }
                WindowPlacement.Floating -> {
                    isFullscreen = false
                    isMaximized = false
                }
            }
        }

    /**
     * `true` if the window is in fullscreen mode, `false` otherwise
     */
    private var isFullscreen: Boolean by composePanel::fullscreen

    /**
     * `true` if the window is maximized to fill all available screen space, `false` otherwise
     */
    private var isMaximized: Boolean
        get() = extendedState and MAXIMIZED_BOTH != 0
        set(value) {
            extendedState = if (value) {
                extendedState or MAXIMIZED_BOTH
            } else {
                extendedState and MAXIMIZED_BOTH.inv()
            }
        }

    /**
     * `true` if the window is minimized to the taskbar, `false` otherwise
     */
    var isMinimized: Boolean
        get() = extendedState and ICONIFIED != 0
        set(value) {
            extendedState = if (value) {
                extendedState or ICONIFIED
            } else {
                extendedState and ICONIFIED.inv()
            }
        }

    /**
     * Registers a task to run when the rendering API changes.
     */
    fun onRenderApiChanged(action: () -> Unit) {
        composePanel.onRenderApiChanged(action)
    }

    /**
     * Retrieve underlying platform-specific operating system handle for the root window where
     * ComposeWindow is rendered. Currently returns HWND on Windows, Window on X11 and NSWindow
     * on macOS.
     */
    val windowHandle: Long get() = composePanel.windowHandle

    /**
     * Returns low-level rendering API used for rendering in this ComposeWindow. API is
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
