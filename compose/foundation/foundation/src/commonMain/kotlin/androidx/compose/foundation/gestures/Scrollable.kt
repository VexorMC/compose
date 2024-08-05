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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.FocusedBoundsObserverNode
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec.Companion.DefaultBringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewResponderNode
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 */
@Stable
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = scrollable(
    state = state,
    orientation = orientation,
    enabled = enabled,
    reverseDirection = reverseDirection,
    flingBehavior = flingBehavior,
    interactionSource = interactionSource,
    overscrollEffect = null
)

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the
 * over scrolling logic. Consider using [ScrollableDefaults.overscrollEffect] for the platform
 * look-and-feel.
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have
 * some scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should
 * also apply [androidx.compose.foundation.overscroll] modifier.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 * @param bringIntoViewSpec The configuration that this scrollable should use to perform
 * scrolling when scroll requests are received from the focus system. If null is provided the
 * system will use the behavior provided by [LocalBringIntoViewSpec] which by default has a
 * platform dependent implementation.
 *
 * Note: This API is experimental as it brings support for some experimental features:
 * [overscrollEffect] and [bringIntoViewSpec].
 */
@Stable
@ExperimentalFoundationApi
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null
) = this then ScrollableElement(
    state,
    orientation,
    overscrollEffect,
    enabled,
    reverseDirection,
    flingBehavior,
    interactionSource,
    bringIntoViewSpec
)

@OptIn(ExperimentalFoundationApi::class)
private class ScrollableElement(
    val state: ScrollableState,
    val orientation: Orientation,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
    val bringIntoViewSpec: BringIntoViewSpec?
) : ModifierNodeElement<ScrollableNode>() {
    override fun create(): ScrollableNode {
        return ScrollableNode(
            state,
            overscrollEffect,
            flingBehavior,
            orientation,
            enabled,
            reverseDirection,
            interactionSource,
            bringIntoViewSpec
        )
    }

    override fun update(node: ScrollableNode) {
        node.update(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + bringIntoViewSpec.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is ScrollableElement) return false

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["bringIntoViewSpec"] = bringIntoViewSpec
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class ScrollableNode(
    state: ScrollableState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior?,
    orientation: Orientation,
    enabled: Boolean,
    reverseDirection: Boolean,
    interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec?
) : DragGestureNode(
    canDrag = CanDragCalculation,
    enabled = enabled,
    interactionSource = interactionSource,
    orientationLock = orientation
), ObserverModifierNode, CompositionLocalConsumerModifierNode,
    FocusPropertiesModifierNode, KeyInputModifierNode, SemanticsModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    private val nestedScrollDispatcher = NestedScrollDispatcher()

    private val scrollableContainerNode =
        delegate(ScrollableContainerNode(enabled))

    // Place holder fling behavior, we'll initialize it when the density is available.
    private val defaultFlingBehavior = platformDefaultFlingBehavior()

    private val scrollingLogic = ScrollingLogic(
        scrollableState = state,
        orientation = orientation,
        overscrollEffect = overscrollEffect,
        reverseDirection = reverseDirection,
        flingBehavior = flingBehavior ?: defaultFlingBehavior,
        nestedScrollDispatcher = nestedScrollDispatcher,
    )

    private val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    private val contentInViewNode =
        delegate(
            ContentInViewNode(
                orientation,
                scrollingLogic,
                reverseDirection,
                bringIntoViewSpec
            )
        )

    // Need to wait until onAttach to read the scroll config. Currently this is static, so we
    // don't need to worry about observation / updating this over time.
    private var scrollConfig: ScrollConfig? = null
    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    init {
        /**
         * Nested scrolling
         */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))

        /**
         * Focus scrolling
         */
        delegate(FocusTargetModifierNode())
        delegate(BringIntoViewResponderNode(contentInViewNode))
        delegate(FocusedBoundsObserverNode { contentInViewNode.onFocusBoundsChanged(it) })
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit
    ) {
        with(scrollingLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                forEachDelta {
                    scrollByWithOverscroll(
                        it.delta.singleAxisOffset(),
                        source = UserInput
                    )
                }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = false)
        }
    }

    override fun startDragImmediately(): Boolean {
        return scrollingLogic.shouldScrollImmediately()
    }

    private val onWheelScrollStopped: suspend (velocity: Velocity) -> Unit = { velocity ->
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = true)
        }
    }

    val mouseWheelScrollNode = delegate(
        MouseWheelScrollNode(
            scrollingLogic = scrollingLogic,
            onScrollStopped = onWheelScrollStopped,
            enabled = enabled,
        )
    )

    fun update(
        state: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?
    ) {
        var shouldInvalidateSemantics = false
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainerNode.update(enabled)
            shouldInvalidateSemantics = true
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        val resetPointerInputHandling = scrollingLogic.update(
            scrollableState = state,
            orientation = orientation,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = resolvedFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher
        )

        contentInViewNode.update(
            orientation,
            reverseDirection,
            bringIntoViewSpec
        )

        mouseWheelScrollNode.update(
            enabled = enabled
        )

        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            orientationLock = if (scrollingLogic.isVertical()) Vertical else Horizontal,
            shouldResetPointerInputHandling = resetPointerInputHandling
        )

        if (shouldInvalidateSemantics) {
            clearScrollSemanticsActions()
            invalidateSemantics()
        }
    }

    override fun onAttach() {
        updateDefaultFlingBehavior()
        scrollConfig = platformScrollConfig()
    }

    // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-731/Scrollable-doesnt-react-on-density-changes)
    //  it isn't called, because LocalDensity is staticCompositionLocalOf
    override fun onObservedReadsChanged() {
        // if density changes, update the default fling behavior.
        updateDefaultFlingBehavior()
    }

    private fun updateDefaultFlingBehavior() {
        // monitor change in Density
        observeReads {
            val density = currentValueOf(LocalDensity)
            defaultFlingBehavior.updateDensity(density)
        }
    }

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.canFocus = false
    }

    // Key handler for Page up/down scrolling behavior.
    override fun onKeyEvent(event: InternalKeyEvent): Boolean {
        return if (enabled &&
            (event.key == Key.PageDown || event.key == Key.PageUp) &&
            (event.type == KeyEventType.KeyDown) &&
            (!event.isCtrlPressed)
        ) {

            val scrollAmount: Offset = if (scrollingLogic.isVertical()) {
                val viewportHeight = contentInViewNode.viewportSize.height

                val yAmount = if (event.key == Key.PageUp) {
                    viewportHeight.toFloat()
                } else {
                    -viewportHeight.toFloat()
                }

                Offset(0f, yAmount)
            } else {
                val viewportWidth = contentInViewNode.viewportSize.width

                val xAmount = if (event.key == Key.PageUp) {
                    viewportWidth.toFloat()
                } else {
                    -viewportWidth.toFloat()
                }

                Offset(xAmount, 0f)
            }

            // A coroutine is launched for every individual scroll event in the
            // larger scroll gesture. If we see degradation in the future (that is,
            // a fast scroll gesture on a slow device causes UI jank [not seen up to
            // this point), we can switch to a more efficient solution where we
            // lazily launch one coroutine (with the first event) and use a Channel
            // to communicate the scroll amount to the UI thread.
            coroutineScope.launch {
                scrollingLogic.scroll(scrollPriority = MutatePriority.UserInput) {
                    scrollBy(
                        offset = scrollAmount,
                        source = UserInput
                    )
                }
            }
            true
        } else {
            false
        }
    }

    override fun onPreKeyEvent(event: InternalKeyEvent) = false

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let {
            scrollBy(action = it)
        }

        scrollByOffsetAction?.let {
            scrollByOffset(action = it)
        }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch {
                scrollingLogic.semanticsScrollBy(Offset(x, y))
            }
            true
        }

        scrollByOffsetAction = { offset -> scrollingLogic.semanticsScrollBy(offset) }
    }

    private fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }

    // Forward all PointerInputModifierNode method calls to `mmouseWheelScrollNode.pointerInputNode`
    // See explanation in `MouseWheelScrollNode.pointerInputNode`

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        super.onPointerEvent(pointerEvent, pass, bounds)
        mouseWheelScrollNode.pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        super.onCancelPointerInput()
        mouseWheelScrollNode.pointerInputNode.onCancelPointerInput()
    }

    override fun onDensityChange() {
        super.onDensityChange()
        mouseWheelScrollNode.pointerInputNode.onDensityChange()
    }

    override fun onViewConfigurationChange() {
        super.onViewConfigurationChange()
        mouseWheelScrollNode.pointerInputNode.onViewConfigurationChange()
    }
}

/**
 * Contains the default values used by [scrollable]
 */
object ScrollableDefaults {

    /**
     * Create and remember default [FlingBehavior] that will represent natural fling curve.
     */
    @Composable
    fun flingBehavior(): FlingBehavior = rememberPlatformDefaultFlingBehavior()

    /**
     * Create and remember default [OverscrollEffect] that will be used for showing over scroll
     * effects.
     */
    @Composable
    @ExperimentalFoundationApi
    fun overscrollEffect(): OverscrollEffect {
        return rememberOverscrollEffect()
    }

    /**
     * Used to determine the value of `reverseDirection` parameter of [Modifier.scrollable]
     * in scrollable layouts.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     *
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Orientation.Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
    }

    /**
     * A default implementation for [BringIntoViewSpec] that brings a child into view
     * using the least amount of effort.
     */
    @Deprecated(
        "This has been replaced by composition locals LocalBringIntoViewSpec",
        replaceWith = ReplaceWith(
            "LocalBringIntoView.current",
            "androidx.compose.foundation.gestures.LocalBringIntoViewSpec"
        )
    )
    @ExperimentalFoundationApi
    fun bringIntoViewSpec(): BringIntoViewSpec = DefaultBringIntoViewSpec
}

internal interface ScrollConfig {

    /**
     * Enables animated transition of scroll on mouse wheel events.
     */
    val isSmoothScrollingEnabled: Boolean
        get() = true

    fun isPreciseWheelScroll(event: PointerEvent): Boolean = false

    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

internal expect fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig

private val CanDragCalculation: (PointerInputChange) -> Boolean =
    { change -> change.type != PointerType.Mouse }

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class ScrollingLogic(
    var scrollableState: ScrollableState,
    private var orientation: Orientation,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior,
    private var reverseDirection: Boolean,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
) {

    fun Float.toOffset(): Offset = when {
        this == 0f -> Offset.Zero
        orientation == Horizontal -> Offset(this, 0f)
        else -> Offset(0f, this)
    }

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Float.toVelocity(): Velocity = when {
        this == 0f -> Velocity.Zero
        orientation == Horizontal -> Velocity(this, 0f)
        else -> Velocity(0f, this)
    }

    private fun Velocity.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    private fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    private fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    private var latestScrollSource = UserInput
    private var outerStateScope = NoOpScrollScope

    private val nestedScrollScope = object : NestedScrollScope {
        override fun scrollBy(offset: Offset, source: NestedScrollSource): Offset {
            return with(outerStateScope) {
                performScroll(offset, source)
            }
        }

        override fun scrollByWithOverscroll(offset: Offset, source: NestedScrollSource): Offset {
            latestScrollSource = source
            val overscroll = overscrollEffect
            return if (overscroll != null && shouldDispatchOverscroll) {
                overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
            } else {
                with(outerStateScope) {
                    performScroll(offset, source)
                }
            }
        }
    }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) {
            performScroll(delta, latestScrollSource)
        }
    }

    private fun ScrollScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll =
            nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        val singleAxisDeltaForSelfScroll =
            scrollAvailableAfterPreScroll.singleAxisOffset().reverseIfNeeded().toFloat()

        // Consume on a single axis.
        val consumedBySelfScroll =
            scrollBy(singleAxisDeltaForSelfScroll).toOffset().reverseIfNeeded()

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll = nestedScrollDispatcher.dispatchPostScroll(
            consumedBySelfScroll,
            deltaAvailableAfterScroll,
            source
        )
        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val shouldDispatchOverscroll
        get() = scrollableState.canScrollForward || scrollableState.canScrollBackward

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            dispatchRawDelta(scroll)
        }
    }

    fun dispatchRawDelta(scroll: Offset): Offset {
        return scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
            .reverseIfNeeded().toOffset()
    }

    suspend fun onScrollStopped(
        initialVelocity: Velocity,
        isMouseWheel: Boolean
    ) {
        if (isMouseWheel && !flingBehavior.shouldBeTriggeredByMouseWheel) {
            return
        }
        val availableVelocity = initialVelocity.singleAxisVelocity()

        scroll {
            val performFling: suspend (Velocity) -> Velocity = { velocity ->
                val preConsumedByParent = nestedScrollDispatcher
                    .dispatchPreFling(velocity)
                val available = velocity - preConsumedByParent
                val velocityLeft = doFlingAnimation(available)
                val consumedPost =
                    nestedScrollDispatcher.dispatchPostFling(
                        (available - velocityLeft),
                        velocityLeft
                    )
                val totalLeft = velocityLeft - consumedPost
                velocity - totalLeft
            }

            val overscroll = overscrollEffect
            if (overscroll != null && shouldDispatchOverscroll) {
                overscroll.applyToFling(availableVelocity, performFling)
            } else {
                performFling(availableVelocity)
            }
        }
    }

    suspend fun NestedScrollScope.doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available

        val nestedScrollScope = this
        val reverseScope = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float {
                return nestedScrollScope.scrollByWithOverscroll(
                    offset = pixels.toOffset().reverseIfNeeded(),
                    source = SideEffect
                ).toFloat().reverseIfNeeded()
            }
        }
        with(reverseScope) {
            with(flingBehavior) {
                result = result.update(
                    performFling(available.toFloat().reverseIfNeeded()).reverseIfNeeded()
                )
            }
        }
        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress ||
            overscrollEffect?.isInProgress ?: false
    }

    /**
     * Opens a scrolling session with nested scrolling and overscroll support.
     */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit
    ) {
        scrollableState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /**
     * @return true if the pointer input should be reset
     */
    fun update(
        scrollableState: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollableState != scrollableState) {
            this.scrollableState = scrollableState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }

    fun isVertical(): Boolean = orientation == Vertical
}

private val NoOpScrollScope: ScrollScope = object : ScrollScope {
    override fun scrollBy(pixels: Float): Float = pixels
}

private class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollingLogic,
    var enabled: Boolean
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = if (enabled) {
        scrollingLogic.performRawScroll(available)
    } else {
        Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        return if (enabled) {
            var velocityLeft: Velocity = available
            with(scrollingLogic) {
                scroll {
                    velocityLeft = doFlingAnimation(available)
                }
            }
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

/**
 * Compatibility interface for default fling behaviors that depends on [Density].
 */
internal interface ScrollableDefaultFlingBehavior : FlingBehavior {
    /**
     * Update the internal parameters of FlingBehavior in accordance with the new [androidx.compose.ui.unit.Density] value.
     *
     * @param density new density value.
     */
    fun updateDensity(density: Density) = Unit
}

/**
 * TODO Move it to public interface
 *  Currently, default [FlingBehavior] is not triggered at all to avoid unexpected effects
 *  during regular scrolling. However, custom one must be triggered because it's used not
 *  only for "inertia", but also for snapping in [androidx.compose.foundation.pager.Pager] or
 *  [androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior].
 */
private val FlingBehavior.shouldBeTriggeredByMouseWheel
    get() = this !is ScrollableDefaultFlingBehavior

/**
 * This method returns [ScrollableDefaultFlingBehavior] whose density will be managed by the
 * [ScrollableElement] because it's not created inside [Composable] context.
 * This is different from [rememberPlatformDefaultFlingBehavior] which creates [FlingBehavior] whose density
 * depends on [LocalDensity] and is automatically resolved.
 */
internal expect fun platformDefaultFlingBehavior(): ScrollableDefaultFlingBehavior

@Composable
internal expect fun rememberPlatformDefaultFlingBehavior(): FlingBehavior

internal class DefaultFlingBehavior(
    var flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale
) : ScrollableDefaultFlingBehavior {

    // For Testing
    var lastAnimationCycleCount = 0

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        lastAnimationCycleCount = 0
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                val animationState = AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity,
                )
                try {
                    animationState.animateDecay(flingDecay) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                        lastAnimationCycleCount++
                    }
                } catch (exception: CancellationException) {
                    velocityLeft = animationState.velocity
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }

    override fun updateDensity(density: Density) {
        flingDecay = splineBasedDecay(density)
    }
}

private const val DefaultScrollMotionDurationScaleFactor = 1f
internal val DefaultScrollMotionDurationScale = object : MotionDurationScale {
    override val scaleFactor: Float
        get() = DefaultScrollMotionDurationScaleFactor
}

/**
 * (b/311181532): This could not be flattened so we moved it to TraversableNode, but ideally
 * ScrollabeNode should be the one to be travesable.
 */
internal class ScrollableContainerNode(enabled: Boolean) :
    Modifier.Node(),
    TraversableNode {
    override val traverseKey: Any = TraverseKey

    var enabled: Boolean = enabled
        private set

    companion object TraverseKey

    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}

internal val UnityDensity = object : Density {
    override val density: Float
        get() = 1f
    override val fontScale: Float
        get() = 1f
}

/**
 * A scroll scope for nested scrolling and overscroll support.
 */
internal interface NestedScrollScope {
    fun scrollBy(
        offset: Offset,
        source: NestedScrollSource
    ): Offset

    fun scrollByWithOverscroll(
        offset: Offset,
        source: NestedScrollSource
    ): Offset
}

/**
 * Scroll deltas originating from the semantics system. Should be dispatched as an animation
 * driven event.
 */
private suspend fun ScrollingLogic.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = 0f
    scroll(scrollPriority = MutatePriority.Default) {
        animate(0f, offset.toFloat()) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed =
                scrollBy(
                    offset = delta.reverseIfNeeded().toOffset(),
                    source = UserInput
                ).toFloat().reverseIfNeeded()
            previousValue += consumed
        }
    }
    return previousValue.toOffset()
}
