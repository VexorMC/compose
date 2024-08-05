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

import kotlinx.coroutines.CoroutineDispatcher
import org.lwjgl.glfw.GLFW
import kotlin.coroutines.CoroutineContext

class GlfwCoroutineDispatcher : CoroutineDispatcher() {
    private val tasks = mutableListOf<Runnable>()
    private val tasksCopy = mutableListOf<Runnable>()
    private var isStopped = false

    fun runLoop() {
        while (!isStopped) {
            synchronized(tasks) {
                tasksCopy.addAll(tasks)
                tasks.clear()
            }
            for (runnable in tasksCopy) {
                if (!isStopped) {
                    runnable.run()
                }
            }
            tasksCopy.clear()
            GLFW.glfwWaitEvents()
        }
    }

    fun stop() {
        isStopped = true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasks) {
            tasks.add(block)
        }
        GLFW.glfwPostEmptyEvent()
    }
}