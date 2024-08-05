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

package androidx.compose.foundation.text

import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.InternalKeyEvent

private fun Char.isPrintable(): Boolean {
    val block = Character.UnicodeBlock.of(this)
    return (!Character.isISOControl(this)) &&
        this != java.awt.event.KeyEvent.CHAR_UNDEFINED &&
        block != null &&
        block != Character.UnicodeBlock.SPECIALS
}

actual val InternalKeyEvent.isTypedEvent: Boolean
    get() = awtEventOrNull?.id == java.awt.event.KeyEvent.KEY_TYPED &&
        awtEventOrNull?.keyChar?.isPrintable() == true