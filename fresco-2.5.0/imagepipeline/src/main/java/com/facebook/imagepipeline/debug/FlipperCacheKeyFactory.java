/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.debug;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FlipperCacheKeyFactory extends com.facebook.imagepipeline.cache.DefaultCacheKeyFactory {
  @Nullable
  private final DebugImageTracker mDebugImageTracker;

  public FlipperCacheKeyFactory(@Nullable DebugImageTracker debugImageTracker) {
    mDebugImageTracker = debugImageTracker;
  }

  @Override
  public com.facebook.cache.common.CacheKey getBitmapCacheKey(com.facebook.imagepipeline.request.ImageRequest request, @Nullable Object callerContext) {
    CacheKey bitmapCacheKey = super.getBitmapCacheKey(request, callerContext);
    if (mDebugImageTracker != null) {
      mDebugImageTracker.trackImage(request, bitmapCacheKey);
    }
    return bitmapCacheKey;
  }

}
