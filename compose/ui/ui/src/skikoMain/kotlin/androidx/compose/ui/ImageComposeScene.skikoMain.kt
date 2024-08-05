/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.currentNanoTime

/**
 * Render Compose [content] into an [Image]
 *
 * @param width The width of the content.
 * @param height The height of the content.
 * @param density Density of the content which will be used to convert [dp] units.
 * @param content Composable content which needed to be rendered.
 */
fun renderComposeScene(
    width: Int,
    height: Int,
    density: Density = Density(1f),
    content: @Composable () -> Unit
): Image = ImageComposeScene(
    width = width,
    height = height,
    density = density,
    content = content
).use { it.render() }

/**
 * Executes the given [block] function on this [ImageComposeScene] and then closes it down
 * correctly whether an exception is thrown or not.
 *
 * @param block a function to process this [ImageComposeScene].
 * @return the result of [block] function invoked on this [ImageComposeScene].
 */
inline fun <R> ImageComposeScene.use(
    block: (ImageComposeScene) -> R
): R {
    return try {
        block(this)
    } finally {
        close()
    }
}

/**
 * A virtual container that encapsulates Compose UI content with ability to draw it into an image.
 *
 * To set content, use `content` parameter of the constructor, or [setContent] method.
 *
 * To draw content into an image, use [render] method.
 *
 * After [ImageComposeScene] will no longer needed, you should call [close] method, so all resources
 * and subscriptions will be properly closed. Otherwise there can be a memory leak.
 *
 * Instead of calling [close] manually, you can use the helper function [use],
 * it will close the scene for you.
 *
 * [ImageComposeScene] doesn't support concurrent read/write access from different threads.
 *
 * @param width The width of the content.
 * @param height The height of the content.
 * @param density Density of the content which will be used to convert [dp] units.
 * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
 * [rememberCoroutineScope]) and run recompositions.
 * @param content Composable content which needed to be rendered.
 */
@OptIn(InternalComposeUiApi::class)
class ImageComposeScene @ExperimentalComposeUiApi constructor(
    width: Int,
    height: Int,
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    content: @Composable () -> Unit = {},
) {

    constructor(
        width: Int,
        height: Int,
        density: Density = Density(1f),
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        content: @Composable () -> Unit = {},
    ): this(
        width,
        height,
        density,
        LayoutDirection.Ltr,
        coroutineContext,
        content
    )

    private val surface = Surface.makeRasterN32Premul(width, height)

    private val imageSize = IntSize(width, height)

    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
        containerSize = imageSize
    }

    private val _platformContext = object : PlatformContext by PlatformContext.Empty {
        override val windowInfo: WindowInfo
            get() = _windowInfo
    }

    private val _sceneContext = object : ComposeSceneContext {
        override val platformContext: PlatformContext
            get() = _platformContext
    }

    private val scene = CanvasLayersComposeScene(
        density = density,
        layoutDirection = layoutDirection,
        size = imageSize,
        coroutineContext = coroutineContext,
        composeSceneContext = _sceneContext
    ).also {
        it.setContent(content = content)
    }

    /**
     * The default direction of layout for content.
     */
    @ExperimentalComposeUiApi
    var layoutDirection: LayoutDirection by scene::layoutDirection

    /**
     * Close all resources and subscriptions. Not calling this method when [ImageComposeScene] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [ImageComposeScene].
     */
    fun close(): Unit = scene.close()

    @Deprecated(
        message = "The scene isn't tracking list of roots anymore",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("SkiaRootForTest.onRootCreatedCallback")
    )
    val roots: Set<RootForTest>
        get() = throw NotImplementedError()

    /**
     * Constraints used to measure and layout content.
     */
    var constraints: Constraints
        get() = scene.size?.toConstraints() ?: Constraints()
        set(value) { scene.size = value.toIntSize() }

    /**
     * Returns true if there are pending recompositions, renders or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations() = scene.hasInvalidations()

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ImageComposeScene]
     */
    fun setContent(content: @Composable () -> Unit): Unit =
        scene.setContent(content = content)

    /**
     * Returns the current content size
     */
    @Deprecated("Use calculateContentSize() instead", replaceWith = ReplaceWith("calculateContentSize()"))
    val contentSize: IntSize
        get() = scene.calculateContentSize()

    /**
     * Returns the current content size in infinity constraints.
     *
     * @throws IllegalStateException when [ComposeScene] content has lazy layouts without maximum size bounds
     * (e.g. LazyColumn without maximum height).
     */
    @ExperimentalComposeUiApi
    fun calculateContentSize(): IntSize {
        return scene.calculateContentSize()
    }

    /**
     * Render the current content into an image. [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(nanoTime: Long = 0): Image {
        surface.canvas.clear(Color.TRANSPARENT)
        scene.render(surface.canvas.asComposeCanvas(), nanoTime)
        return surface.makeImageSnapshot()
    }

    /**
     * Render the current content into an image. [time] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    @ExperimentalTime
    fun render(time: Duration): Image =
        render(time.toLong(NANOSECONDS))

    /**
     * Send pointer event to the content.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param position The [Offset] of the current pointer event, relative to the content.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons) after the event.
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param nativeEvent The original native event.
     * @param button Represents the index of a button which state changed in this event. It's null
     * when there was no change of the buttons state or when button is not applicable (e.g. touch event).
     */
    fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = currentNanoTime() / 1_000_000L,
        type: PointerType = PointerType.Mouse,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
        button: PointerButton? = null
    ): Unit = scene.sendPointerEvent(
        eventType, position, scrollDelta, timeMillis, type, buttons, keyboardModifiers, nativeEvent, button
    )

    /**
     * Send pointer event to the content. The more detailed version of [sendPointerEvent] that can accept
     * multiple pointers.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param pointers The current pointers with position relative to the content.
     * There can be multiple pointers, for example, if we use Touch and touch screen with multiple fingers.
     * Contains only the state of the active pointers.
     * Touch that is released still considered as active on PointerEventType.Release event (but with pressed=false). It
     * is no longer active after that, and shouldn't be passed to the scene.
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons) after the event.
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param nativeEvent The original native event.
     * @param button Represents the index of a button which state changed in this event. It's null
     * when there was no change of the buttons state or when button is not applicable (e.g. touch event).
     */
    @ExperimentalComposeUiApi
    fun sendPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons = PointerButtons(),
        keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    ): Unit = scene.sendPointerEvent(
        eventType, pointers, buttons, keyboardModifiers, scrollDelta, timeMillis, nativeEvent, button
    )

    /**
     * Send [InternalKeyEvent] to the content.
     * @return true if the event was consumed by the content
     */
    fun sendKeyEvent(event: InternalKeyEvent): Boolean = scene.sendKeyEvent(event)
}

private fun Constraints.toIntSize() =
    if (maxWidth != Constraints.Infinity || maxHeight != Constraints.Infinity) {
        IntSize(width = maxWidth, height = maxHeight)
    } else {
        null
    }

private fun IntSize.toConstraints() = Constraints(maxWidth = width, maxHeight = height)
