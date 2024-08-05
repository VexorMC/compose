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

package androidx.camera.testing.impl;

import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Surface format related utility functions.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SurfaceFormatUtil {
    private SurfaceFormatUtil() {}

    /**
     * Returns the surface pixel format.
     */
    public static int getSurfaceFormat(@Nullable Surface surface) {
        return nativeGetSurfaceFormat(surface);
    }

    static {
        System.loadLibrary("testing_surface_format_jni");
    }

    private static native int nativeGetSurfaceFormat(@Nullable Surface surface);
}
