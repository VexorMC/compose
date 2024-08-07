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

package androidx.compose.foundation.copyPasteAndroidTests.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BasicTextSemanticsTest {

    @Test
    fun semanticsTextChanges_String() = runSkikoComposeUiTest {
        var text by mutableStateOf("before")
        setContent {
            BasicText(text)
        }
        onNodeWithText("before").assertExists()
        text = "after"
        waitForIdle()
        onNodeWithText("after").assertExists()
    }

    @Test
    fun semanticsTextChanges_AnnotatedString() = runSkikoComposeUiTest {
        var text by mutableStateOf("before")
        setContent {
            BasicText(AnnotatedString(text))
        }
        onNodeWithText("before").assertExists()
        text = "after"
        waitForIdle()
        onNodeWithText("after").assertExists()
    }
}
