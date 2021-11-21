/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import android.net.Uri;
import bolts.Continuation;
import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.datasource.SimpleDataSource;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.datasource.CloseableProducerToDataSourceAdapter;
import com.facebook.imagepipeline.datasource.ProducerToDataSourceAdapter;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import com.facebook.imagepipeline.listener.ForwardingRequestListener2;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.producers.InternalRequestListener;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
/**
 *  The entry point for the image pipeline. 
 */
@ThreadSafe
public class ImagePipeline {
  private static final CancellationException PREFETCH_EXCEPTION = 
      new CancellationException("Prefetching is not enabled");

  private final ProducerSequenceFactory mProducerSequenceFactory;

  private final com.facebook.imagepipeline.listener.RequestListener mRequestListener;

  private final com.facebook.imagepipeline.listener.RequestListener2 mRequestListener2;

  private final com.facebook.common.internal.Supplier<Boolean> mIsPrefetchEnabledSupplier;

  private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;

  private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;

  private final com.facebook.imagepipeline.cache.BufferedDiskCache mMainBufferedDiskCache;

  private final com.facebook.imagepipeline.cache.BufferedDiskCache mSmallImageBufferedDiskCache;

  private final com.facebook.imagepipeline.cache.CacheKeyFactory mCacheKeyFactory;

  private final com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  private final com.facebook.common.internal.Supplier<Boolean> mSuppressBitmapPrefetchingSupplier;

  private AtomicLong mIdCounter;

  private final com.facebook.common.internal.Supplier<Boolean> mLazyDataSource;

  @Nullable
  private final com.facebook.callercontext.CallerContextVerifier mCallerContextVerifier;

  private final ImagePipelineConfigInterface mConfig;

  public ImagePipeline(ProducerSequenceFactory producerSequenceFactory, Set<RequestListener> requestListeners, Set<RequestListener2> requestListener2s, com.facebook.common.internal.Supplier<Boolean> isPrefetchEnabledSupplier, com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache, com.facebook.imagepipeline.cache.MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache, com.facebook.imagepipeline.cache.BufferedDiskCache mainBufferedDiskCache, com.facebook.imagepipeline.cache.BufferedDiskCache smallImageBufferedDiskCache, com.facebook.imagepipeline.cache.CacheKeyFactory cacheKeyFactory, com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue threadHandoffProducerQueue, com.facebook.common.internal.Supplier<Boolean> suppressBitmapPrefetchingSupplier, com.facebook.common.internal.Supplier<Boolean> lazyDataSource, @Nullable com.facebook.callercontext.CallerContextVerifier callerContextVerifier, ImagePipelineConfigInterface config) {
    mIdCounter = new AtomicLong();
    mProducerSequenceFactory = producerSequenceFactory;
    mRequestListener = new ForwardingRequestListener(requestListeners);
    mRequestListener2 = new ForwardingRequestListener2(requestListener2s);
    mIsPrefetchEnabledSupplier = isPrefetchEnabledSupplier;
    mBitmapMemoryCache = bitmapMemoryCache;
    mEncodedMemoryCache = encodedMemoryCache;
    mMainBufferedDiskCache = mainBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mThreadHandoffProducerQueue = threadHandoffProducerQueue;
    mSuppressBitmapPrefetchingSupplier = suppressBitmapPrefetchingSupplier;
    mLazyDataSource = lazyDataSource;
    mCallerContextVerifier = callerContextVerifier;
    mConfig = config;
  }

  /**
   * Generates unique id for RequestFuture.
   * 
   * @return unique id
   */
  public String generateUniqueFutureId() {
    return String.valueOf(mIdCounter.getAndIncrement());
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   * 
   * @param imageRequest the request to submit (what to execute).
   * @param callerContext the caller context of the caller of data source supplier
   * @param requestLevel which level to look down until for the image
   * @return a DataSource representing pending results and completion of the request
   */
  public com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> getDataSourceSupplier(final com.facebook.imagepipeline.request.ImageRequest imageRequest, final Object callerContext, final com.facebook.imagepipeline.request.ImageRequest.RequestLevel requestLevel) {
    return new Supplier<DataSource<CloseableReference<CloseableImage>>>() {
      @Override
      public DataSource<CloseableReference<CloseableImage>> get() {
        return fetchDecodedImage(imageRequest, callerContext, requestLevel);
      }

      @Override
      public String toString() {
        return Objects.toStringHelper(this).add("uri", imageRequest.getSourceUri()).toString();
      }
    };
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   * 
   * @param imageRequest the request to submit (what to execute).
   * @param callerContext the caller context of the caller of data source supplier
   * @param requestLevel which level to look down until for the image
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @return a DataSource representing pending results and completion of the request
   */
  public com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> getDataSourceSupplier(final com.facebook.imagepipeline.request.ImageRequest imageRequest, final Object callerContext, final com.facebook.imagepipeline.request.ImageRequest.RequestLevel requestLevel, @Nullable final com.facebook.imagepipeline.listener.RequestListener requestListener) {
    return new Supplier<DataSource<CloseableReference<CloseableImage>>>() {
      @Override
      public DataSource<CloseableReference<CloseableImage>> get() {
        return fetchDecodedImage(imageRequest, callerContext, requestLevel, requestListener);
      }

      @Override
      public String toString() {
        return Objects.toStringHelper(this).add("uri", imageRequest.getSourceUri()).toString();
      }
    };
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   * 
   * @param imageRequest the request to submit (what to execute).
   * @param callerContext the caller context of the caller of data source supplier
   * @param requestLevel which level to look down until for the image
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @param uiComponentId optional UI component ID requesting the image
   * @return a DataSource representing pending results and completion of the request
   */
  public com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> getDataSourceSupplier(final com.facebook.imagepipeline.request.ImageRequest imageRequest, final Object callerContext, final com.facebook.imagepipeline.request.ImageRequest.RequestLevel requestLevel, @Nullable final com.facebook.imagepipeline.listener.RequestListener requestListener, @Nullable final String uiComponentId) {
    return new Supplier<DataSource<CloseableReference<CloseableImage>>>() {
      @Override
      public DataSource<CloseableReference<CloseableImage>> get() {
        return fetchDecodedImage(
            imageRequest, callerContext, requestLevel, requestListener, uiComponentId);
      }

      @Override
      public String toString() {
        return Objects.toStringHelper(this).add("uri", imageRequest.getSourceUri()).toString();
      }
    };
  }

  /**
   * Returns a DataSource supplier that will on get submit the request for execution and return a
   * DataSource representing the pending results of the task.
   * 
   * @param imageRequest the request to submit (what to execute).
   * @return a DataSource representing pending results and completion of the request
   */
  public com.facebook.common.internal.Supplier<DataSource<CloseableReference<PooledByteBuffer>>> getEncodedImageDataSourceSupplier(final com.facebook.imagepipeline.request.ImageRequest imageRequest, final Object callerContext) {
    return new Supplier<DataSource<CloseableReference<PooledByteBuffer>>>() {
      @Override
      public DataSource<CloseableReference<PooledByteBuffer>> get() {
        return fetchEncodedImage(imageRequest, callerContext);
      }

      @Override
      public String toString() {
        return Objects.toStringHelper(this).add("uri", imageRequest.getSourceUri()).toString();
      }
    };
  }

  /**
   * Submits a request for bitmap cache lookup.
   * 
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @return a DataSource representing the image
   */
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchImageFromBitmapCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext) {
    return fetchDecodedImage(
        imageRequest, callerContext, ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @return a DataSource representing the pending decoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext) {
    return fetchDecodedImage(imageRequest, callerContext, ImageRequest.RequestLevel.FULL_FETCH);
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @return a DataSource representing the pending decoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    return fetchDecodedImage(
        imageRequest, callerContext, ImageRequest.RequestLevel.FULL_FETCH, requestListener);
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param lowestPermittedRequestLevelOnSubmit the lowest request level permitted for image request
   * @return a DataSource representing the pending decoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.request.ImageRequest.RequestLevel lowestPermittedRequestLevelOnSubmit) {
    return fetchDecodedImage(
        imageRequest, callerContext, lowestPermittedRequestLevelOnSubmit, null);
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param lowestPermittedRequestLevelOnSubmit the lowest request level permitted for image reques
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @return a DataSource representing the pending decoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.request.ImageRequest.RequestLevel lowestPermittedRequestLevelOnSubmit, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    return fetchDecodedImage(
        imageRequest, callerContext, lowestPermittedRequestLevelOnSubmit, requestListener, null);
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending decoded
   * image(s).
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @param callerContext the caller context for image request
   * @param lowestPermittedRequestLevelOnSubmit the lowest request level permitted for image reques
   * @param requestListener additional image request listener independent of ImageRequest listeners
   * @param uiComponentId optional UI component ID that is requesting the image
   * @return a DataSource representing the pending decoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.request.ImageRequest.RequestLevel lowestPermittedRequestLevelOnSubmit, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener, @Nullable String uiComponentId) {
    try {
      Producer<CloseableReference<CloseableImage>> producerSequence =
          mProducerSequenceFactory.getDecodedImageProducerSequence(imageRequest);
      return submitFetchRequest(
          producerSequence,
          imageRequest,
          lowestPermittedRequestLevelOnSubmit,
          callerContext,
          requestListener,
          uiComponentId);
    } catch (Exception exception) {
      return DataSources.immediateFailedDataSource(exception);
    }
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending encoded
   * image(s).
   * 
   * <p>The ResizeOptions in the imageRequest will be ignored for this fetch
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @return a DataSource representing the pending encoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<PooledByteBuffer>> fetchEncodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext) {
    return fetchEncodedImage(imageRequest, callerContext, null);
  }

  /**
   * Submits a request for execution and returns a DataSource representing the pending encoded
   * image(s).
   * 
   * <p>The ResizeOptions in the imageRequest will be ignored for this fetch
   * 
   * <p>The returned DataSource must be closed once the client has finished with it.
   * 
   * @param imageRequest the request to submit
   * @return a DataSource representing the pending encoded image(s)
   */
  public com.facebook.datasource.DataSource<CloseableReference<PooledByteBuffer>> fetchEncodedImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    Preconditions.checkNotNull(imageRequest.getSourceUri());
    try {
      Producer<CloseableReference<PooledByteBuffer>> producerSequence =
          mProducerSequenceFactory.getEncodedImageProducerSequence(imageRequest);
      // The resize options are used to determine whether images are going to be downsampled during
      // decode or not. For the case where the image has to be downsampled and it's a local image it
      // will be kept as a FileInputStream until decoding instead of reading it in memory. Since
      // this method returns an encoded image, it should always be read into memory. Therefore, the
      // resize options are ignored to avoid treating the image as if it was to be downsampled
      // during decode.
      if (imageRequest.getResizeOptions() != null) {
        imageRequest = ImageRequestBuilder.fromRequest(imageRequest).setResizeOptions(null).build();
      }
      return submitFetchRequest(
          producerSequence,
          imageRequest,
          ImageRequest.RequestLevel.FULL_FETCH,
          callerContext,
          requestListener,
          null);
    } catch (Exception exception) {
      return DataSources.immediateFailedDataSource(exception);
    }
  }

  /**
   * Submits a request for prefetching to the bitmap cache.
   * 
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   * 
   * @param imageRequest the request to submit
   * @return a DataSource that can safely be ignored.
   */
  public com.facebook.datasource.DataSource<Void> prefetchToBitmapCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext) {
    return prefetchToBitmapCache(imageRequest, callerContext, null);
  }

  public com.facebook.datasource.DataSource<Void> prefetchToBitmapCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ImagePipeline#prefetchToBitmapCache");
      }

      if (!mIsPrefetchEnabledSupplier.get()) {
        return DataSources.immediateFailedDataSource(PREFETCH_EXCEPTION);
      }
      try {
        final Boolean shouldDecodePrefetches = imageRequest.shouldDecodePrefetches();
        final boolean skipBitmapCache =
            shouldDecodePrefetches != null
                ? !shouldDecodePrefetches // use imagerequest param if specified
                : mSuppressBitmapPrefetchingSupplier
                    .get(); // otherwise fall back to pipeline's default
        Producer<Void> producerSequence =
            skipBitmapCache
                ? mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(imageRequest)
                : mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(imageRequest);
        return submitPrefetchRequest(
            producerSequence,
            imageRequest,
            ImageRequest.RequestLevel.FULL_FETCH,
            callerContext,
            Priority.MEDIUM,
            requestListener);
      } catch (Exception exception) {
        return DataSources.immediateFailedDataSource(exception);
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * Submits a request for prefetching to the disk cache with a default priority.
   * 
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   * 
   * @param imageRequest the request to submit
   * @return a DataSource that can safely be ignored.
   */
  public com.facebook.datasource.DataSource<Void> prefetchToDiskCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext) {
    return prefetchToDiskCache(imageRequest, callerContext, Priority.MEDIUM);
  }

  public com.facebook.datasource.DataSource<Void> prefetchToDiskCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    return prefetchToDiskCache(imageRequest, callerContext, Priority.MEDIUM, requestListener);
  }

  /**
   * Submits a request for prefetching to the disk cache.
   * 
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   * 
   * @param imageRequest the request to submit
   * @param priority custom priority for the fetch
   * @return a DataSource that can safely be ignored.
   */
  public com.facebook.datasource.DataSource<Void> prefetchToDiskCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.common.Priority priority) {
    return prefetchToDiskCache(imageRequest, callerContext, priority, null);
  }

  public com.facebook.datasource.DataSource<Void> prefetchToDiskCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.common.Priority priority, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    if (!mIsPrefetchEnabledSupplier.get()) {
      return DataSources.immediateFailedDataSource(PREFETCH_EXCEPTION);
    }
    try {
      Producer<Void> producerSequence =
          mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(imageRequest);
      return submitPrefetchRequest(
          producerSequence,
          imageRequest,
          ImageRequest.RequestLevel.FULL_FETCH,
          callerContext,
          priority,
          requestListener);
    } catch (Exception exception) {
      return DataSources.immediateFailedDataSource(exception);
    }
  }

  /**
   * Submits a request for prefetching to the encoded cache with a default priority.
   * 
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   * 
   * @param imageRequest the request to submit
   * @return a DataSource that can safely be ignored.
   */
  public com.facebook.datasource.DataSource<Void> prefetchToEncodedCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext) {
    return prefetchToEncodedCache(imageRequest, callerContext, Priority.MEDIUM);
  }

  public com.facebook.datasource.DataSource<Void> prefetchToEncodedCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    return prefetchToEncodedCache(imageRequest, callerContext, Priority.MEDIUM, requestListener);
  }

  /**
   * Submits a request for prefetching to the encoded cache.
   * 
   * <p>Beware that if your network fetcher doesn't support priorities prefetch requests may slow
   * down images which are immediately required on screen.
   * 
   * @param imageRequest the request to submit
   * @param priority custom priority for the fetch
   * @return a DataSource that can safely be ignored.
   */
  public com.facebook.datasource.DataSource<Void> prefetchToEncodedCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.common.Priority priority) {
    return prefetchToEncodedCache(imageRequest, callerContext, priority, null);
  }

  public com.facebook.datasource.DataSource<Void> prefetchToEncodedCache(com.facebook.imagepipeline.request.ImageRequest imageRequest, Object callerContext, com.facebook.imagepipeline.common.Priority priority, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ImagePipeline#prefetchToEncodedCache");
      }

      if (!mIsPrefetchEnabledSupplier.get()) {
        return DataSources.immediateFailedDataSource(PREFETCH_EXCEPTION);
      }
      try {
        Producer<Void> producerSequence =
            mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(imageRequest);
        return submitPrefetchRequest(
            producerSequence,
            imageRequest,
            ImageRequest.RequestLevel.FULL_FETCH,
            callerContext,
            priority,
            requestListener);
      } catch (Exception exception) {
        return DataSources.immediateFailedDataSource(exception);
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * Removes all images with the specified {@link Uri} from memory cache.
   * 
   * @param uri The uri of the image to evict
   */
  public void evictFromMemoryCache(final Uri uri) {
    Predicate<CacheKey> predicate = predicateForUri(uri);
    mBitmapMemoryCache.removeAll(predicate);
    mEncodedMemoryCache.removeAll(predicate);
  }

  /**
   * If you have supplied your own cache key factory when configuring the pipeline, this method may
   * not work correctly. It will only work if the custom factory builds the cache key entirely from
   * the URI. If that is not the case, use {@link #evictFromDiskCache(ImageRequest)}.
   * 
   * @param uri The uri of the image to evict
   */
  public void evictFromDiskCache(final Uri uri) {
    evictFromDiskCache(ImageRequest.fromUri(uri));
  }

  /**
   * Removes all images with the specified {@link Uri} from disk cache.
   * 
   * @param imageRequest The imageRequest for the image to evict from disk cache
   */
  public void evictFromDiskCache(final com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, null);
    mMainBufferedDiskCache.remove(cacheKey);
    mSmallImageBufferedDiskCache.remove(cacheKey);
  }

  /**
   * If you have supplied your own cache key factory when configuring the pipeline, this method may
   * not work correctly. It will only work if the custom factory builds the cache key entirely from
   * the URI. If that is not the case, use {@link #evictFromMemoryCache(Uri)} and {@link
   * #evictFromDiskCache(ImageRequest)} separately.
   * 
   * @param uri The uri of the image to evict
   */
  public void evictFromCache(final Uri uri) {
    evictFromMemoryCache(uri);
    evictFromDiskCache(uri);
  }

  /**
   *  Clear the memory caches 
   */
  public void clearMemoryCaches() {
    Predicate<CacheKey> allPredicate =
        new Predicate<CacheKey>() {
          @Override
          public boolean apply(CacheKey key) {
            return true;
          }
        };
    mBitmapMemoryCache.removeAll(allPredicate);
    mEncodedMemoryCache.removeAll(allPredicate);
  }

  /**
   *  Clear disk caches 
   */
  public void clearDiskCaches() {
    mMainBufferedDiskCache.clearAll();
    mSmallImageBufferedDiskCache.clearAll();
  }

  /**
   * Current disk caches size
   * 
   * @return size in Bytes
   */
  public long getUsedDiskCacheSize() {
    return mMainBufferedDiskCache.getSize() + mSmallImageBufferedDiskCache.getSize();
  }

  /**
   *  Clear all the caches (memory and disk) 
   */
  public void clearCaches() {
    clearMemoryCaches();
    clearDiskCaches();
  }

  /**
   * Returns whether the image is stored in the bitmap memory cache.
   * 
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the bitmap memory cache, false otherwise
   */
  public boolean isInBitmapMemoryCache(final Uri uri) {
    if (uri == null) {
      return false;
    }
    Predicate<CacheKey> bitmapCachePredicate = predicateForUri(uri);
    return mBitmapMemoryCache.contains(bitmapCachePredicate);
  }

  /**
   *  @return The Bitmap MemoryCache 
   */
  public com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> getBitmapMemoryCache() {
    return mBitmapMemoryCache;
  }

  /**
   * Returns whether the image is stored in the bitmap memory cache.
   * 
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the bitmap memory cache, false otherwise.
   */
  public boolean isInBitmapMemoryCache(final com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    if (imageRequest == null) {
      return false;
    }
    final CacheKey cacheKey = mCacheKeyFactory.getBitmapCacheKey(imageRequest, null);
    CloseableReference<CloseableImage> ref = mBitmapMemoryCache.get(cacheKey);
    try {
      return CloseableReference.isValid(ref);
    } finally {
      CloseableReference.closeSafely(ref);
    }
  }

  /**
   * Returns whether the image is stored in the encoded memory cache.
   * 
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the encoded memory cache, false otherwise
   */
  public boolean isInEncodedMemoryCache(final Uri uri) {
    if (uri == null) {
      return false;
    }
    Predicate<CacheKey> encodedCachePredicate = predicateForUri(uri);
    return mEncodedMemoryCache.contains(encodedCachePredicate);
  }

  /**
   * Returns whether the image is stored in the encoded memory cache.
   * 
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the encoded memory cache, false otherwise.
   */
  public boolean isInEncodedMemoryCache(final com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    if (imageRequest == null) {
      return false;
    }
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, null);
    CloseableReference<PooledByteBuffer> ref = mEncodedMemoryCache.get(cacheKey);
    try {
      return CloseableReference.isValid(ref);
    } finally {
      CloseableReference.closeSafely(ref);
    }
  }

  /**
   * Returns whether the image is stored in the disk cache. Performs disk cache check synchronously.
   * It is not recommended to use this unless you know what exactly you are doing. Disk cache check
   * is a costly operation, the call will block the caller thread until the cache check is
   * completed.
   * 
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  public boolean isInDiskCacheSync(final Uri uri) {
    return isInDiskCacheSync(uri, ImageRequest.CacheChoice.SMALL)
        || isInDiskCacheSync(uri, ImageRequest.CacheChoice.DEFAULT);
  }

  /**
   * Returns whether the image is stored in the disk cache. Performs disk cache check synchronously.
   * It is not recommended to use this unless you know what exactly you are doing. Disk cache check
   * is a costly operation, the call will block the caller thread until the cache check is
   * completed.
   * 
   * @param uri the uri for the image to be looked up.
   * @param cacheChoice the cacheChoice for the cache to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  public boolean isInDiskCacheSync(final Uri uri, final com.facebook.imagepipeline.request.ImageRequest.CacheChoice cacheChoice) {
    ImageRequest imageRequest =
        ImageRequestBuilder.newBuilderWithSource(uri).setCacheChoice(cacheChoice).build();
    return isInDiskCacheSync(imageRequest);
  }

  /**
   * Performs disk cache check synchronously. It is not recommended to use this unless you know what
   * exactly you are doing. Disk cache check is a costly operation, the call will block the caller
   * thread until the cache check is completed.
   * 
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  public boolean isInDiskCacheSync(final com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, null);
    final ImageRequest.CacheChoice cacheChoice = imageRequest.getCacheChoice();

    switch (cacheChoice) {
      case DEFAULT:
        return mMainBufferedDiskCache.diskCheckSync(cacheKey);
      case SMALL:
        return mSmallImageBufferedDiskCache.diskCheckSync(cacheKey);
      default:
        return false;
    }
  }

  /**
   * Returns whether the image is stored in the disk cache.
   * 
   * <p>If you have supplied your own cache key factory when configuring the pipeline, this method
   * may not work correctly. It will only work if the custom factory builds the cache key entirely
   * from the URI. If that is not the case, use {@link #isInDiskCache(ImageRequest)}.
   * 
   * @param uri the uri for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  public com.facebook.datasource.DataSource<Boolean> isInDiskCache(final Uri uri) {
    return isInDiskCache(ImageRequest.fromUri(uri));
  }

  /**
   * Returns whether the image is stored in the disk cache.
   * 
   * @param imageRequest the imageRequest for the image to be looked up.
   * @return true if the image was found in the disk cache, false otherwise.
   */
  public com.facebook.datasource.DataSource<Boolean> isInDiskCache(final com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, null);
    final SimpleDataSource<Boolean> dataSource = SimpleDataSource.create();
    mMainBufferedDiskCache
        .contains(cacheKey)
        .continueWithTask(
            new Continuation<Boolean, Task<Boolean>>() {
              @Override
              public Task<Boolean> then(Task<Boolean> task) throws Exception {
                if (!task.isCancelled() && !task.isFaulted() && task.getResult()) {
                  return Task.forResult(true);
                }
                return mSmallImageBufferedDiskCache.contains(cacheKey);
              }
            })
        .continueWith(
            new Continuation<Boolean, Void>() {
              @Override
              public Void then(Task<Boolean> task) throws Exception {
                dataSource.setResult(!task.isCancelled() && !task.isFaulted() && task.getResult());
                return null;
              }
            });
    return dataSource;
  }

  /**
   *  @return {@link CacheKey} for doing bitmap cache lookups in the pipeline. 
   */
  @Nullable
  public com.facebook.cache.common.CacheKey getCacheKey(@Nullable com.facebook.imagepipeline.request.ImageRequest imageRequest, @Nullable Object callerContext) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipeline#getCacheKey");
    }
    final CacheKeyFactory cacheKeyFactory = mCacheKeyFactory;
    CacheKey cacheKey = null;
    if (cacheKeyFactory != null && imageRequest != null) {
      if (imageRequest.getPostprocessor() != null) {
        cacheKey = cacheKeyFactory.getPostprocessedBitmapCacheKey(imageRequest, callerContext);
      } else {
        cacheKey = cacheKeyFactory.getBitmapCacheKey(imageRequest, callerContext);
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return cacheKey;
  }

  /**
   * Returns a reference to the cached image
   * 
   * @param cacheKey
   * @return a closeable reference or null if image was not present in cache
   */
  @Nullable
  public com.facebook.common.references.CloseableReference<CloseableImage> getCachedImage(@Nullable com.facebook.cache.common.CacheKey cacheKey) {
    MemoryCache<CacheKey, CloseableImage> memoryCache = mBitmapMemoryCache;
    if (memoryCache == null || cacheKey == null) {
      return null;
    }
    CloseableReference<CloseableImage> closeableImage = memoryCache.get(cacheKey);
    if (closeableImage != null && !closeableImage.get().getQualityInfo().isOfFullQuality()) {
      closeableImage.close();
      return null;
    }
    return closeableImage;
  }

  public boolean hasCachedImage(@Nullable com.facebook.cache.common.CacheKey cacheKey) {
    MemoryCache<CacheKey, CloseableImage> memoryCache = mBitmapMemoryCache;
    if (memoryCache == null || cacheKey == null) {
      return false;
    }
    return memoryCache.contains(cacheKey);
  }

  private <T> com.facebook.datasource.DataSource<CloseableReference<T>> submitFetchRequest(com.facebook.imagepipeline.producers.Producer<CloseableReference<T>> producerSequence, com.facebook.imagepipeline.request.ImageRequest imageRequest, com.facebook.imagepipeline.request.ImageRequest.RequestLevel lowestPermittedRequestLevelOnSubmit, Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener, @Nullable String uiComponentId) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipeline#submitFetchRequest");
    }
    final RequestListener2 requestListener2 =
        new InternalRequestListener(
            getRequestListenerForRequest(imageRequest, requestListener), mRequestListener2);

    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext, false);
    }

    try {
      ImageRequest.RequestLevel lowestPermittedRequestLevel =
          ImageRequest.RequestLevel.getMax(
              imageRequest.getLowestPermittedRequestLevel(), lowestPermittedRequestLevelOnSubmit);
      SettableProducerContext settableProducerContext =
          new SettableProducerContext(
              imageRequest,
              generateUniqueFutureId(),
              uiComponentId,
              requestListener2,
              callerContext,
              lowestPermittedRequestLevel,
              /* isPrefetch */ false,
              imageRequest.getProgressiveRenderingEnabled()
                  || !UriUtil.isNetworkUri(imageRequest.getSourceUri()),
              imageRequest.getPriority(),
              mConfig);
      return CloseableProducerToDataSourceAdapter.create(
          producerSequence, settableProducerContext, requestListener2);
    } catch (Exception exception) {
      return DataSources.immediateFailedDataSource(exception);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  public <T> com.facebook.datasource.DataSource<CloseableReference<T>> submitFetchRequest(com.facebook.imagepipeline.producers.Producer<CloseableReference<T>> producerSequence, com.facebook.imagepipeline.producers.SettableProducerContext settableProducerContext, com.facebook.imagepipeline.listener.RequestListener requestListener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipeline#submitFetchRequest");
    }
    try {
      final RequestListener2 requestListener2 =
          new InternalRequestListener(requestListener, mRequestListener2);

      return CloseableProducerToDataSourceAdapter.create(
          producerSequence, settableProducerContext, requestListener2);
    } catch (Exception exception) {
      return DataSources.immediateFailedDataSource(exception);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  public ProducerSequenceFactory getProducerSequenceFactory() {
    return mProducerSequenceFactory;
  }

  private com.facebook.datasource.DataSource<Void> submitPrefetchRequest(com.facebook.imagepipeline.producers.Producer<Void> producerSequence, com.facebook.imagepipeline.request.ImageRequest imageRequest, com.facebook.imagepipeline.request.ImageRequest.RequestLevel lowestPermittedRequestLevelOnSubmit, Object callerContext, com.facebook.imagepipeline.common.Priority priority, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    final RequestListener2 requestListener2 =
        new InternalRequestListener(
            getRequestListenerForRequest(imageRequest, requestListener), mRequestListener2);

    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext, true);
    }
    try {
      ImageRequest.RequestLevel lowestPermittedRequestLevel =
          ImageRequest.RequestLevel.getMax(
              imageRequest.getLowestPermittedRequestLevel(), lowestPermittedRequestLevelOnSubmit);
      SettableProducerContext settableProducerContext =
          new SettableProducerContext(
              imageRequest,
              generateUniqueFutureId(),
              requestListener2,
              callerContext,
              lowestPermittedRequestLevel,
              /* isPrefetch */ true,
              /* isIntermediateResultExpected */ false,
              priority,
              mConfig);
      return ProducerToDataSourceAdapter.create(
          producerSequence, settableProducerContext, requestListener2);
    } catch (Exception exception) {
      return DataSources.immediateFailedDataSource(exception);
    }
  }

  public com.facebook.imagepipeline.listener.RequestListener getRequestListenerForRequest(com.facebook.imagepipeline.request.ImageRequest imageRequest, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener) {
    if (requestListener == null) {
      if (imageRequest.getRequestListener() == null) {
        return mRequestListener;
      }
      return new ForwardingRequestListener(mRequestListener, imageRequest.getRequestListener());
    } else {
      if (imageRequest.getRequestListener() == null) {
        return new ForwardingRequestListener(mRequestListener, requestListener);
      }
      return new ForwardingRequestListener(
          mRequestListener, requestListener, imageRequest.getRequestListener());
    }
  }

  public com.facebook.imagepipeline.listener.RequestListener getCombinedRequestListener(@Nullable com.facebook.imagepipeline.listener.RequestListener listener) {
    if (listener == null) {
      return mRequestListener;
    }
    return new ForwardingRequestListener(mRequestListener, listener);
  }

  private com.facebook.common.internal.Predicate<CacheKey> predicateForUri(final Uri uri) {
    return new Predicate<CacheKey>() {
      @Override
      public boolean apply(CacheKey key) {
        return key.containsUri(uri);
      }
    };
  }

  public void pause() {
    mThreadHandoffProducerQueue.startQueueing();
  }

  public void resume() {
    mThreadHandoffProducerQueue.stopQueuing();
  }

  public boolean isPaused() {
    return mThreadHandoffProducerQueue.isQueueing();
  }

  public com.facebook.common.internal.Supplier<Boolean> isLazyDataSource() {
    return mLazyDataSource;
  }

  /**
   *  @return The CacheKeyFactory implementation used by ImagePipeline 
   */
  public com.facebook.imagepipeline.cache.CacheKeyFactory getCacheKeyFactory() {
    return mCacheKeyFactory;
  }

  public ImagePipelineConfigInterface getConfig() {
    return mConfig;
  }

}
