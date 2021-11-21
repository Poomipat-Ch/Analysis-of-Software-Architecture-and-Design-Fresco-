/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.platform;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.soloader.DoNotOptimize;
import javax.annotation.Nullable;
/**
 * package
 * package
 */
@Nullsafe(Nullsafe.Mode.STRICT)
@DoNotOptimize
class PreverificationHelper {
  @TargetApi(Build.VERSION_CODES.O)
  @DoNotOptimize
  boolean shouldUseHardwareBitmapConfig(@Nullable Bitmap.Config config) {
    return config == Bitmap.Config.HARDWARE;
  }

}
