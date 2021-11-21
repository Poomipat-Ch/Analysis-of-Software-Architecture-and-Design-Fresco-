/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.samples.showcase.postprocessor;

import android.graphics.Bitmap;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.request.BasePostprocessor;
import javax.annotation.Nullable;
/**
 * Postprocessor that measures the performance of {@link BasePostprocessor#process(Bitmap,
 * PlatformBitmapFactory)}.
 */
public class BenchmarkPostprocessorForManualBitmapHandling extends BasePostprocessorWithDurationCallback {
  private final com.facebook.imagepipeline.request.BasePostprocessor mPostprocessor;

  public BenchmarkPostprocessorForManualBitmapHandling(com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback durationCallback, com.facebook.imagepipeline.request.BasePostprocessor postprocessor) {
    super(durationCallback);
    mPostprocessor = postprocessor;
  }

  @Override
  public com.facebook.common.references.CloseableReference<Bitmap> process(Bitmap sourceBitmap, com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory bitmapFactory) {
    long startTime = System.nanoTime();
    CloseableReference<Bitmap> result = mPostprocessor.process(sourceBitmap, bitmapFactory);
    showDuration(System.nanoTime() - startTime);
    return result;
  }

  @Nullable
  @Override
  public com.facebook.cache.common.CacheKey getPostprocessorCacheKey() {
    return mPostprocessor.getPostprocessorCacheKey();
  }

}
