/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import android.content.ContentResolver;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.DecodeProducer;
import com.facebook.imagepipeline.producers.DiskCacheReadProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.EncodedProbeProducer;
import com.facebook.imagepipeline.producers.LocalAssetFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriFetchProducer;
import com.facebook.imagepipeline.producers.LocalFileFetchProducer;
import com.facebook.imagepipeline.producers.LocalResourceFetchProducer;
import com.facebook.imagepipeline.producers.LocalVideoThumbnailProducer;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.PostprocessorProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.QualifiedResourceFetchProducer;
import com.facebook.imagepipeline.producers.RemoveImageTransformMetaDataProducer;
import com.facebook.imagepipeline.producers.SwallowResultProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.producers.ThrottlingProducer;
import com.facebook.imagepipeline.producers.ThumbnailBranchProducer;
import com.facebook.imagepipeline.producers.ThumbnailProducer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.Map;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_DATA;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_ASSET;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_CONTENT;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_NETWORK;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ProducerSequenceFactory {
  private final ContentResolver mContentResolver;

  private final ProducerFactory mProducerFactory;

  private final com.facebook.imagepipeline.producers.NetworkFetcher mNetworkFetcher;

  private final boolean mResizeAndRotateEnabledForNetwork;

  private final boolean mWebpSupportEnabled;

  private final boolean mPartialImageCachingEnabled;

  private final com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  private final boolean mDownsampleEnabled;

  private final boolean mUseBitmapPrepareToDraw;

  private final boolean mDiskCacheEnabled;

  private final com.facebook.imagepipeline.transcoder.ImageTranscoderFactory mImageTranscoderFactory;

  private final boolean mIsEncodedMemoryCacheProbingEnabled;

  private final boolean mIsDiskCacheProbingEnabled;

  private final boolean mUseCombinedNetworkAndCacheProducer;

  private final boolean mAllowDelay;

  /**
   *  Saved sequences
   */
  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mNetworkFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<EncodedImage> mBackgroundLocalFileFetchToEncodedMemorySequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<EncodedImage> mBackgroundLocalContentUriFetchToEncodedMemorySequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<EncodedImage> mBackgroundNetworkFetchToEncodedMemorySequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> mLocalFileEncodedImageProducerSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> mLocalContentUriEncodedImageProducerSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> mNetworkEncodedImageProducerSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<Void> mLocalFileFetchToEncodedMemoryPrefetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<Void> mNetworkFetchToEncodedMemoryPrefetchSequence;

  @Nullable
  private com.facebook.imagepipeline.producers.Producer<EncodedImage> mCommonNetworkFetchToEncodedMemorySequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mLocalImageFileFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mLocalVideoFileFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mLocalContentUriFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mLocalResourceFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mLocalAssetFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mDataFetchSequence;

  @VisibleForTesting
  @Nullable
  com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> mQualifiedResourceFetchSequence;

  @VisibleForTesting
  Map<com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>>, Producer<CloseableReference<CloseableImage>>> mPostprocessorSequences;

  @VisibleForTesting
  Map<com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>>, Producer<Void>> mCloseableImagePrefetchSequences;

  @VisibleForTesting
  Map<com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>>, Producer<CloseableReference<CloseableImage>>> mBitmapPrepareSequences;

  public ProducerSequenceFactory(ContentResolver contentResolver, ProducerFactory producerFactory, com.facebook.imagepipeline.producers.NetworkFetcher networkFetcher, boolean resizeAndRotateEnabledForNetwork, boolean webpSupportEnabled, com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue threadHandoffProducerQueue, boolean downSampleEnabled, boolean useBitmapPrepareToDraw, boolean partialImageCachingEnabled, boolean diskCacheEnabled, com.facebook.imagepipeline.transcoder.ImageTranscoderFactory imageTranscoderFactory, boolean isEncodedMemoryCacheProbingEnabled, boolean isDiskCacheProbingEnabled, boolean useCombinedNetworkAndCacheProducer, boolean allowDelay) {
    mContentResolver = contentResolver;
    mProducerFactory = producerFactory;
    mNetworkFetcher = networkFetcher;
    mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
    mWebpSupportEnabled = webpSupportEnabled;
    mUseCombinedNetworkAndCacheProducer = useCombinedNetworkAndCacheProducer;
    mPostprocessorSequences = new HashMap<>();
    mCloseableImagePrefetchSequences = new HashMap<>();
    mBitmapPrepareSequences = new HashMap<>();
    mThreadHandoffProducerQueue = threadHandoffProducerQueue;
    mDownsampleEnabled = downSampleEnabled;
    mUseBitmapPrepareToDraw = useBitmapPrepareToDraw;
    mPartialImageCachingEnabled = partialImageCachingEnabled;
    mDiskCacheEnabled = diskCacheEnabled;
    mImageTranscoderFactory = imageTranscoderFactory;
    mIsEncodedMemoryCacheProbingEnabled = isEncodedMemoryCacheProbingEnabled;
    mIsDiskCacheProbingEnabled = isDiskCacheProbingEnabled;
    mAllowDelay = allowDelay;
  }

  /**
   * Returns a sequence that can be used for a request for an encoded image from either network or
   * local files.
   * 
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> getEncodedImageProducerSequence(com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ProducerSequenceFactory#getEncodedImageProducerSequence");
      }
      validateEncodedImageRequest(imageRequest);
      final Uri uri = imageRequest.getSourceUri();

      switch (imageRequest.getSourceUriType()) {
        case SOURCE_TYPE_NETWORK:
          return getNetworkFetchEncodedImageProducerSequence();
        case SOURCE_TYPE_LOCAL_VIDEO_FILE:
        case SOURCE_TYPE_LOCAL_IMAGE_FILE:
          return getLocalFileFetchEncodedImageProducerSequence();
        case SOURCE_TYPE_LOCAL_CONTENT:
          return getLocalContentUriFetchEncodedImageProducerSequence();
        default:
          throw new IllegalArgumentException(
              "Unsupported uri scheme for encoded image fetch! Uri is: "
                  + getShortenedUriString(uri));
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   *  Returns a sequence that can be used for a request for an encoded image from network. 
   */
  public com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> getNetworkFetchEncodedImageProducerSequence() {
    synchronized (this) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getNetworkFetchEncodedImageProducerSequence");
      }
      if (mNetworkEncodedImageProducerSequence == null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection(
              "ProducerSequenceFactory#getNetworkFetchEncodedImageProducerSequence:init");
        }
        mNetworkEncodedImageProducerSequence =
            new RemoveImageTransformMetaDataProducer(
                getBackgroundNetworkFetchToEncodedMemorySequence());
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    return mNetworkEncodedImageProducerSequence;
  }

  /**
   *  Returns a sequence that can be used for a request for an encoded image from a local file. 
   */
  public com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> getLocalFileFetchEncodedImageProducerSequence() {
    synchronized (this) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getLocalFileFetchEncodedImageProducerSequence");
      }
      if (mLocalFileEncodedImageProducerSequence == null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection(
              "ProducerSequenceFactory#getLocalFileFetchEncodedImageProducerSequence:init");
        }
        mLocalFileEncodedImageProducerSequence =
            new RemoveImageTransformMetaDataProducer(
                getBackgroundLocalFileFetchToEncodeMemorySequence());
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    return mLocalFileEncodedImageProducerSequence;
  }

  /**
   * Returns a sequence that can be used for a request for an encoded image from a local content
   * uri.
   */
  public com.facebook.imagepipeline.producers.Producer<CloseableReference<PooledByteBuffer>> getLocalContentUriFetchEncodedImageProducerSequence() {
    synchronized (this) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getLocalContentUriFetchEncodedImageProducerSequence");
      }
      if (mLocalContentUriEncodedImageProducerSequence == null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection(
              "ProducerSequenceFactory#getLocalContentUriFetchEncodedImageProducerSequence:init");
        }
        mLocalContentUriEncodedImageProducerSequence =
            new RemoveImageTransformMetaDataProducer(
                getBackgroundLocalContentUriFetchToEncodeMemorySequence());
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    return mLocalContentUriEncodedImageProducerSequence;
  }

  /**
   * Returns a sequence that can be used for a prefetch request for an encoded image.
   * 
   * <p>Guaranteed to return the same sequence as {@code getEncodedImageProducerSequence(request)},
   * except that it is pre-pended with a {@link SwallowResultProducer}.
   * 
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public com.facebook.imagepipeline.producers.Producer<Void> getEncodedImagePrefetchProducerSequence(com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    validateEncodedImageRequest(imageRequest);

    switch (imageRequest.getSourceUriType()) {
      case SOURCE_TYPE_NETWORK:
        return getNetworkFetchToEncodedMemoryPrefetchSequence();
      case SOURCE_TYPE_LOCAL_VIDEO_FILE:
      case SOURCE_TYPE_LOCAL_IMAGE_FILE:
        return getLocalFileFetchToEncodedMemoryPrefetchSequence();
      default:
        final Uri uri = imageRequest.getSourceUri();
        throw new IllegalArgumentException(
            "Unsupported uri scheme for encoded image fetch! Uri is: "
                + getShortenedUriString(uri));
    }
  }

  private static void validateEncodedImageRequest(com.facebook.imagepipeline.request.ImageRequest imageRequest)
  {
    Preconditions.checkNotNull(imageRequest);
    Preconditions.checkArgument(
        imageRequest.getLowestPermittedRequestLevel().getValue()
            <= ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.getValue());
  }

  /**
   * Returns a sequence that can be used for a request for a decoded image.
   * 
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getDecodedImageProducerSequence(com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#getDecodedImageProducerSequence");
    }
    Producer<CloseableReference<CloseableImage>> pipelineSequence =
        getBasicDecodedImageSequence(imageRequest);

    if (imageRequest.getPostprocessor() != null) {
      pipelineSequence = getPostprocessorSequence(pipelineSequence);
    }

    if (mUseBitmapPrepareToDraw) {
      pipelineSequence = getBitmapPrepareSequence(pipelineSequence);
    }

    if (mAllowDelay && imageRequest.getDelayMs() > 0) {
      pipelineSequence = getDelaySequence(pipelineSequence);
    }

    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return pipelineSequence;
  }

  /**
   * Returns a sequence that can be used for a prefetch request for a decoded image.
   * 
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public com.facebook.imagepipeline.producers.Producer<Void> getDecodedImagePrefetchProducerSequence(com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    Producer<CloseableReference<CloseableImage>> inputProducer =
        getBasicDecodedImageSequence(imageRequest);

    if (mUseBitmapPrepareToDraw) {
      inputProducer = getBitmapPrepareSequence(inputProducer);
    }

    return getDecodedImagePrefetchSequence(inputProducer);
  }

  private com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getBasicDecodedImageSequence(com.facebook.imagepipeline.request.ImageRequest imageRequest) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ProducerSequenceFactory#getBasicDecodedImageSequence");
      }
      Preconditions.checkNotNull(imageRequest);

      Uri uri = imageRequest.getSourceUri();
      Preconditions.checkNotNull(uri, "Uri is null.");

      switch (imageRequest.getSourceUriType()) {
        case SOURCE_TYPE_NETWORK:
          return getNetworkFetchSequence();
        case SOURCE_TYPE_LOCAL_VIDEO_FILE:
          return getLocalVideoFileFetchSequence();
        case SOURCE_TYPE_LOCAL_IMAGE_FILE:
          return getLocalImageFileFetchSequence();
        case SOURCE_TYPE_LOCAL_CONTENT:
          if (MediaUtils.isVideo(mContentResolver.getType(uri))) {
            return getLocalVideoFileFetchSequence();
          }
          return getLocalContentUriFetchSequence();
        case SOURCE_TYPE_LOCAL_ASSET:
          return getLocalAssetFetchSequence();
        case SOURCE_TYPE_LOCAL_RESOURCE:
          return getLocalResourceFetchSequence();
        case SOURCE_TYPE_QUALIFIED_RESOURCE:
          return getQualifiedResourceFetchSequence();
        case SOURCE_TYPE_DATA:
          return getDataFetchSequence();
        default:
          throw new IllegalArgumentException(
              "Unsupported uri scheme! Uri is: " + getShortenedUriString(uri));
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * swallow result if prefetch -> bitmap cache get -> background thread hand-off -> multiplex ->
   * bitmap cache -> decode -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * network fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getNetworkFetchSequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#getNetworkFetchSequence");
    }
    if (mNetworkFetchSequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ProducerSequenceFactory#getNetworkFetchSequence:init");
      }
      mNetworkFetchSequence =
          newBitmapCacheGetToDecodeSequence(getCommonNetworkFetchToEncodedMemorySequence());
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mNetworkFetchSequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * network fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<EncodedImage> getBackgroundNetworkFetchToEncodedMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getBackgroundNetworkFetchToEncodedMemorySequence");
    }
    if (mBackgroundNetworkFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getBackgroundNetworkFetchToEncodedMemorySequence:init");
      }
      // Use hand-off producer to ensure that we don't do any unnecessary work on the UI thread.
      mBackgroundNetworkFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              getCommonNetworkFetchToEncodedMemorySequence(), mThreadHandoffProducerQueue);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mBackgroundNetworkFetchToEncodedMemorySequence;
  }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache -> disk cache ->
   * (webp transcode) -> network fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<Void> getNetworkFetchToEncodedMemoryPrefetchSequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getNetworkFetchToEncodedMemoryPrefetchSequence");
    }
    if (mNetworkFetchToEncodedMemoryPrefetchSequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getNetworkFetchToEncodedMemoryPrefetchSequence:init");
      }
      mNetworkFetchToEncodedMemoryPrefetchSequence =
          mProducerFactory.newSwallowResultProducer(
              getBackgroundNetworkFetchToEncodedMemorySequence());
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mNetworkFetchToEncodedMemoryPrefetchSequence;
  }

  /**
   * multiplex -> encoded cache -> disk cache -> (webp transcode) -> network fetch. Alternatively,
   * multiplex -> combined network and cache
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<EncodedImage> getCommonNetworkFetchToEncodedMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getCommonNetworkFetchToEncodedMemorySequence");
    }
    if (mCommonNetworkFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getCommonNetworkFetchToEncodedMemorySequence:init");
      }
      Producer<EncodedImage> inputProducer =
          Preconditions.checkNotNull(
              mUseCombinedNetworkAndCacheProducer
                  ? mProducerFactory.newCombinedNetworkAndCacheProducer(mNetworkFetcher)
                  : newEncodedCacheMultiplexToTranscodeSequence(
                      mProducerFactory.newNetworkFetchProducer(mNetworkFetcher)));
      mCommonNetworkFetchToEncodedMemorySequence =
          ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer);

      mCommonNetworkFetchToEncodedMemorySequence =
          mProducerFactory.newResizeAndRotateProducer(
              mCommonNetworkFetchToEncodedMemorySequence,
              mResizeAndRotateEnabledForNetwork && !mDownsampleEnabled,
              mImageTranscoderFactory);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mCommonNetworkFetchToEncodedMemorySequence;
  }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache -> disk cache ->
   * (webp transcode) -> local file fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<Void> getLocalFileFetchToEncodedMemoryPrefetchSequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getLocalFileFetchToEncodedMemoryPrefetchSequence");
    }
    if (mLocalFileFetchToEncodedMemoryPrefetchSequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getLocalFileFetchToEncodedMemoryPrefetchSequence:init");
      }
      mLocalFileFetchToEncodedMemoryPrefetchSequence =
          mProducerFactory.newSwallowResultProducer(
              getBackgroundLocalFileFetchToEncodeMemorySequence());
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mLocalFileFetchToEncodedMemoryPrefetchSequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * local file fetch
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<EncodedImage> getBackgroundLocalFileFetchToEncodeMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getBackgroundLocalFileFetchToEncodeMemorySequence");
    }
    if (mBackgroundLocalFileFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getBackgroundLocalFileFetchToEncodeMemorySequence:init");
      }
      final LocalFileFetchProducer localFileFetchProducer =
          mProducerFactory.newLocalFileFetchProducer();

      final Producer<EncodedImage> toEncodedMultiplexProducer =
          newEncodedCacheMultiplexToTranscodeSequence(localFileFetchProducer);

      mBackgroundLocalFileFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              toEncodedMultiplexProducer, mThreadHandoffProducerQueue);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mBackgroundLocalFileFetchToEncodedMemorySequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * local content resolver fetch
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<EncodedImage> getBackgroundLocalContentUriFetchToEncodeMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getBackgroundLocalContentUriFetchToEncodeMemorySequence");
    }
    if (mBackgroundLocalContentUriFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getBackgroundLocalContentUriFetchToEncodeMemorySequence:init");
      }
      final LocalContentUriFetchProducer localFileFetchProducer =
          mProducerFactory.newLocalContentUriFetchProducer();

      final Producer<EncodedImage> toEncodedMultiplexProducer =
          newEncodedCacheMultiplexToTranscodeSequence(localFileFetchProducer);

      mBackgroundLocalContentUriFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              toEncodedMultiplexProducer, mThreadHandoffProducerQueue);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mBackgroundLocalContentUriFetchToEncodedMemorySequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * file fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getLocalImageFileFetchSequence() {
    if (mLocalImageFileFetchSequence == null) {
      LocalFileFetchProducer localFileFetchProducer = mProducerFactory.newLocalFileFetchProducer();
      mLocalImageFileFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(localFileFetchProducer);
    }
    return mLocalImageFileFetchSequence;
  }

  /**
   *  Bitmap cache get -> thread hand off -> multiplex -> bitmap cache -> local video thumbnail 
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getLocalVideoFileFetchSequence() {
    if (mLocalVideoFileFetchSequence == null) {
      LocalVideoThumbnailProducer localVideoThumbnailProducer =
          mProducerFactory.newLocalVideoThumbnailProducer();
      mLocalVideoFileFetchSequence =
          newBitmapCacheGetToBitmapCacheSequence(localVideoThumbnailProducer);
    }
    return mLocalVideoFileFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> thumbnail resize and rotate -> thumbnail branch -> local content
   * thumbnail creation -> exif thumbnail creation -> local image resize and rotate -> add meta data
   * producer -> multiplex -> encoded cache -> (webp transcode) -> local content uri fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getLocalContentUriFetchSequence() {
    if (mLocalContentUriFetchSequence == null) {
      LocalContentUriFetchProducer localContentUriFetchProducer =
          mProducerFactory.newLocalContentUriFetchProducer();

      ThumbnailProducer<EncodedImage>[] thumbnailProducers = new ThumbnailProducer[2];
      thumbnailProducers[0] = mProducerFactory.newLocalContentUriThumbnailFetchProducer();
      thumbnailProducers[1] = mProducerFactory.newLocalExifThumbnailProducer();

      mLocalContentUriFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(
              localContentUriFetchProducer, thumbnailProducers);
    }
    return mLocalContentUriFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) ->
   * qualified resource fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getQualifiedResourceFetchSequence() {
    if (mQualifiedResourceFetchSequence == null) {
      QualifiedResourceFetchProducer qualifiedResourceFetchProducer =
          mProducerFactory.newQualifiedResourceFetchProducer();
      mQualifiedResourceFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(qualifiedResourceFetchProducer);
    }
    return mQualifiedResourceFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * resource fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getLocalResourceFetchSequence() {
    if (mLocalResourceFetchSequence == null) {
      LocalResourceFetchProducer localResourceFetchProducer =
          mProducerFactory.newLocalResourceFetchProducer();
      mLocalResourceFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(localResourceFetchProducer);
    }
    return mLocalResourceFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * asset fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getLocalAssetFetchSequence() {
    if (mLocalAssetFetchSequence == null) {
      LocalAssetFetchProducer localAssetFetchProducer =
          mProducerFactory.newLocalAssetFetchProducer();
      mLocalAssetFetchSequence = newBitmapCacheGetToLocalTransformSequence(localAssetFetchProducer);
    }
    return mLocalAssetFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> bitmap cache -> decode -> resize and rotate
   * -> (webp transcode) -> data fetch.
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getDataFetchSequence() {
    if (mDataFetchSequence == null) {
      Producer<EncodedImage> inputProducer = mProducerFactory.newDataFetchProducer();
      if (WebpSupportStatus.sIsWebpSupportRequired
          && (!mWebpSupportEnabled || WebpSupportStatus.sWebpBitmapFactory == null)) {
        inputProducer = mProducerFactory.newWebpTranscodeProducer(inputProducer);
      }
      inputProducer = mProducerFactory.newAddImageTransformMetaDataProducer(inputProducer);
      inputProducer =
          mProducerFactory.newResizeAndRotateProducer(inputProducer, true, mImageTranscoderFactory);
      mDataFetchSequence = newBitmapCacheGetToDecodeSequence(inputProducer);
    }
    return mDataFetchSequence;
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   * 
   * @param inputProducer the source producer
   * @return the new sequence
   */
  private com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToLocalTransformSequence(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    ThumbnailProducer<EncodedImage>[] defaultThumbnailProducers = new ThumbnailProducer[1];
    defaultThumbnailProducers[0] = mProducerFactory.newLocalExifThumbnailProducer();
    return newBitmapCacheGetToLocalTransformSequence(inputProducer, defaultThumbnailProducers);
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   * 
   * @param inputProducer the source producer
   * @param thumbnailProducers the thumbnail producers from which to request the image before
   *     falling back to the full image producer sequence
   * @return the new sequence
   */
  private com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToLocalTransformSequence(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer, com.facebook.imagepipeline.producers.ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    inputProducer = newEncodedCacheMultiplexToTranscodeSequence(inputProducer);
    Producer<EncodedImage> inputProducerAfterDecode =
        newLocalTransformationsSequence(inputProducer, thumbnailProducers);
    return newBitmapCacheGetToDecodeSequence(inputProducerAfterDecode);
  }

  /**
   * Same as {@code newBitmapCacheGetToBitmapCacheSequence} but with an extra DecodeProducer.
   * 
   * @param inputProducer producer providing the input to the decode
   * @return bitmap cache get to decode sequence
   */
  private com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToDecodeSequence(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#newBitmapCacheGetToDecodeSequence");
    }
    DecodeProducer decodeProducer = mProducerFactory.newDecodeProducer(inputProducer);
    Producer<CloseableReference<CloseableImage>> result =
        newBitmapCacheGetToBitmapCacheSequence(decodeProducer);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  /**
   * encoded cache multiplex -> encoded cache -> (disk cache) -> (webp transcode)
   * 
   * @param inputProducer producer providing the input to the transcode
   * @return encoded cache multiplex to webp transcode sequence
   */
  private com.facebook.imagepipeline.producers.Producer<EncodedImage> newEncodedCacheMultiplexToTranscodeSequence(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    if (WebpSupportStatus.sIsWebpSupportRequired
        && (!mWebpSupportEnabled || WebpSupportStatus.sWebpBitmapFactory == null)) {
      inputProducer = mProducerFactory.newWebpTranscodeProducer(inputProducer);
    }
    if (mDiskCacheEnabled) {
      inputProducer = newDiskCacheSequence(inputProducer);
    }
    EncodedMemoryCacheProducer encodedMemoryCacheProducer =
        mProducerFactory.newEncodedMemoryCacheProducer(inputProducer);
    if (mIsDiskCacheProbingEnabled) {
      EncodedProbeProducer probeProducer =
          mProducerFactory.newEncodedProbeProducer(encodedMemoryCacheProducer);
      return mProducerFactory.newEncodedCacheKeyMultiplexProducer(probeProducer);
    }
    return mProducerFactory.newEncodedCacheKeyMultiplexProducer(encodedMemoryCacheProducer);
  }

  private com.facebook.imagepipeline.producers.Producer<EncodedImage> newDiskCacheSequence(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer) {
    Producer<EncodedImage> cacheWriteProducer;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#newDiskCacheSequence");
    }
    if (mPartialImageCachingEnabled) {
      Producer<EncodedImage> partialDiskCacheProducer =
          mProducerFactory.newPartialDiskCacheProducer(inputProducer);
      cacheWriteProducer = mProducerFactory.newDiskCacheWriteProducer(partialDiskCacheProducer);
    } else {
      cacheWriteProducer = mProducerFactory.newDiskCacheWriteProducer(inputProducer);
    }
    DiskCacheReadProducer result = mProducerFactory.newDiskCacheReadProducer(cacheWriteProducer);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  /**
   * Bitmap cache get -> thread hand off -> multiplex -> bitmap cache
   * 
   * @param inputProducer producer providing the input to the bitmap cache
   * @return bitmap cache get to bitmap cache sequence
   */
  private com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToBitmapCacheSequence(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    BitmapMemoryCacheProducer bitmapMemoryCacheProducer =
        mProducerFactory.newBitmapMemoryCacheProducer(inputProducer);
    BitmapMemoryCacheKeyMultiplexProducer bitmapKeyMultiplexProducer =
        mProducerFactory.newBitmapMemoryCacheKeyMultiplexProducer(bitmapMemoryCacheProducer);
    Producer<CloseableReference<CloseableImage>> threadHandoffProducer =
        mProducerFactory.newBackgroundThreadHandoffProducer(
            bitmapKeyMultiplexProducer, mThreadHandoffProducerQueue);
    if (mIsEncodedMemoryCacheProbingEnabled || mIsDiskCacheProbingEnabled) {
      BitmapMemoryCacheGetProducer bitmapMemoryCacheGetProducer =
          mProducerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer);
      return mProducerFactory.newBitmapProbeProducer(bitmapMemoryCacheGetProducer);
    }
    return mProducerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer);
  }

  /**
   * Branch on separate images -> thumbnail resize and rotate -> thumbnail producers as provided ->
   * local image resize and rotate -> add meta data producer
   * 
   * @param inputProducer producer providing the input to add meta data producer
   * @param thumbnailProducers the thumbnail producers from which to request the image before
   *     falling back to the full image producer sequence
   * @return local transformations sequence
   */
  private com.facebook.imagepipeline.producers.Producer<EncodedImage> newLocalTransformationsSequence(com.facebook.imagepipeline.producers.Producer<EncodedImage> inputProducer, com.facebook.imagepipeline.producers.ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    Producer<EncodedImage> localImageProducer =
        ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer);
    localImageProducer =
        mProducerFactory.newResizeAndRotateProducer(
            localImageProducer, true, mImageTranscoderFactory);
    ThrottlingProducer<EncodedImage> localImageThrottlingProducer =
        mProducerFactory.newThrottlingProducer(localImageProducer);
    return mProducerFactory.newBranchOnSeparateImagesProducer(
        newLocalThumbnailProducer(thumbnailProducers), localImageThrottlingProducer);
  }

  private com.facebook.imagepipeline.producers.Producer<EncodedImage> newLocalThumbnailProducer(com.facebook.imagepipeline.producers.ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    ThumbnailBranchProducer thumbnailBranchProducer =
        mProducerFactory.newThumbnailBranchProducer(thumbnailProducers);
    return mProducerFactory.newResizeAndRotateProducer(
        thumbnailBranchProducer, true, mImageTranscoderFactory);
  }

  /**
   *  post-processor producer -> copy producer -> inputProducer 
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getPostprocessorSequence(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    Producer<CloseableReference<CloseableImage>> result =
        mPostprocessorSequences.get(inputProducer);
    if (result == null) {
      PostprocessorProducer postprocessorProducer =
          mProducerFactory.newPostprocessorProducer(inputProducer);
      result = mProducerFactory.newPostprocessorBitmapMemoryCacheProducer(postprocessorProducer);
      mPostprocessorSequences.put(inputProducer, result);
    }
    return result;
  }

  /**
   *  swallow result producer -> inputProducer 
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<Void> getDecodedImagePrefetchSequence(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    Producer<Void> result = mCloseableImagePrefetchSequences.get(inputProducer);
    if (result == null) {
      result = mProducerFactory.newSwallowResultProducer(inputProducer);
      mCloseableImagePrefetchSequences.put(inputProducer, result);
    }
    return result;
  }

  /**
   *  bitmap prepare producer -> inputProducer 
   */
  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getBitmapPrepareSequence(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {
    Producer<CloseableReference<CloseableImage>> bitmapPrepareProducer =
        mBitmapPrepareSequences.get(inputProducer);

    if (bitmapPrepareProducer == null) {
      bitmapPrepareProducer = mProducerFactory.newBitmapPrepareProducer(inputProducer);
      mBitmapPrepareSequences.put(inputProducer, bitmapPrepareProducer);
    }

    return bitmapPrepareProducer;
  }

  private synchronized com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> getDelaySequence(com.facebook.imagepipeline.producers.Producer<CloseableReference<CloseableImage>> inputProducer) {

    Producer<CloseableReference<CloseableImage>> delayProducer =
        mProducerFactory.newDelayProducer(inputProducer);
    return delayProducer;
  }

  private static String getShortenedUriString(Uri uri)
  {
    final String uriString = String.valueOf(uri);
    return uriString.length() > 30 ? uriString.substring(0, 30) + "..." : uriString;
  }

}
