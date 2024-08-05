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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    Column {
        var text by remember { mutableStateOf("Text") }
        TextField(text, { text = it })

        val interactionSource = remember { MutableInteractionSource() }
        val hovered by interactionSource.collectIsHoveredAsState()

        val background by animateColorAsState(
            if(hovered) Color.Black
            else Color(50, 50, 50)
        )

        Box(Modifier.hoverable(interactionSource).background(background, RoundedCornerShape(10.dp))) {
            Text("Hello!", modifier = Modifier.padding(8.dp))
        }

        Box(Modifier.weight(1f)) {
            val state = rememberLazyListState()

            LazyColumn(state = state, modifier = Modifier.width(200.dp).fillMaxHeight()) {
                items(100) {
                    Text("Item $it")
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}