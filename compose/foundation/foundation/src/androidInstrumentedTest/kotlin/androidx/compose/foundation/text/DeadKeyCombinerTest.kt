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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class DeadKeyCombinerTest {

    private val mInternalKeyEventUmlaut = InternalKeyEvent(
        NativeKeyEvent(
            0,
            0,
            NativeKeyEvent.ACTION_DOWN,
            NativeKeyEvent.KEYCODE_U,
            0,
            NativeKeyEvent.META_ALT_ON
        )
    )

    private val mInternalKeyEventSpace =
        InternalKeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_SPACE))

    private val mInternalKeyEventO =
        InternalKeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_O))

    private val mInternalKeyEventJ =
        InternalKeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_J))

    @Test
    fun testHappyPath() {
        test(
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventO to 'ö',
        )
    }

    @Test
    fun testMultipleDeadKeysFollowedByMultipleComposingKeys() {
        test(
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventO to 'ö',
            mInternalKeyEventO to 'o',
            mInternalKeyEventO to 'o',
        )
    }

    @Test
    fun testMultiplePressesInterleaved() {
        test(
            mInternalKeyEventO to 'o',
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventO to 'ö',
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventO to 'ö',
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventO to 'ö',
            mInternalKeyEventO to 'o',
        )
    }

    @Test
    fun testNonExistingCombinationFallsBackToCurrentKey() {
        test(
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventJ to 'j',
        )
    }

    @Test
    fun testSameDeadKey() {
        test(
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventUmlaut to null,
        )
    }

    @Test
    fun testDeadKeyThenSpaceOutputsTheAccent() {
        test(
            mInternalKeyEventUmlaut to null,
            mInternalKeyEventSpace to '¨',
        )
    }

    private fun test(vararg pairs: Pair<InternalKeyEvent, Char?>) {
        val combiner = DeadKeyCombiner()
        pairs.forEach { (event, result) ->
            assertThat(combiner.consume(event)?.toChar()).run {
                when (result) {
                    null -> isNull()
                    else -> isEqualTo(result)
                }
            }
        }
    }
}
