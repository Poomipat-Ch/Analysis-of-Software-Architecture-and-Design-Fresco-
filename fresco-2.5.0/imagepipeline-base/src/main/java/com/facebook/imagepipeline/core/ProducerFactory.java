/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import androidx.annotation.Nullable;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BoundedLinkedHashSet;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.AddImageTransformMetaDataProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.BitmapPrepareProducer;
import com.facebook.imagepipeline.producers.BitmapProbeProducer;
import com.facebook.imagepipeline.producers.BranchOnSeparateImagesProducer;
import com.facebook.imagepipeline.producers.DataFetchProducer;
import com.facebook.imagepipeline.producers.DecodeProducer;
import com.facebook.imagepipeline.producers.DelayProducer;
import com.facebook.imagepipeline.producers.DiskCacheReadProducer;
import com.facebook.imagepipeline.producers.DiskCacheWriteProducer;
import com.facebook.imagepipeline.producers.EncodedCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.EncodedProbeProducer;
import com.facebook.imagepipeline.producers.LocalAssetFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriThumbnailFetchProducer;
import com.facebook.imagepipeline.producers.LocalExifThumbnailProducer;
import com.facebook.imagepipeline.producers.LocalFileFetchProducer;
import com.facebook.imagepipeline.producers.LocalResourceFetchProducer;
import com.facebook.imagepipeline.producers.LocalVideoThumbnailProducer;
import com.facebook.imagepipeline.producers.NetworkFetchProducer;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.PartialDiskCacheProducer;
import com.facebook.imagepipeline.producers.PostprocessedBitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.PostprocessorProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.QualifiedResourceFetchProducer;
import com.facebook.imagepipeline.producers.ResizeAndRotateProducer;
import com.facebook.imagepipeline.producers.SwallowResultProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.producers.ThrottlingProducer;
import com.facebook.imagepipeline.producers.ThumbnailBranchProducer;
import com.facebook.imagepipeline.producers.ThumbnailProducer;
import com.facebook.imagepipeline.producers.WebpTranscodeProducer;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ProducerFactory {
  private static final int MAX_SIMULTANEOUS_REQUESTS =  5;

  /**
   *  Local dependencies
   */
  protected ContentResolver mContentResolver;

  protected Resources mResources;

  protected AssetManager mAssetManager;

  /**
   *  Decode dependencies
   */
  protected final com.facebook.common.memory.ByteArrayPool mByteArrayPool;

  protected final com.facebook.imagepipeline.decoder.ImageDecoder mImageDecoder;

  protected final com.facebook.imagepipeline.decoder.ProgressiveJpegConfig mProgressiveJpegConfig;

  protected final boolean mDownsampleEnabled;

  protected final boolean mResizeAndRotateEnabledForNetwork;

  protected final boolean mDecodeCancellationEnabled;

  /**
   *  Dependencies used by multiple steps
   */
  protected final ExecutorSupplier mExecutorSupplier;

  protected final com.facebook.common.memory.PooledByteBufferFactory mPooledByteBufferFactory;

  /**
   *  Cache dependencies
   */
  protected final com.facebook.imagepipeline.cache.BufferedDiskCache mDefaultBufferedDiskCache;

  protected final com.facebook.imagepipeline.cache.BufferedDiskCache mSmallImageBufferedDiskCache;

  protected final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;

  protected final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;

  protected final com.facebook.imagepipeline.cache.CacheKeyFactory mCacheKeyFactory;

  protected final com.facebook.imagepipeline.cache.BoundedLinkedHashSet<CacheKey> mEncodedMemoryCacheHistory;

  protected final com.facebook.imagepipeline.cache.BoundedLinkedHashSet<CacheKey> mDiskCacheHistory;

  /**
   *  Postproc dependencies
   */
  protected final com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory mPlatformBitmapFactory;

  /**
   *  BitmapPrepare dependencies
   */
  protected final int mBitmapPrepareToDrawMinSizeBytes;

  protected final int mBitmapPrepareToDrawMaxSizeBytes;

  protected boolean mBitmapPrepareToDrawForPrefetch;

  /**
   *  Core factory dependencies
   */
  protected final CloseableReferenceFactory mCloseableReferenceFactory;

  protected final int mMaxBitmapSize;

  protected final boolean mKeepCancelledFetchAsLowPriority;

  public ProducerFactory(Context context, com.facebook.common.memory.ByteArrayPool byteArrayPool, com.facebook.imagepipeline.decoder.ImageDecoder imageDecoder, com.facebook.imagepipeline.decoder.ProgressiveJpegConfig progressiveJpegConfig, boolean downsampleEnabled, boolean resizeAndRotateEnabledForNetwork, boolean decodeCancellationEnabled, ExecutorSupplier executorSupplier, com.facebook.common.memory.PooledByteBufferFactory pooledByteBufferFactory, com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache, com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache, com.facebook.imagepipeline.cache.BufferedDiskCache defaultBufferedDiskCache, com.facebook.imagepipeline.cache.BufferedDiskCache smallImageBufferedDiskCache, com.facebook.imagepipeline.cache.CacheKeyFactory cacheKeyFactory, com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory platformBitmapFactory, int bitmapPrepareToDrawMinSizeBytes, int bitmapPrepareToDrawMaxSizeBytes, boolean bitmapPrepareToDrawForPrefetch, int maxBitmapSize, CloseableReferenceFactory closeableReferenceFactory, boolean keepCancelledFetchAsLowPriority, int trackedKeysSize) {
    mContentResolver = context.getApplicationContext().getContentResolver();
    mResources = context.getApplicationContext().getResources();
    mAssetManager = context.getApplicationContext().getAssets();

    mByteArrayPool = byteArrayPool;
    mImageDecoder = imageDecoder;
    mProgressiveJpegConfig = progressiveJpegConfig;
    mDownsampleEnabled = downsampleEnabled;
    mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
    mDecodeCancellationEnabled = decodeCancellationEnabled;

    mExecutorSupplier = executorSupplier;
    mPooledByteBufferFactory = pooledByteBufferFactory;

    mBitmapMemoryCache = bitmapMemoryCache;
    mEncodedMemoryCache = encodedMemoryCache;
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mPlatformBitmapFactory = platformBitmapFactory;
    mEncodedMemoryCacheHistory = new BoundedLinkedHashSet<>(trackedKeysSize);
    mDiskCacheHistory = new BoundedLinkedHashSet<>(trackedKeysSize);

    mBitmapPrepareToDrawMinSizeBytes = bitmapPrepareToDrawMinSizeBytes;
    mBitmapPrepareToDrawMaxSizeBytes = bitmapPrepareToDrawMaxSizeBytes;
    mBitmapPrepareToDrawForPrefetch = bitmapPrepareToDrawForPrefetch;

    mMaxBitmapSize = maxBitmapSize;
    mCloseableReferenceFactory = closeableReferenceFactory;

    mKeepCancelledFetchAsLowPriority = keepCancelledFetchAsLowPriority;
  }

  public static com.facebook.imagepipeline.producers.AddImageTransformMetaDataProducer newAddImageTransformMetaDataProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer)
  {
    return new AddImageTransformMetaDataProducer(inputProducer);
  }

  public com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer newBitmapMemoryCacheGetProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapMemoryCacheGetProducer(mBitmapMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer newBitmapMemoryCacheKeyMultiplexProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapMemoryCacheKeyMultiplexProducer(mCacheKeyFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer newBitmapMemoryCacheProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapMemoryCacheProducer(mBitmapMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public static com.facebook.imagepipeline.producers.BranchOnSeparateImagesProducer newBranchOnSeparateImagesProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer1, com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer2)
  {
    return new BranchOnSeparateImagesProducer(inputProducer1, inputProducer2);
  }

  public com.facebook.imagepipeline.producers.DataFetchProducer newDataFetchProducer() {
    return new DataFetchProducer(mPooledByteBufferFactory);
  }

  public com.facebook.imagepipeline.producers.DecodeProducer newDecodeProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new DecodeProducer(
        mByteArrayPool,
        mExecutorSupplier.forDecode(),
        mImageDecoder,
        mProgressiveJpegConfig,
        mDownsampleEnabled,
        mResizeAndRotateEnabledForNetwork,
        mDecodeCancellationEnabled,
        inputProducer,
        mMaxBitmapSize,
        mCloseableReferenceFactory,
        null,
        Suppliers.BOOLEAN_FALSE);
  }

  public com.facebook.imagepipeline.producers.DiskCacheReadProducer newDiskCacheReadProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new DiskCacheReadProducer(
        mDefaultBufferedDiskCache, mSmallImageBufferedDiskCache, mCacheKeyFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.DiskCacheWriteProducer newDiskCacheWriteProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new DiskCacheWriteProducer(
        mDefaultBufferedDiskCache, mSmallImageBufferedDiskCache, mCacheKeyFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.PartialDiskCacheProducer newPartialDiskCacheProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new PartialDiskCacheProducer(
        mDefaultBufferedDiskCache,
        mCacheKeyFactory,
        mPooledByteBufferFactory,
        mByteArrayPool,
        inputProducer);
  }

  public com.facebook.imagepipeline.producers.EncodedCacheKeyMultiplexProducer newEncodedCacheKeyMultiplexProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new EncodedCacheKeyMultiplexProducer(
        mCacheKeyFactory, mKeepCancelledFetchAsLowPriority, inputProducer);
  }

  public com.facebook.imagepipeline.producers.BitmapProbeProducer newBitmapProbeProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapProbeProducer(
        mEncodedMemoryCache,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mEncodedMemoryCacheHistory,
        mDiskCacheHistory,
        inputProducer);
  }

  public com.facebook.imagepipeline.producers.EncodedProbeProducer newEncodedProbeProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new EncodedProbeProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mEncodedMemoryCacheHistory,
        mDiskCacheHistory,
        inputProducer);
  }

  public com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer newEncodedMemoryCacheProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new EncodedMemoryCacheProducer(mEncodedMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.LocalAssetFetchProducer newLocalAssetFetchProducer() {
    return new LocalAssetFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mAssetManager);
  }

  public com.facebook.imagepipeline.producers.LocalContentUriFetchProducer newLocalContentUriFetchProducer() {
    return new LocalContentUriFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mContentResolver);
  }

  public com.facebook.imagepipeline.producers.LocalContentUriThumbnailFetchProducer newLocalContentUriThumbnailFetchProducer() {
    return new LocalContentUriThumbnailFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mContentResolver);
  }

  public com.facebook.imagepipeline.producers.LocalExifThumbnailProducer newLocalExifThumbnailProducer() {
    return new LocalExifThumbnailProducer(
        mExecutorSupplier.forThumbnailProducer(), mPooledByteBufferFactory, mContentResolver);
  }

  public com.facebook.imagepipeline.producers.ThumbnailBranchProducer newThumbnailBranchProducer(com.facebook.imagepipeline.producers.ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    return new ThumbnailBranchProducer(thumbnailProducers);
  }

  public com.facebook.imagepipeline.producers.LocalFileFetchProducer newLocalFileFetchProducer() {
    return new LocalFileFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory);
  }

  public com.facebook.imagepipeline.producers.QualifiedResourceFetchProducer newQualifiedResourceFetchProducer() {
    return new QualifiedResourceFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mContentResolver);
  }

  public com.facebook.imagepipeline.producers.LocalResourceFetchProducer newLocalResourceFetchProducer() {
    return new LocalResourceFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mResources);
  }

  public com.facebook.imagepipeline.producers.LocalVideoThumbnailProducer newLocalVideoThumbnailProducer() {
    return new LocalVideoThumbnailProducer(
        mExecutorSupplier.forLocalStorageRead(), mContentResolver);
  }

  public com.facebook.imagepipeline.producers.Producer<EncodedImage> newNetworkFetchProducer(com.facebook.imagepipeline.producers.NetworkFetcher networkFetcher) {
    return new NetworkFetchProducer(mPooledByteBufferFactory, mByteArrayPool, networkFetcher);
  }

  public com.facebook.imagepipeline.producers.PostprocessedBitmapMemoryCacheProducer newPostprocessorBitmapMemoryCacheProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new PostprocessedBitmapMemoryCacheProducer(
        mBitmapMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.PostprocessorProducer newPostprocessorProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new PostprocessorProducer(
        inputProducer, mPlatformBitmapFactory, mExecutorSupplier.forBackgroundTasks());
  }

  public com.facebook.imagepipeline.producers.ResizeAndRotateProducer newResizeAndRotateProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer, final boolean isResizingEnabled, com.facebook.imagepipeline.transcoder.ImageTranscoderFactory imageTranscoderFactory) {
    return new ResizeAndRotateProducer(
        mExecutorSupplier.forBackgroundTasks(),
        mPooledByteBufferFactory,
        inputProducer,
        isResizingEnabled,
        imageTranscoderFactory);
  }

  public <T> com.facebook.imagepipeline.producers.SwallowResultProducer<T> newSwallowResultProducer(com.facebook.imagepipeline.producers.Producer<T> inputProducer) {
    return new SwallowResultProducer<T>(inputProducer);
  }

  public <T> com.facebook.imagepipeline.producers.Producer<T> newBackgroundThreadHandoffProducer(com.facebook.imagepipeline.producers.Producer<T> inputProducer, com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue inputThreadHandoffProducerQueue) {
    return new ThreadHandoffProducer<T>(inputProducer, inputThreadHandoffProducerQueue);
  }

  public <T> com.facebook.imagepipeline.producers.ThrottlingProducer<T> newThrottlingProducer(com.facebook.imagepipeline.producers.Producer<T> inputProducer) {
    return new ThrottlingProducer<T>(
        MAX_SIMULTANEOUS_REQUESTS,
        mExecutorSupplier.forLightweightBackgroundTasks(),
        inputProducer);
  }

  public com.facebook.imagepipeline.producers.WebpTranscodeProducer newWebpTranscodeProducer(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    return new WebpTranscodeProducer(
        mExecutorSupplier.forBackgroundTasks(), mPooledByteBufferFactory, inputProducer);
  }

  public com.facebook.imagepipeline.producers.BitmapPrepareProducer newBitmapPrepareProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapPrepareProducer(
        inputProducer,
        mBitmapPrepareToDrawMinSizeBytes,
        mBitmapPrepareToDrawMaxSizeBytes,
        mBitmapPrepareToDrawForPrefetch);
  }

  public com.facebook.imagepipeline.producers.DelayProducer newDelayProducer(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new DelayProducer(
        inputProducer, mExecutorSupplier.scheduledExecutorServiceForBackgroundTasks());
  }

  @Nullable
  public com.facebook.imagepipeline.producers.Producer<EncodedImage> newCombinedNetworkAndCacheProducer(final com.facebook.imagepipeline.producers.NetworkFetcher networkFetcher) {
    return null;
  }

}
