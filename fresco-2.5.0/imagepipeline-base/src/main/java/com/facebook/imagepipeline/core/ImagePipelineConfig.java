/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.annotation.VisibleForTesting;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.webp.BitmapCreator;
import com.facebook.common.webp.WebpBitmapFactory;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.imagepipeline.bitmaps.HoneycombBitmapCreator;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheTrimStrategy;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.CountingLruBitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.DefaultBitmapMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.cache.DefaultEncodedMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.cache.NoOpImageCacheStatsTracker;
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker;
import com.facebook.imagepipeline.debug.NoOpCloseableReferenceLeakTracker;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoderConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
/**
 * Master configuration class for the image pipeline library.
 * 
 * <p>To use: <code>
 *   ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
 *       .setXXX(xxx)
 *       .setYYY(yyy)
 *       .build();
 *   ImagePipelineFactory factory = new ImagePipelineFactory(config);
 *   ImagePipeline pipeline = factory.getImagePipeline();
 * </code>
 * 
 * <p>This should only be done once per process.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePipelineConfig implements ImagePipelineConfigInterface {
  /**
   *  If a member here is marked @Nullable, it must be constructed by ImagePipelineFactory
   *  on demand if needed.
   *  There are a lot of parameters in this class. Please follow strict alphabetical order.
   */
  private final Bitmap.Config mBitmapConfig;

  private final com.facebook.common.internal.Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;

  private final com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy mBitmapMemoryCacheTrimStrategy;

  @Nullable
  private final com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver<CacheKey> mBitmapMemoryCacheEntryStateObserver;

  private final com.facebook.imagepipeline.cache.CacheKeyFactory mCacheKeyFactory;

  private final Context mContext;

  private final boolean mDownsampleEnabled;

  private final FileCacheFactory mFileCacheFactory;

  private final com.facebook.common.internal.Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;

  private final ExecutorSupplier mExecutorSupplier;

  private final com.facebook.imagepipeline.cache.ImageCacheStatsTracker mImageCacheStatsTracker;

  @Nullable
  private final com.facebook.imagepipeline.decoder.ImageDecoder mImageDecoder;

  @Nullable
  private final com.facebook.imagepipeline.transcoder.ImageTranscoderFactory mImageTranscoderFactory;

  @Nullable
  @ImageTranscoderType
  private final Integer mImageTranscoderType;

  private final com.facebook.common.internal.Supplier<Boolean> mIsPrefetchEnabledSupplier;

  private final com.facebook.cache.disk.DiskCacheConfig mMainDiskCacheConfig;

  private final com.facebook.common.memory.MemoryTrimmableRegistry mMemoryTrimmableRegistry;

  @MemoryChunkType
  private final int mMemoryChunkType;

  private final com.facebook.imagepipeline.producers.NetworkFetcher mNetworkFetcher;

  private final int mHttpNetworkTimeout;

  @Nullable
  private final com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory mPlatformBitmapFactory;

  private final com.facebook.imagepipeline.memory.PoolFactory mPoolFactory;

  private final com.facebook.imagepipeline.decoder.ProgressiveJpegConfig mProgressiveJpegConfig;

  private final Set<com.facebook.imagepipeline.listener.RequestListener> mRequestListeners;

  private final Set<com.facebook.imagepipeline.listener.RequestListener2> mRequestListener2s;

  private final boolean mResizeAndRotateEnabledForNetwork;

  private final com.facebook.cache.disk.DiskCacheConfig mSmallImageDiskCacheConfig;

  @Nullable
  private final com.facebook.imagepipeline.decoder.ImageDecoderConfig mImageDecoderConfig;

  private final ImagePipelineExperiments mImagePipelineExperiments;

  private final boolean mDiskCacheEnabled;

  @Nullable
  private final com.facebook.callercontext.CallerContextVerifier mCallerContextVerifier;

  private final com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker mCloseableReferenceLeakTracker;

  @Nullable
  private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mBitmapCache;

  @Nullable
  private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;

  @Nullable
  private final com.facebook.common.executors.SerialExecutorService mSerialExecutorServiceForAnimatedImages;

  private final com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory mBitmapMemoryCacheFactory;

  public static class DefaultImageRequestConfig {
    private boolean mProgressiveRenderingEnabled =  false;

    private DefaultImageRequestConfig() {
    }

    public void setProgressiveRenderingEnabled(boolean progressiveRenderingEnabled) {
      this.mProgressiveRenderingEnabled = progressiveRenderingEnabled;
    }

    public boolean isProgressiveRenderingEnabled() {
      return mProgressiveRenderingEnabled;
    }

  }

  private static ImagePipelineConfig.DefaultImageRequestConfig sDefaultImageRequestConfig = 
      new DefaultImageRequestConfig();

  private ImagePipelineConfig(ImagePipelineConfig.Builder builder) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipelineConfig()");
    }
    // We have to build experiments before the rest
    mImagePipelineExperiments = builder.mExperimentsBuilder.build();
    mBitmapMemoryCacheParamsSupplier =
        builder.mBitmapMemoryCacheParamsSupplier == null
            ? new DefaultBitmapMemoryCacheParamsSupplier(
                (ActivityManager)
                    Preconditions.checkNotNull(
                        builder.mContext.getSystemService(Context.ACTIVITY_SERVICE)))
            : builder.mBitmapMemoryCacheParamsSupplier;
    mBitmapMemoryCacheTrimStrategy =
        builder.mBitmapMemoryCacheTrimStrategy == null
            ? new BitmapMemoryCacheTrimStrategy()
            : builder.mBitmapMemoryCacheTrimStrategy;
    mBitmapMemoryCacheEntryStateObserver = builder.mBitmapMemoryCacheEntryStateObserver;
    mBitmapConfig = builder.mBitmapConfig == null ? Bitmap.Config.ARGB_8888 : builder.mBitmapConfig;
    mCacheKeyFactory =
        builder.mCacheKeyFactory == null
            ? DefaultCacheKeyFactory.getInstance()
            : builder.mCacheKeyFactory;
    mContext = Preconditions.checkNotNull(builder.mContext);
    mFileCacheFactory =
        builder.mFileCacheFactory == null
            ? new DiskStorageCacheFactory(new DynamicDefaultDiskStorageFactory())
            : builder.mFileCacheFactory;
    mDownsampleEnabled = builder.mDownsampleEnabled;
    mEncodedMemoryCacheParamsSupplier =
        builder.mEncodedMemoryCacheParamsSupplier == null
            ? new DefaultEncodedMemoryCacheParamsSupplier()
            : builder.mEncodedMemoryCacheParamsSupplier;
    mImageCacheStatsTracker =
        builder.mImageCacheStatsTracker == null
            ? NoOpImageCacheStatsTracker.getInstance()
            : builder.mImageCacheStatsTracker;
    mImageDecoder = builder.mImageDecoder;
    mImageTranscoderFactory = getImageTranscoderFactory(builder);
    mImageTranscoderType = builder.mImageTranscoderType;
    mIsPrefetchEnabledSupplier =
        builder.mIsPrefetchEnabledSupplier == null
            ? new Supplier<Boolean>() {
              @Override
              public Boolean get() {
                return true;
              }
            }
            : builder.mIsPrefetchEnabledSupplier;
    mMainDiskCacheConfig =
        builder.mMainDiskCacheConfig == null
            ? getDefaultMainDiskCacheConfig(builder.mContext)
            : builder.mMainDiskCacheConfig;
    mMemoryTrimmableRegistry =
        builder.mMemoryTrimmableRegistry == null
            ? NoOpMemoryTrimmableRegistry.getInstance()
            : builder.mMemoryTrimmableRegistry;
    mMemoryChunkType = getMemoryChunkType(builder, mImagePipelineExperiments);
    mHttpNetworkTimeout =
        builder.mHttpConnectionTimeout < 0
            ? HttpUrlConnectionNetworkFetcher.HTTP_DEFAULT_TIMEOUT
            : builder.mHttpConnectionTimeout;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipelineConfig->mNetworkFetcher");
    }
    mNetworkFetcher =
        builder.mNetworkFetcher == null
            ? new HttpUrlConnectionNetworkFetcher(mHttpNetworkTimeout)
            : builder.mNetworkFetcher;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    mPlatformBitmapFactory = builder.mPlatformBitmapFactory;
    mPoolFactory =
        builder.mPoolFactory == null
            ? new PoolFactory(PoolConfig.newBuilder().build())
            : builder.mPoolFactory;
    mProgressiveJpegConfig =
        builder.mProgressiveJpegConfig == null
            ? new SimpleProgressiveJpegConfig()
            : builder.mProgressiveJpegConfig;
    mRequestListeners =
        builder.mRequestListeners == null
            ? new HashSet<RequestListener>()
            : builder.mRequestListeners;
    mRequestListener2s =
        builder.mRequestListener2s == null
            ? new HashSet<RequestListener2>()
            : builder.mRequestListener2s;
    mResizeAndRotateEnabledForNetwork = builder.mResizeAndRotateEnabledForNetwork;
    mSmallImageDiskCacheConfig =
        builder.mSmallImageDiskCacheConfig == null
            ? mMainDiskCacheConfig
            : builder.mSmallImageDiskCacheConfig;
    mImageDecoderConfig = builder.mImageDecoderConfig;
    // Below this comment can't be built in alphabetical order, because of dependencies
    int numCpuBoundThreads = mPoolFactory.getFlexByteArrayPoolMaxNumThreads();
    mExecutorSupplier =
        builder.mExecutorSupplier == null
            ? new DefaultExecutorSupplier(numCpuBoundThreads)
            : builder.mExecutorSupplier;
    mDiskCacheEnabled = builder.mDiskCacheEnabled;
    mCallerContextVerifier = builder.mCallerContextVerifier;
    mCloseableReferenceLeakTracker = builder.mCloseableReferenceLeakTracker;
    mBitmapCache = builder.mBitmapMemoryCache;
    mBitmapMemoryCacheFactory =
        builder.mBitmapMemoryCacheFactory == null
            ? new CountingLruBitmapMemoryCacheFactory()
            : builder.mBitmapMemoryCacheFactory;
    mEncodedMemoryCache = builder.mEncodedMemoryCache;
    mSerialExecutorServiceForAnimatedImages = builder.mSerialExecutorServiceForAnimatedImages;
    // Here we manage the WebpBitmapFactory implementation if any
    WebpBitmapFactory webpBitmapFactory = mImagePipelineExperiments.getWebpBitmapFactory();
    if (webpBitmapFactory != null) {
      BitmapCreator bitmapCreator = new HoneycombBitmapCreator(getPoolFactory());
      setWebpBitmapFactory(webpBitmapFactory, mImagePipelineExperiments, bitmapCreator);
    } else {
      // We check using introspection only if the experiment is enabled
      if (mImagePipelineExperiments.isWebpSupportEnabled()
          && WebpSupportStatus.sIsWebpSupportRequired) {
        webpBitmapFactory = WebpSupportStatus.loadWebpBitmapFactoryIfExists();
        if (webpBitmapFactory != null) {
          BitmapCreator bitmapCreator = new HoneycombBitmapCreator(getPoolFactory());
          setWebpBitmapFactory(webpBitmapFactory, mImagePipelineExperiments, bitmapCreator);
        }
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  public static class Builder {
    @Nullable
    private Bitmap.Config mBitmapConfig;

    @Nullable
    private com.facebook.common.internal.Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;

    @Nullable
    private com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver<CacheKey> mBitmapMemoryCacheEntryStateObserver;

    @Nullable
    private com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy mBitmapMemoryCacheTrimStrategy;

    @Nullable
    private com.facebook.imagepipeline.cache.CacheKeyFactory mCacheKeyFactory;

    private final Context mContext;

    private boolean mDownsampleEnabled =  false;

    @Nullable
    private com.facebook.common.internal.Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;

    @Nullable
    private ExecutorSupplier mExecutorSupplier;

    @Nullable
    private com.facebook.imagepipeline.cache.ImageCacheStatsTracker mImageCacheStatsTracker;

    @Nullable
    private com.facebook.imagepipeline.decoder.ImageDecoder mImageDecoder;

    @Nullable
    private com.facebook.imagepipeline.transcoder.ImageTranscoderFactory mImageTranscoderFactory;

    @Nullable
    @ImageTranscoderType
    private Integer mImageTranscoderType =  null;

    @Nullable
    private com.facebook.common.internal.Supplier<Boolean> mIsPrefetchEnabledSupplier;

    @Nullable
    private com.facebook.cache.disk.DiskCacheConfig mMainDiskCacheConfig;

    @Nullable
    private com.facebook.common.memory.MemoryTrimmableRegistry mMemoryTrimmableRegistry;

    @Nullable
    @MemoryChunkType
    private Integer mMemoryChunkType =  null;

    @Nullable
    private com.facebook.imagepipeline.producers.NetworkFetcher mNetworkFetcher;

    @Nullable
    private com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory mPlatformBitmapFactory;

    @Nullable
    private com.facebook.imagepipeline.memory.PoolFactory mPoolFactory;

    @Nullable
    private com.facebook.imagepipeline.decoder.ProgressiveJpegConfig mProgressiveJpegConfig;

    @Nullable
    private Set<com.facebook.imagepipeline.listener.RequestListener> mRequestListeners;

    @Nullable
    private Set<com.facebook.imagepipeline.listener.RequestListener2> mRequestListener2s;

    private boolean mResizeAndRotateEnabledForNetwork =  true;

    @Nullable
    private com.facebook.cache.disk.DiskCacheConfig mSmallImageDiskCacheConfig;

    @Nullable
    private FileCacheFactory mFileCacheFactory;

    @Nullable
    private com.facebook.imagepipeline.decoder.ImageDecoderConfig mImageDecoderConfig;

    private int mHttpConnectionTimeout =  -1;

    private final ImagePipelineExperiments.Builder mExperimentsBuilder = 
        new ImagePipelineExperiments.Builder(this);

    private boolean mDiskCacheEnabled =  true;

    @Nullable
    private com.facebook.callercontext.CallerContextVerifier mCallerContextVerifier;

    private com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker mCloseableReferenceLeakTracker = 
        new NoOpCloseableReferenceLeakTracker();

    @Nullable
    private com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;

    @Nullable
    private com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;

    @Nullable
    private com.facebook.common.executors.SerialExecutorService mSerialExecutorServiceForAnimatedImages;

    @Nullable
    private com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory mBitmapMemoryCacheFactory;

    private Builder(Context context) {
      // Doesn't use a setter as always required.
      mContext = Preconditions.checkNotNull(context);
    }

    public ImagePipelineConfig.Builder setBitmapsConfig(Bitmap.Config config) {
      mBitmapConfig = config;
      return this;
    }

    public ImagePipelineConfig.Builder setBitmapMemoryCacheParamsSupplier(com.facebook.common.internal.Supplier<MemoryCacheParams> bitmapMemoryCacheParamsSupplier) {
      mBitmapMemoryCacheParamsSupplier =
          Preconditions.checkNotNull(bitmapMemoryCacheParamsSupplier);
      return this;
    }

    public ImagePipelineConfig.Builder setBitmapMemoryCacheEntryStateObserver(com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver<CacheKey> bitmapMemoryCacheEntryStateObserver) {
      mBitmapMemoryCacheEntryStateObserver = bitmapMemoryCacheEntryStateObserver;
      return this;
    }

    public ImagePipelineConfig.Builder setBitmapMemoryCacheTrimStrategy(com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy trimStrategy) {
      mBitmapMemoryCacheTrimStrategy = trimStrategy;
      return this;
    }

    public ImagePipelineConfig.Builder setCacheKeyFactory(com.facebook.imagepipeline.cache.CacheKeyFactory cacheKeyFactory) {
      mCacheKeyFactory = cacheKeyFactory;
      return this;
    }

    public ImagePipelineConfig.Builder setHttpConnectionTimeout(int httpConnectionTimeoutMs) {
      mHttpConnectionTimeout = httpConnectionTimeoutMs;
      return this;
    }

    public ImagePipelineConfig.Builder setFileCacheFactory(FileCacheFactory fileCacheFactory) {
      mFileCacheFactory = fileCacheFactory;
      return this;
    }

    public boolean isDownsampleEnabled() {
      return mDownsampleEnabled;
    }

    public ImagePipelineConfig.Builder setDownsampleEnabled(boolean downsampleEnabled) {
      mDownsampleEnabled = downsampleEnabled;
      return this;
    }

    public boolean isDiskCacheEnabled() {
      return mDiskCacheEnabled;
    }

    public ImagePipelineConfig.Builder setDiskCacheEnabled(boolean diskCacheEnabled) {
      mDiskCacheEnabled = diskCacheEnabled;
      return this;
    }

    public ImagePipelineConfig.Builder setEncodedMemoryCacheParamsSupplier(com.facebook.common.internal.Supplier<MemoryCacheParams> encodedMemoryCacheParamsSupplier) {
      mEncodedMemoryCacheParamsSupplier =
          Preconditions.checkNotNull(encodedMemoryCacheParamsSupplier);
      return this;
    }

    public ImagePipelineConfig.Builder setExecutorSupplier(ExecutorSupplier executorSupplier) {
      mExecutorSupplier = executorSupplier;
      return this;
    }

    public ImagePipelineConfig.Builder setImageCacheStatsTracker(com.facebook.imagepipeline.cache.ImageCacheStatsTracker imageCacheStatsTracker) {
      mImageCacheStatsTracker = imageCacheStatsTracker;
      return this;
    }

    public ImagePipelineConfig.Builder setImageDecoder(com.facebook.imagepipeline.decoder.ImageDecoder imageDecoder) {
      mImageDecoder = imageDecoder;
      return this;
    }

    @Nullable
    @ImageTranscoderType
    public Integer getImageTranscoderType() {
      return mImageTranscoderType;
    }

    public ImagePipelineConfig.Builder setImageTranscoderType(@ImageTranscoderType int imageTranscoderType) {
      mImageTranscoderType = imageTranscoderType;
      return this;
    }

    public ImagePipelineConfig.Builder setImageTranscoderFactory(com.facebook.imagepipeline.transcoder.ImageTranscoderFactory imageTranscoderFactory) {
      mImageTranscoderFactory = imageTranscoderFactory;
      return this;
    }

    public ImagePipelineConfig.Builder setIsPrefetchEnabledSupplier(com.facebook.common.internal.Supplier<Boolean> isPrefetchEnabledSupplier) {
      mIsPrefetchEnabledSupplier = isPrefetchEnabledSupplier;
      return this;
    }

    public ImagePipelineConfig.Builder setMainDiskCacheConfig(com.facebook.cache.disk.DiskCacheConfig mainDiskCacheConfig) {
      mMainDiskCacheConfig = mainDiskCacheConfig;
      return this;
    }

    public ImagePipelineConfig.Builder setMemoryTrimmableRegistry(com.facebook.common.memory.MemoryTrimmableRegistry memoryTrimmableRegistry) {
      mMemoryTrimmableRegistry = memoryTrimmableRegistry;
      return this;
    }

    @Nullable
    @MemoryChunkType
    public Integer getMemoryChunkType() {
      return mMemoryChunkType;
    }

    public ImagePipelineConfig.Builder setMemoryChunkType(@MemoryChunkType int memoryChunkType) {
      mMemoryChunkType = memoryChunkType;
      return this;
    }

    public ImagePipelineConfig.Builder setNetworkFetcher(com.facebook.imagepipeline.producers.NetworkFetcher networkFetcher) {
      mNetworkFetcher = networkFetcher;
      return this;
    }

    public ImagePipelineConfig.Builder setPlatformBitmapFactory(com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory platformBitmapFactory) {
      mPlatformBitmapFactory = platformBitmapFactory;
      return this;
    }

    public ImagePipelineConfig.Builder setPoolFactory(com.facebook.imagepipeline.memory.PoolFactory poolFactory) {
      mPoolFactory = poolFactory;
      return this;
    }

    public ImagePipelineConfig.Builder setProgressiveJpegConfig(com.facebook.imagepipeline.decoder.ProgressiveJpegConfig progressiveJpegConfig) {
      mProgressiveJpegConfig = progressiveJpegConfig;
      return this;
    }

    public ImagePipelineConfig.Builder setRequestListeners(Set<RequestListener> requestListeners) {
      mRequestListeners = requestListeners;
      return this;
    }

    public ImagePipelineConfig.Builder setRequestListener2s(Set<RequestListener2> requestListeners) {
      mRequestListener2s = requestListeners;
      return this;
    }

    public ImagePipelineConfig.Builder setResizeAndRotateEnabledForNetwork(boolean resizeAndRotateEnabledForNetwork) {
      mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
      return this;
    }

    public ImagePipelineConfig.Builder setSmallImageDiskCacheConfig(com.facebook.cache.disk.DiskCacheConfig smallImageDiskCacheConfig) {
      mSmallImageDiskCacheConfig = smallImageDiskCacheConfig;
      return this;
    }

    public ImagePipelineConfig.Builder setImageDecoderConfig(com.facebook.imagepipeline.decoder.ImageDecoderConfig imageDecoderConfig) {
      mImageDecoderConfig = imageDecoderConfig;
      return this;
    }

    public ImagePipelineConfig.Builder setCallerContextVerifier(com.facebook.callercontext.CallerContextVerifier callerContextVerifier) {
      mCallerContextVerifier = callerContextVerifier;
      return this;
    }

    public ImagePipelineConfig.Builder setCloseableReferenceLeakTracker(com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker closeableReferenceLeakTracker) {
      mCloseableReferenceLeakTracker = closeableReferenceLeakTracker;
      return this;
    }

    public ImagePipelineConfig.Builder setBitmapMemoryCache(@Nullable com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache) {
      mBitmapMemoryCache = bitmapMemoryCache;
      return this;
    }

    public ImagePipelineConfig.Builder setEncodedMemoryCache(@Nullable com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache) {
      mEncodedMemoryCache = encodedMemoryCache;
      return this;
    }

    public ImagePipelineConfig.Builder setExecutorServiceForAnimatedImages(@Nullable com.facebook.common.executors.SerialExecutorService serialExecutorService) {
      mSerialExecutorServiceForAnimatedImages = serialExecutorService;
      return this;
    }

    public ImagePipelineConfig.Builder setBitmapMemoryCacheFactory(@Nullable com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory bitmapMemoryCacheFactory) {
      mBitmapMemoryCacheFactory = bitmapMemoryCacheFactory;
      return this;
    }

    @Nullable
    public com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory getBitmapMemoryCacheFactory() {
      return mBitmapMemoryCacheFactory;
    }

    public ImagePipelineExperiments.Builder experiment() {
      return mExperimentsBuilder;
    }

    public ImagePipelineConfig build() {
      return new ImagePipelineConfig(this);
    }

  }

  private static void setWebpBitmapFactory(final com.facebook.common.webp.WebpBitmapFactory webpBitmapFactory, final ImagePipelineExperiments imagePipelineExperiments, final com.facebook.common.webp.BitmapCreator bitmapCreator)
  {
    WebpSupportStatus.sWebpBitmapFactory = webpBitmapFactory;
    final WebpBitmapFactory.WebpErrorLogger webpErrorLogger =
        imagePipelineExperiments.getWebpErrorLogger();
    if (webpErrorLogger != null) {
      webpBitmapFactory.setWebpErrorLogger(webpErrorLogger);
    }
    if (bitmapCreator != null) {
      webpBitmapFactory.setBitmapCreator(bitmapCreator);
    }
  }

  private static com.facebook.cache.disk.DiskCacheConfig getDefaultMainDiskCacheConfig(final Context context)
  {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("DiskCacheConfig.getDefaultMainDiskCacheConfig");
      }
      return DiskCacheConfig.newBuilder(context).build();
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @VisibleForTesting
  static void resetDefaultRequestConfig()
  {
    sDefaultImageRequestConfig = new DefaultImageRequestConfig();
  }

  @Override
  public Bitmap.Config getBitmapConfig() {
    return mBitmapConfig;
  }

  @Override
  public com.facebook.common.internal.Supplier<MemoryCacheParams> getBitmapMemoryCacheParamsSupplier() {
    return mBitmapMemoryCacheParamsSupplier;
  }

  @Override
  public com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy getBitmapMemoryCacheTrimStrategy() {
    return mBitmapMemoryCacheTrimStrategy;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver<CacheKey> getBitmapMemoryCacheEntryStateObserver() {
    return mBitmapMemoryCacheEntryStateObserver;
  }

  @Override
  public com.facebook.imagepipeline.cache.CacheKeyFactory getCacheKeyFactory() {
    return mCacheKeyFactory;
  }

  @Override
  public Context getContext() {
    return mContext;
  }

  public static ImagePipelineConfig.DefaultImageRequestConfig getDefaultImageRequestConfig()
  {
    return sDefaultImageRequestConfig;
  }

  @Override
  public FileCacheFactory getFileCacheFactory() {
    return mFileCacheFactory;
  }

  @Override
  public boolean isDownsampleEnabled() {
    return mDownsampleEnabled;
  }

  @Override
  public boolean isDiskCacheEnabled() {
    return mDiskCacheEnabled;
  }

  @Override
  public com.facebook.common.internal.Supplier<MemoryCacheParams> getEncodedMemoryCacheParamsSupplier() {
    return mEncodedMemoryCacheParamsSupplier;
  }

  @Override
  public ExecutorSupplier getExecutorSupplier() {
    return mExecutorSupplier;
  }

  @Override
  @Nullable
  public com.facebook.common.executors.SerialExecutorService getExecutorServiceForAnimatedImages() {
    return mSerialExecutorServiceForAnimatedImages;
  }

  @Override
  public com.facebook.imagepipeline.cache.ImageCacheStatsTracker getImageCacheStatsTracker() {
    return mImageCacheStatsTracker;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.decoder.ImageDecoder getImageDecoder() {
    return mImageDecoder;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.transcoder.ImageTranscoderFactory getImageTranscoderFactory() {
    return mImageTranscoderFactory;
  }

  @Override
  @Nullable
  @ImageTranscoderType
  public Integer getImageTranscoderType() {
    return mImageTranscoderType;
  }

  @Override
  public com.facebook.common.internal.Supplier<Boolean> getIsPrefetchEnabledSupplier() {
    return mIsPrefetchEnabledSupplier;
  }

  @Override
  public com.facebook.cache.disk.DiskCacheConfig getMainDiskCacheConfig() {
    return mMainDiskCacheConfig;
  }

  @Override
  public com.facebook.common.memory.MemoryTrimmableRegistry getMemoryTrimmableRegistry() {
    return mMemoryTrimmableRegistry;
  }

  @Override
  @MemoryChunkType
  public int getMemoryChunkType() {
    return mMemoryChunkType;
  }

  @Override
  public com.facebook.imagepipeline.producers.NetworkFetcher getNetworkFetcher() {
    return mNetworkFetcher;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory getPlatformBitmapFactory() {
    return mPlatformBitmapFactory;
  }

  @Override
  public com.facebook.imagepipeline.memory.PoolFactory getPoolFactory() {
    return mPoolFactory;
  }

  @Override
  public com.facebook.imagepipeline.decoder.ProgressiveJpegConfig getProgressiveJpegConfig() {
    return mProgressiveJpegConfig;
  }

  @Override
  public Set<com.facebook.imagepipeline.listener.RequestListener> getRequestListeners() {
    return Collections.unmodifiableSet(mRequestListeners);
  }

  @Override
  public Set<com.facebook.imagepipeline.listener.RequestListener2> getRequestListener2s() {
    return Collections.unmodifiableSet(mRequestListener2s);
  }

  @Override
  public boolean isResizeAndRotateEnabledForNetwork() {
    return mResizeAndRotateEnabledForNetwork;
  }

  @Override
  public com.facebook.cache.disk.DiskCacheConfig getSmallImageDiskCacheConfig() {
    return mSmallImageDiskCacheConfig;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.decoder.ImageDecoderConfig getImageDecoderConfig() {
    return mImageDecoderConfig;
  }

  @Override
  @Nullable
  public com.facebook.callercontext.CallerContextVerifier getCallerContextVerifier() {
    return mCallerContextVerifier;
  }

  @Override
  public ImagePipelineExperiments getExperiments() {
    return mImagePipelineExperiments;
  }

  @Override
  public com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker getCloseableReferenceLeakTracker() {
    return mCloseableReferenceLeakTracker;
  }

  public static ImagePipelineConfig.Builder newBuilder(Context context)
  {
    return new Builder(context);
  }

  @Nullable
  private static com.facebook.imagepipeline.transcoder.ImageTranscoderFactory getImageTranscoderFactory(final ImagePipelineConfig.Builder builder)
  {
    if (builder.mImageTranscoderFactory != null && builder.mImageTranscoderType != null) {
      throw new IllegalStateException(
          "You can't define a custom ImageTranscoderFactory and provide an ImageTranscoderType");
    }
    if (builder.mImageTranscoderFactory != null) {
      return builder.mImageTranscoderFactory;
    } else {
      return null; // This member will be constructed by ImagePipelineFactory
    }
  }

  @MemoryChunkType
  private static int getMemoryChunkType(final ImagePipelineConfig.Builder builder, final ImagePipelineExperiments imagePipelineExperiments)
  {
    if (builder.mMemoryChunkType != null) {
      return builder.mMemoryChunkType;
    } else if (imagePipelineExperiments.getMemoryType() == MemoryChunkType.ASHMEM_MEMORY
        && Build.VERSION.SDK_INT >= 27) {
      return MemoryChunkType.ASHMEM_MEMORY;
    } else if (imagePipelineExperiments.getMemoryType() == MemoryChunkType.BUFFER_MEMORY) {
      return MemoryChunkType.BUFFER_MEMORY;
    } else if (imagePipelineExperiments.getMemoryType() == MemoryChunkType.NATIVE_MEMORY) {
      return MemoryChunkType.NATIVE_MEMORY;
    } else {
      return MemoryChunkType.NATIVE_MEMORY;
    }
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> getBitmapCacheOverride() {
    return mBitmapCache;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> getEncodedMemoryCacheOverride() {
    return mEncodedMemoryCache;
  }

  @Override
  public com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory getBitmapMemoryCacheFactory() {
    return mBitmapMemoryCacheFactory;
  }

}
