/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.impl.source.VitoImageSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 *  Vito image pipeline to fetch an image for a given VitoImageRequest. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoImagePipelineImpl implements com.facebook.fresco.vito.core.VitoImagePipeline {
  private final com.facebook.imagepipeline.core.ImagePipeline mImagePipeline;

  private final com.facebook.fresco.vito.core.ImagePipelineUtils mImagePipelineUtils;

  public VitoImagePipelineImpl(com.facebook.imagepipeline.core.ImagePipeline imagePipeline, com.facebook.fresco.vito.core.ImagePipelineUtils imagePipelineUtils) {
    mImagePipeline = imagePipeline;
    mImagePipelineUtils = imagePipelineUtils;
  }

  @Override
  public com.facebook.fresco.vito.core.VitoImageRequest createImageRequest(Resources resources, com.facebook.fresco.vito.source.ImageSource imageSource, @Nullable com.facebook.fresco.vito.options.ImageOptions options) {
    if (options == null) {
      options = ImageOptions.defaults();
    }
    if (!(imageSource instanceof VitoImageSource)) {
      throw new IllegalArgumentException("ImageSource not supported: " + imageSource);
    }
    VitoImageSource vitoImageSource = (VitoImageSource) imageSource;
    CacheKey finalImageCacheKey = null;
    ImageRequest finalImageRequest =
        vitoImageSource.maybeExtractFinalImageRequest(mImagePipelineUtils, options);

    if (finalImageRequest != null) {
      finalImageCacheKey = mImagePipeline.getCacheKey(finalImageRequest, null);
    }
    return new VitoImageRequest(
        resources, imageSource, options, finalImageRequest, finalImageCacheKey);
  }

  @Override
  @Nullable
  public com.facebook.common.references.CloseableReference<CloseableImage> getCachedImage(com.facebook.fresco.vito.core.VitoImageRequest imageRequest) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("VitoImagePipeline#getCachedImage");
    }
    try {
      CloseableReference<CloseableImage> cachedImageReference =
          mImagePipeline.getCachedImage(imageRequest.finalImageCacheKey);
      if (CloseableReference.isValid(cachedImageReference)) {
        return cachedImageReference;
      }
      return null;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(final com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable final Object callerContext, @Nullable final com.facebook.imagepipeline.listener.RequestListener requestListener, final long uiComponentId) {
    if (!(imageRequest.imageSource instanceof VitoImageSource)) {
      return DataSources.immediateFailedDataSource(
          new IllegalArgumentException("Unknown ImageSource " + imageRequest.imageSource));
    }
    VitoImageSource vitoImageSource = (VitoImageSource) imageRequest.imageSource;
    final String stringId = VitoUtils.getStringId(uiComponentId);
    return vitoImageSource
        .createDataSourceSupplier(
            mImagePipeline,
            mImagePipelineUtils,
            imageRequest.imageOptions,
            callerContext,
            requestListener,
            stringId)
        .get();
  }

}
