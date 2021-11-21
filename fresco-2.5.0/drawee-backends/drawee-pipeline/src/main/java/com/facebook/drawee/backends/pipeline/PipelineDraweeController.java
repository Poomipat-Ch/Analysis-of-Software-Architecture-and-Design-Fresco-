/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.time.AwakeTimeSinceBootClock;
import com.facebook.datasource.DataSource;
import com.facebook.drawable.base.DrawableWithCaches;
import com.facebook.drawee.backends.pipeline.debug.DebugOverlayImageOriginColor;
import com.facebook.drawee.backends.pipeline.debug.DebugOverlayImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ForwardingImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImageOriginRequestListener;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.debug.DebugControllerOverlayDrawable;
import com.facebook.drawee.debug.listener.ImageLoadingTimeControllerListener;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;
import com.facebook.fresco.ui.common.MultiUriHelper;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
/**
 * Drawee controller that bridges the image pipeline with {@link SettableDraweeHierarchy}.
 * 
 * <p>The hierarchy's actual image is set to the image(s) obtained by the provided data source. The
 * data source is automatically obtained and closed based on attach / detach calls.
 */
public class PipelineDraweeController extends com.facebook.drawee.controller.AbstractDraweeController<, > {
  private static final Class<?> TAG =  PipelineDraweeController.class;

  /**
   *  Components
   */
  private final Resources mResources;

  private final com.facebook.imagepipeline.drawable.DrawableFactory mDefaultDrawableFactory;

  /**
   *  Global drawable factories that are set when Fresco is initialized
   */
  @Nullable
  private final com.facebook.common.internal.ImmutableList<DrawableFactory> mGlobalDrawableFactories;

  @Nullable
  private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mMemoryCache;

  private com.facebook.cache.common.CacheKey mCacheKey;

  /**
   *  Constant state (non-final because controllers can be reused)
   */
  private com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> mDataSourceSupplier;

  private boolean mDrawDebugOverlay;

  /**
   *  Drawable factories that are unique for a given image request
   */
  @Nullable
  private com.facebook.common.internal.ImmutableList<DrawableFactory> mCustomDrawableFactories;

  @Nullable
  private com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor mImagePerfMonitor;

  @GuardedBy("this")
  @Nullable
  private Set<com.facebook.imagepipeline.listener.RequestListener> mRequestListeners;

  @GuardedBy("this")
  @Nullable
  private com.facebook.drawee.backends.pipeline.info.ImageOriginListener mImageOriginListener;

  private com.facebook.drawee.backends.pipeline.debug.DebugOverlayImageOriginListener mDebugOverlayImageOriginListener;

  @Nullable
  private com.facebook.imagepipeline.request.ImageRequest mImageRequest;

  @Nullable
  private com.facebook.imagepipeline.request.ImageRequest[] mFirstAvailableImageRequests;

  @Nullable
  private com.facebook.imagepipeline.request.ImageRequest mLowResImageRequest;

  public PipelineDraweeController(Resources resources, com.facebook.drawee.components.DeferredReleaser deferredReleaser, com.facebook.imagepipeline.drawable.DrawableFactory animatedDrawableFactory, Executor uiThreadExecutor, @Nullable com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> memoryCache, @Nullable com.facebook.common.internal.ImmutableList<DrawableFactory> globalDrawableFactories) {
    super(deferredReleaser, uiThreadExecutor, null, null);
    mResources = resources;
    mDefaultDrawableFactory = new DefaultDrawableFactory(resources, animatedDrawableFactory);
    mGlobalDrawableFactories = globalDrawableFactories;
    mMemoryCache = memoryCache;
  }

  /**
   * Initializes this controller with the new data source supplier, id and caller context. This
   * allows for reusing of the existing controller instead of instantiating a new one. This method
   * should be called when the controller is in detached state.
   * 
   * @param dataSourceSupplier data source supplier
   * @param id unique id for this controller
   * @param callerContext tag and context for this controller
   */
  public void initialize(com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier, String id, com.facebook.cache.common.CacheKey cacheKey, Object callerContext, @Nullable com.facebook.common.internal.ImmutableList<DrawableFactory> customDrawableFactories, @Nullable com.facebook.drawee.backends.pipeline.info.ImageOriginListener imageOriginListener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeController#initialize");
    }
    super.initialize(id, callerContext);
    init(dataSourceSupplier);
    mCacheKey = cacheKey;
    setCustomDrawableFactories(customDrawableFactories);
    clearImageOriginListeners();
    maybeUpdateDebugOverlay(null);
    addImageOriginListener(imageOriginListener);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  protected synchronized void initializePerformanceMonitoring(@Nullable com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener imagePerfDataListener, com.facebook.drawee.controller.AbstractDraweeControllerBuilder<
                PipelineDraweeControllerBuilder,
                ImageRequest,
                CloseableReference<CloseableImage>,
                ImageInfo> builder, com.facebook.common.internal.Supplier<Boolean> asyncLogging) {
    if (mImagePerfMonitor != null) {
      mImagePerfMonitor.reset();
    }
    if (imagePerfDataListener != null) {
      if (mImagePerfMonitor == null) {
        mImagePerfMonitor = new ImagePerfMonitor(AwakeTimeSinceBootClock.get(), this, asyncLogging);
      }
      mImagePerfMonitor.addImagePerfDataListener(imagePerfDataListener);
      mImagePerfMonitor.setEnabled(true);
      mImagePerfMonitor.updateImageRequestData(builder);
    }

    mImageRequest = builder.getImageRequest();
    mFirstAvailableImageRequests = builder.getFirstAvailableImageRequests();
    mLowResImageRequest = builder.getLowResImageRequest();
  }

  public void setDrawDebugOverlay(boolean drawDebugOverlay) {
    mDrawDebugOverlay = drawDebugOverlay;
  }

  public void setCustomDrawableFactories(@Nullable com.facebook.common.internal.ImmutableList<DrawableFactory> customDrawableFactories) {
    mCustomDrawableFactories = customDrawableFactories;
  }

  public synchronized void addRequestListener(com.facebook.imagepipeline.listener.RequestListener requestListener) {
    if (mRequestListeners == null) {
      mRequestListeners = new HashSet<>();
    }
    mRequestListeners.add(requestListener);
  }

  public synchronized void removeRequestListener(com.facebook.imagepipeline.listener.RequestListener requestListener) {
    if (mRequestListeners == null) {
      return;
    }
    mRequestListeners.remove(requestListener);
  }

  public synchronized void addImageOriginListener(com.facebook.drawee.backends.pipeline.info.ImageOriginListener imageOriginListener) {
    if (mImageOriginListener instanceof ForwardingImageOriginListener) {
      ((ForwardingImageOriginListener) mImageOriginListener)
          .addImageOriginListener(imageOriginListener);
    } else if (mImageOriginListener != null) {
      mImageOriginListener =
          new ForwardingImageOriginListener(mImageOriginListener, imageOriginListener);
    } else {
      mImageOriginListener = imageOriginListener;
    }
  }

  public synchronized void removeImageOriginListener(com.facebook.drawee.backends.pipeline.info.ImageOriginListener imageOriginListener) {
    if (mImageOriginListener instanceof ForwardingImageOriginListener) {
      ((ForwardingImageOriginListener) mImageOriginListener)
          .removeImageOriginListener(imageOriginListener);
      return;
    }
    if (mImageOriginListener == imageOriginListener) {
      mImageOriginListener = null;
    }
  }

  protected void clearImageOriginListeners() {
    synchronized (this) {
      mImageOriginListener = null;
    }
  }

  private void init(com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;

    maybeUpdateDebugOverlay(null);
  }

  protected Resources getResources() {
    return mResources;
  }

  protected com.facebook.cache.common.CacheKey getCacheKey() {
    return mCacheKey;
  }

  @Nullable
  public synchronized com.facebook.imagepipeline.listener.RequestListener getRequestListener() {
    RequestListener imageOriginRequestListener = null;
    if (mImageOriginListener != null) {
      imageOriginRequestListener = new ImageOriginRequestListener(getId(), mImageOriginListener);
    }
    if (mRequestListeners != null) {
      ForwardingRequestListener requestListener = new ForwardingRequestListener(mRequestListeners);
      if (imageOriginRequestListener != null) {
        requestListener.addRequestListener(imageOriginRequestListener);
      }
      return requestListener;
    }
    return imageOriginRequestListener;
  }

  @Override
  protected com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> getDataSource() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeController#getDataSource");
    }
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x: getDataSource", System.identityHashCode(this));
    }
    DataSource<CloseableReference<CloseableImage>> result = mDataSourceSupplier.get();
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  @Override
  protected Drawable createDrawable(com.facebook.common.references.CloseableReference<CloseableImage> image) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("PipelineDraweeController#createDrawable");
      }
      Preconditions.checkState(CloseableReference.isValid(image));
      CloseableImage closeableImage = image.get();

      maybeUpdateDebugOverlay(closeableImage);

      Drawable drawable =
          maybeCreateDrawableFromFactories(mCustomDrawableFactories, closeableImage);
      if (drawable != null) {
        return drawable;
      }

      drawable = maybeCreateDrawableFromFactories(mGlobalDrawableFactories, closeableImage);
      if (drawable != null) {
        return drawable;
      }

      drawable = mDefaultDrawableFactory.createDrawable(closeableImage);
      if (drawable != null) {
        return drawable;
      }
      throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Nullable
  private Drawable maybeCreateDrawableFromFactories(@Nullable com.facebook.common.internal.ImmutableList<DrawableFactory> drawableFactories, com.facebook.imagepipeline.image.CloseableImage closeableImage) {
    if (drawableFactories == null) {
      return null;
    }
    for (DrawableFactory factory : drawableFactories) {
      if (factory.supportsImageType(closeableImage)) {
        Drawable drawable = factory.createDrawable(closeableImage);
        if (drawable != null) {
          return drawable;
        }
      }
    }
    return null;
  }

  @Override
  public void setHierarchy(@Nullable com.facebook.drawee.interfaces.DraweeHierarchy hierarchy) {
    super.setHierarchy(hierarchy);
    maybeUpdateDebugOverlay(null);
  }

  @Override
  public boolean isSameImageRequest(@Nullable com.facebook.drawee.interfaces.DraweeController other) {
    if (mCacheKey != null && other instanceof PipelineDraweeController) {
      return Objects.equal(mCacheKey, ((PipelineDraweeController) other).getCacheKey());
    }
    return false;
  }

  private void maybeUpdateDebugOverlay(@Nullable com.facebook.imagepipeline.image.CloseableImage image) {
    if (!mDrawDebugOverlay) {
      return;
    }

    if (getControllerOverlay() == null) {
      final DebugControllerOverlayDrawable controllerOverlay = new DebugControllerOverlayDrawable();
      ImageLoadingTimeControllerListener overlayImageLoadListener =
          new ImageLoadingTimeControllerListener(controllerOverlay);
      mDebugOverlayImageOriginListener = new DebugOverlayImageOriginListener();
      addControllerListener(overlayImageLoadListener);
      setControllerOverlay(controllerOverlay);
    }

    if (mImageOriginListener == null) {
      addImageOriginListener(mDebugOverlayImageOriginListener);
    }

    if (getControllerOverlay() instanceof DebugControllerOverlayDrawable) {
      updateDebugOverlay(image, (DebugControllerOverlayDrawable) getControllerOverlay());
    }
  }

  /**
   * updateDebugOverlay updates the debug overlay. Subclasses of {@link PipelineDraweeController}
   * can override this method (and call <code>super</code>) to provide additional debug information.
   */
  protected void updateDebugOverlay(@Nullable com.facebook.imagepipeline.image.CloseableImage image, com.facebook.drawee.debug.DebugControllerOverlayDrawable debugOverlay) {
    debugOverlay.setControllerId(getId());

    final DraweeHierarchy draweeHierarchy = getHierarchy();
    ScaleType scaleType = null;
    if (draweeHierarchy != null) {
      final ScaleTypeDrawable scaleTypeDrawable =
          ScalingUtils.getActiveScaleTypeDrawable(draweeHierarchy.getTopLevelDrawable());
      scaleType = scaleTypeDrawable != null ? scaleTypeDrawable.getScaleType() : null;
    }
    debugOverlay.setScaleType(scaleType);

    // fill in image origin text and color hint
    final int origin = mDebugOverlayImageOriginListener.getImageOrigin();
    final String originText = ImageOriginUtils.toString(origin);
    final int originColor = DebugOverlayImageOriginColor.getImageOriginColor(origin);
    debugOverlay.setOrigin(originText, originColor);

    if (image != null) {
      debugOverlay.setDimensions(image.getWidth(), image.getHeight());
      debugOverlay.setImageSize(image.getSizeInBytes());
    } else {
      debugOverlay.reset();
    }
  }

  @Override
  protected com.facebook.imagepipeline.image.ImageInfo getImageInfo(com.facebook.common.references.CloseableReference<CloseableImage> image) {
    Preconditions.checkState(CloseableReference.isValid(image));
    return image.get();
  }

  @Override
  protected int getImageHash(@Nullable com.facebook.common.references.CloseableReference<CloseableImage> image) {
    return (image != null) ? image.getValueHash() : 0;
  }

  @Override
  protected void releaseImage(@Nullable com.facebook.common.references.CloseableReference<CloseableImage> image) {
    CloseableReference.closeSafely(image);
  }

  @Override
  protected void releaseDrawable(@Nullable Drawable drawable) {
    if (drawable instanceof DrawableWithCaches) {
      ((DrawableWithCaches) drawable).dropCaches();
    }
  }

  @Override
  @Nullable
  protected com.facebook.common.references.CloseableReference<CloseableImage> getCachedImage() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeController#getCachedImage");
    }
    try {
      if (mMemoryCache == null || mCacheKey == null) {
        return null;
      }
      // We get the CacheKey
      CloseableReference<CloseableImage> closeableImage = mMemoryCache.get(mCacheKey);
      if (closeableImage != null && !closeableImage.get().getQualityInfo().isOfFullQuality()) {
        closeableImage.close();
        return null;
      }
      return closeableImage;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  protected void onImageLoadedFromCacheImmediately(String id, com.facebook.common.references.CloseableReference<CloseableImage> cachedImage) {
    super.onImageLoadedFromCacheImmediately(id, cachedImage);
    synchronized (this) {
      if (mImageOriginListener != null) {
        mImageOriginListener.onImageLoaded(
            id, ImageOrigin.MEMORY_BITMAP_SHORTCUT, true, "PipelineDraweeController");
      }
    }
  }

  protected com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> getDataSourceSupplier() {
    return mDataSourceSupplier;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("super", super.toString())
        .add("dataSourceSupplier", mDataSourceSupplier)
        .toString();
  }

  @Override
  @Nullable
  public Map<String, Object> obtainExtrasFromImage(com.facebook.imagepipeline.image.ImageInfo info) {
    if (info == null) return null;
    return info.getExtras();
  }

  @Override
  @Nullable
  protected Uri getMainUri() {
    return MultiUriHelper.getMainUri(
        mImageRequest,
        mLowResImageRequest,
        mFirstAvailableImageRequests,
        ImageRequest.REQUEST_TO_URI_FN);
  }

}
