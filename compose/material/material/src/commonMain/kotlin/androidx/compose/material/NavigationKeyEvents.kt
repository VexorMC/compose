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

package androidx.compose.material

import androidx.compose.ui.input.key.InternalKeyEvent

internal expect val InternalKeyEvent.isDirectionUp: Boolean
internal expect val InternalKeyEvent.isDirectionDown: Boolean
internal expect val InternalKeyEvent.isDirectionRight: Boolean
internal expect val InternalKeyEvent.isDirectionLeft: Boolean
internal expect val InternalKeyEvent.isHome: Boolean
internal expect val InternalKeyEvent.isMoveEnd: Boolean
internal expect val InternalKeyEvent.isPgUp: Boolean
internal expect val InternalKeyEvent.isPgDn: Boolean
