/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.factory;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.time.MonotonicClock;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer;
import com.facebook.fresco.animation.bitmap.cache.AnimationFrameCacheKey;
import com.facebook.fresco.animation.bitmap.cache.FrescoFrameCache;
import com.facebook.fresco.animation.bitmap.cache.KeepLastFrameCache;
import com.facebook.fresco.animation.bitmap.cache.NoOpCache;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.DefaultBitmapFramePreparer;
import com.facebook.fresco.animation.bitmap.preparation.FixedNumberBitmapFramePreparationStrategy;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendAnimationInformation;
import com.facebook.fresco.animation.bitmap.wrapper.AnimatedDrawableBackendFrameRenderer;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
/**
 *  Animation factory for {@link AnimatedDrawable2}. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ExperimentalBitmapAnimationDrawableFactory implements com.facebook.imagepipeline.drawable.DrawableFactory {
  public static final int CACHING_STRATEGY_NO_CACHE =  0;

  public static final int CACHING_STRATEGY_FRESCO_CACHE =  1;

  public static final int CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING =  2;

  public static final int CACHING_STRATEGY_KEEP_LAST_CACHE =  3;

  private final com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;

  private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;

  private final ExecutorService mExecutorServiceForFramePreparing;

  private final com.facebook.common.time.MonotonicClock mMonotonicClock;

  private final com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory mPlatformBitmapFactory;

  private final com.facebook.imagepipeline.cache.CountingMemoryCache<CacheKey, CloseableImage> mBackingCache;

  private final com.facebook.common.internal.Supplier<Integer> mCachingStrategySupplier;

  private final com.facebook.common.internal.Supplier<Integer> mNumberOfFramesToPrepareSupplier;

  private final com.facebook.common.internal.Supplier<Boolean> mUseDeepEqualsForCacheKey;

  public ExperimentalBitmapAnimationDrawableFactory(com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider animatedDrawableBackendProvider, ScheduledExecutorService scheduledExecutorServiceForUiThread, ExecutorService executorServiceForFramePreparing, com.facebook.common.time.MonotonicClock monotonicClock, com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory platformBitmapFactory, com.facebook.imagepipeline.cache.CountingMemoryCache<CacheKey, CloseableImage> backingCache, com.facebook.common.internal.Supplier<Integer> cachingStrategySupplier, com.facebook.common.internal.Supplier<Integer> numberOfFramesToPrepareSupplier, com.facebook.common.internal.Supplier<Boolean> useDeepEqualsForCacheKey) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
    mExecutorServiceForFramePreparing = executorServiceForFramePreparing;
    mMonotonicClock = monotonicClock;
    mPlatformBitmapFactory = platformBitmapFactory;
    mBackingCache = backingCache;
    mCachingStrategySupplier = cachingStrategySupplier;
    mNumberOfFramesToPrepareSupplier = numberOfFramesToPrepareSupplier;
    mUseDeepEqualsForCacheKey = useDeepEqualsForCacheKey;
  }

  @Override
  public boolean supportsImageType(com.facebook.imagepipeline.image.CloseableImage image) {
    return image instanceof CloseableAnimatedImage;
  }

  @Override
  public com.facebook.fresco.animation.drawable.AnimatedDrawable2 createDrawable(com.facebook.imagepipeline.image.CloseableImage image) {
    CloseableAnimatedImage closeable = ((CloseableAnimatedImage) image);
    AnimatedImage animatedImage = closeable.getImage();
    return new AnimatedDrawable2(
        createAnimationBackend(
            Preconditions.checkNotNull(closeable.getImageResult()),
            animatedImage != null ? animatedImage.getAnimatedBitmapConfig() : null));
  }

  private com.facebook.fresco.animation.backend.AnimationBackend createAnimationBackend(com.facebook.imagepipeline.animated.base.AnimatedImageResult animatedImageResult, @Nullable Bitmap.Config animatedBitmapConig) {
    AnimatedDrawableBackend animatedDrawableBackend =
        createAnimatedDrawableBackend(animatedImageResult);

    BitmapFrameCache bitmapFrameCache = createBitmapFrameCache(animatedImageResult);
    BitmapFrameRenderer bitmapFrameRenderer =
        new AnimatedDrawableBackendFrameRenderer(bitmapFrameCache, animatedDrawableBackend);

    int numberOfFramesToPrefetch = mNumberOfFramesToPrepareSupplier.get();
    BitmapFramePreparationStrategy bitmapFramePreparationStrategy = null;
    BitmapFramePreparer bitmapFramePreparer = null;
    if (numberOfFramesToPrefetch > 0) {
      bitmapFramePreparationStrategy =
          new FixedNumberBitmapFramePreparationStrategy(numberOfFramesToPrefetch);
      bitmapFramePreparer = createBitmapFramePreparer(bitmapFrameRenderer, animatedBitmapConig);
    }

    BitmapAnimationBackend bitmapAnimationBackend =
        new BitmapAnimationBackend(
            mPlatformBitmapFactory,
            bitmapFrameCache,
            new AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend),
            bitmapFrameRenderer,
            bitmapFramePreparationStrategy,
            bitmapFramePreparer);

    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        bitmapAnimationBackend, mMonotonicClock, mScheduledExecutorServiceForUiThread);
  }

  private com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer createBitmapFramePreparer(com.facebook.fresco.animation.bitmap.BitmapFrameRenderer bitmapFrameRenderer, @Nullable Bitmap.Config animatedBitmapConig) {
    return new DefaultBitmapFramePreparer(
        mPlatformBitmapFactory,
        bitmapFrameRenderer,
        animatedBitmapConig != null ? animatedBitmapConig : Bitmap.Config.ARGB_8888,
        mExecutorServiceForFramePreparing);
  }

  private com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend createAnimatedDrawableBackend(com.facebook.imagepipeline.animated.base.AnimatedImageResult animatedImageResult) {
    AnimatedImage animatedImage = animatedImageResult.getImage();
    Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
    return mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);
  }

  private com.facebook.fresco.animation.bitmap.BitmapFrameCache createBitmapFrameCache(com.facebook.imagepipeline.animated.base.AnimatedImageResult animatedImageResult) {
    switch (mCachingStrategySupplier.get()) {
      case CACHING_STRATEGY_FRESCO_CACHE:
        return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), true);
      case CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING:
        return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), false);
      case CACHING_STRATEGY_KEEP_LAST_CACHE:
        return new KeepLastFrameCache();
      case CACHING_STRATEGY_NO_CACHE:
      default:
        return new NoOpCache();
    }
  }

  private com.facebook.imagepipeline.animated.impl.AnimatedFrameCache createAnimatedFrameCache(final com.facebook.imagepipeline.animated.base.AnimatedImageResult animatedImageResult) {
    return new AnimatedFrameCache(
        new AnimationFrameCacheKey(animatedImageResult.hashCode(), mUseDeepEqualsForCacheKey.get()),
        mBackingCache);
  }

}
