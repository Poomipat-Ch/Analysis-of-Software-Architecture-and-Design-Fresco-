/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline;

import android.content.res.Resources;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
/**
 *  Default implementation of {@link PipelineDraweeControllerFactory}. 
 */
public class PipelineDraweeControllerFactory {
  private Resources mResources;

  private com.facebook.drawee.components.DeferredReleaser mDeferredReleaser;

  private com.facebook.imagepipeline.drawable.DrawableFactory mAnimatedDrawableFactory;

  private Executor mUiThreadExecutor;

  @Nullable
  private com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mMemoryCache;

  @Nullable
  private com.facebook.common.internal.ImmutableList<DrawableFactory> mDrawableFactories;

  @Nullable
  private com.facebook.common.internal.Supplier<Boolean> mDebugOverlayEnabledSupplier;

  public void init(Resources resources, com.facebook.drawee.components.DeferredReleaser deferredReleaser, com.facebook.imagepipeline.drawable.DrawableFactory animatedDrawableFactory, Executor uiThreadExecutor, com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> memoryCache, @Nullable com.facebook.common.internal.ImmutableList<DrawableFactory> drawableFactories, @Nullable com.facebook.common.internal.Supplier<Boolean> debugOverlayEnabledSupplier) {
    mResources = resources;
    mDeferredReleaser = deferredReleaser;
    mAnimatedDrawableFactory = animatedDrawableFactory;
    mUiThreadExecutor = uiThreadExecutor;
    mMemoryCache = memoryCache;
    mDrawableFactories = drawableFactories;
    mDebugOverlayEnabledSupplier = debugOverlayEnabledSupplier;
  }

  public PipelineDraweeController newController() {
    PipelineDraweeController controller =
        internalCreateController(
            mResources,
            mDeferredReleaser,
            mAnimatedDrawableFactory,
            mUiThreadExecutor,
            mMemoryCache,
            mDrawableFactories);
    if (mDebugOverlayEnabledSupplier != null) {
      controller.setDrawDebugOverlay(mDebugOverlayEnabledSupplier.get());
    }
    return controller;
  }

  protected PipelineDraweeController internalCreateController(Resources resources, com.facebook.drawee.components.DeferredReleaser deferredReleaser, com.facebook.imagepipeline.drawable.DrawableFactory animatedDrawableFactory, Executor uiThreadExecutor, @Nullable com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> memoryCache, @Nullable com.facebook.common.internal.ImmutableList<DrawableFactory> drawableFactories) {
    return new PipelineDraweeController(
        resources,
        deferredReleaser,
        animatedDrawableFactory,
        uiThreadExecutor,
        memoryCache,
        drawableFactories);
  }

}
