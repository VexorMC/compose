/*
 * Copyright 2024 The Android Open Source Project
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

package dev.lunasa

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.lunasa.lwjgl.ComposeLWJGL
import kotlin.system.exitProcess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat.Companion.GR_GL_RGBA8
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skiko.FrameDispatcher
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.system.MemoryUtil.NULL

@OptIn(InternalComposeUiApi::class, ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main() {
    var width = 1280
    var height = 720

    glfwInit()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    val windowHandle: Long = glfwCreateWindow(width, height, "Compose LWJGL Demo", NULL, NULL)
    glfwMakeContextCurrent(windowHandle)
    glfwSwapInterval(1)

    GL.createCapabilities()

    val context = DirectContext.makeGL()
    var surface = createSurface(width, height, context) // Skia Surface, bound to the OpenGL framebuffer
    val glfwDispatcher = GlfwCoroutineDispatcher() // a custom coroutine dispatcher, in which Compose will run

    glfwSetWindowCloseCallback(windowHandle) {
        glfwDispatcher.stop()
    }

    lateinit var composeScene: ComposeScene

    ComposeLWJGL.enableGlobalSnapshotManager = true
    ComposeLWJGL.globalSnapshotManagerDispatcher = newSingleThreadContext("")

    fun render() {
        surface.canvas.clear(Color.WHITE)
        composeScene.size = IntSize(width, height)
        composeScene.render(surface.canvas.asComposeCanvas(), System.nanoTime())

        context.flush()
        glfwSwapBuffers(windowHandle)
    }

    val frameDispatcher = FrameDispatcher(glfwDispatcher) { render() }

    val density = Density(glfwGetWindowContentScale(windowHandle))
    composeScene = CanvasLayersComposeScene(coroutineContext = glfwDispatcher, invalidate = frameDispatcher::scheduleFrame)
    composeScene.density = density

    glfwSetWindowSizeCallback(windowHandle) { _, windowWidth, windowHeight ->
        width = windowWidth
        height = windowHeight
        surface.close()
        surface = createSurface(width, height, context)

        glfwSwapInterval(0)
        render()
        glfwSwapInterval(1)
    }

    composeScene.subscribeToGLFWEvents(windowHandle)
    composeScene.setContent { App() }
    glfwShowWindow(windowHandle)

    glfwDispatcher.runLoop()
    composeScene.close()
    glfwDestroyWindow(windowHandle)

    exitProcess(0)
}

private fun createSurface(width: Int, height: Int, context: DirectContext): Surface {
    val fbId = GL11.glGetInteger(GL_FRAMEBUFFER_BINDING)
    val renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, fbId, GR_GL_RGBA8)
    return Surface.makeFromBackendRenderTarget(
        context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB
    )!!
}

private fun glfwGetWindowContentScale(window: Long): Float {
    return FloatArray(1).also {
        glfwGetWindowContentScale(window, it, FloatArray(1))
    }[0]
}