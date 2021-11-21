/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import android.content.Context;
import android.graphics.Bitmap;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoderConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Set;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ImagePipelineConfigInterface {
  Bitmap.Config getBitmapConfig() ;

  com.facebook.common.internal.Supplier<MemoryCacheParams> getBitmapMemoryCacheParamsSupplier() ;

  com.facebook.imagepipeline.cache.MemoryCache.CacheTrimStrategy getBitmapMemoryCacheTrimStrategy() ;

  @Nullable
  com.facebook.imagepipeline.cache.CountingMemoryCache.EntryStateObserver<CacheKey> getBitmapMemoryCacheEntryStateObserver() ;

  com.facebook.imagepipeline.cache.CacheKeyFactory getCacheKeyFactory() ;

  Context getContext() ;

  FileCacheFactory getFileCacheFactory() ;

  boolean isDownsampleEnabled() ;

  boolean isDiskCacheEnabled() ;

  com.facebook.common.internal.Supplier<MemoryCacheParams> getEncodedMemoryCacheParamsSupplier() ;

  ExecutorSupplier getExecutorSupplier() ;

  @Nullable
  com.facebook.common.executors.SerialExecutorService getExecutorServiceForAnimatedImages() ;

  com.facebook.imagepipeline.cache.ImageCacheStatsTracker getImageCacheStatsTracker() ;

  @Nullable
  com.facebook.imagepipeline.decoder.ImageDecoder getImageDecoder() ;

  @Nullable
  com.facebook.imagepipeline.transcoder.ImageTranscoderFactory getImageTranscoderFactory() ;

  @Nullable
  @ImageTranscoderType
  Integer getImageTranscoderType() ;

  com.facebook.common.internal.Supplier<Boolean> getIsPrefetchEnabledSupplier() ;

  com.facebook.cache.disk.DiskCacheConfig getMainDiskCacheConfig() ;

  com.facebook.common.memory.MemoryTrimmableRegistry getMemoryTrimmableRegistry() ;

  @MemoryChunkType
  int getMemoryChunkType() ;

  com.facebook.imagepipeline.producers.NetworkFetcher getNetworkFetcher() ;

  @Nullable
  com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory getPlatformBitmapFactory() ;

  com.facebook.imagepipeline.memory.PoolFactory getPoolFactory() ;

  com.facebook.imagepipeline.decoder.ProgressiveJpegConfig getProgressiveJpegConfig() ;

  Set<com.facebook.imagepipeline.listener.RequestListener> getRequestListeners() ;

  Set<com.facebook.imagepipeline.listener.RequestListener2> getRequestListener2s() ;

  boolean isResizeAndRotateEnabledForNetwork() ;

  com.facebook.cache.disk.DiskCacheConfig getSmallImageDiskCacheConfig() ;

  @Nullable
  com.facebook.imagepipeline.decoder.ImageDecoderConfig getImageDecoderConfig() ;

  @Nullable
  com.facebook.callercontext.CallerContextVerifier getCallerContextVerifier() ;

  ImagePipelineExperiments getExperiments() ;

  com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker getCloseableReferenceLeakTracker() ;

  @Nullable
  com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> getBitmapCacheOverride() ;

  @Nullable
  com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> getEncodedMemoryCacheOverride() ;

  com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory getBitmapMemoryCacheFactory() ;

}
