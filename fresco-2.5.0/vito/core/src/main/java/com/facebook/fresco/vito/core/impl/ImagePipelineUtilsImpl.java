/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import javax.annotation.Nullable;
/**
 * Utility methods to create {@link ImageRequest}s for {@link
 * com.facebook.fresco.vito.options.ImageOptions}.
 */
public class ImagePipelineUtilsImpl implements com.facebook.fresco.vito.core.ImagePipelineUtils {
  public interface ImageDecodeOptionsProvider {
    @Nullable
    com.facebook.imagepipeline.common.ImageDecodeOptions create(com.facebook.imagepipeline.request.ImageRequestBuilder imageRequestBuilder, com.facebook.fresco.vito.options.DecodedImageOptions imageOptions) ;

  }

  public interface CircularBitmapRounding {
    com.facebook.imagepipeline.common.ImageDecodeOptions getDecodeOptions(boolean antiAliased) ;

  }

  private final ImagePipelineUtilsImpl.ImageDecodeOptionsProvider mImageDecodeOptionsProvider;

  public ImagePipelineUtilsImpl(ImagePipelineUtilsImpl.ImageDecodeOptionsProvider imageDecodeOptionsProvider) {
    mImageDecodeOptionsProvider = imageDecodeOptionsProvider;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest buildImageRequest(@Nullable Uri uri, com.facebook.fresco.vito.options.DecodedImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    final ImageRequestBuilder imageRequestBuilder =
        createEncodedImageRequestBuilder(uri, imageOptions);
    ImageRequestBuilder builder =
        createDecodedImageRequestBuilder(imageRequestBuilder, imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest wrapDecodedImageRequest(com.facebook.imagepipeline.request.ImageRequest imageRequest, com.facebook.fresco.vito.options.DecodedImageOptions imageOptions) {
    ImageRequestBuilder builder =
        createDecodedImageRequestBuilder(
            createEncodedImageRequestBuilder(imageRequest, imageOptions), imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Override
  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest buildEncodedImageRequest(@Nullable Uri uri, com.facebook.fresco.vito.options.EncodedImageOptions imageOptions) {
    ImageRequestBuilder builder = createEncodedImageRequestBuilder(uri, imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Nullable
  protected com.facebook.imagepipeline.request.ImageRequestBuilder createDecodedImageRequestBuilder(@Nullable com.facebook.imagepipeline.request.ImageRequestBuilder imageRequestBuilder, com.facebook.fresco.vito.options.DecodedImageOptions imageOptions) {
    if (imageRequestBuilder == null) {
      return null;
    }

    ResizeOptions resizeOptions = imageOptions.getResizeOptions();
    if (resizeOptions != null) {
      imageRequestBuilder.setResizeOptions(resizeOptions);
    }

    RotationOptions rotationOptions = imageOptions.getRotationOptions();
    if (rotationOptions != null) {
      imageRequestBuilder.setRotationOptions(rotationOptions);
    }

    ImageDecodeOptions imageDecodeOptions =
        mImageDecodeOptionsProvider.create(imageRequestBuilder, imageOptions);
    if (imageDecodeOptions != null) {
      imageRequestBuilder.setImageDecodeOptions(imageDecodeOptions);
    }

    imageRequestBuilder.setLocalThumbnailPreviewsEnabled(
        imageOptions.areLocalThumbnailPreviewsEnabled());

    Postprocessor postprocessor = imageOptions.getPostprocessor();
    if (postprocessor != null) {
      imageRequestBuilder.setPostprocessor(postprocessor);
    }

    if (imageOptions.isProgressiveDecodingEnabled() != null) {
      imageRequestBuilder.setProgressiveRenderingEnabled(
          imageOptions.isProgressiveDecodingEnabled());
    }

    return imageRequestBuilder;
  }

  @Nullable
  protected com.facebook.imagepipeline.request.ImageRequestBuilder createEncodedImageRequestBuilder(@Nullable Uri uri, com.facebook.fresco.vito.options.EncodedImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    return ImageRequestBuilder.newBuilderWithSource(uri)
        .setRequestPriority(imageOptions.getPriority());
  }

  @Nullable
  protected com.facebook.imagepipeline.request.ImageRequestBuilder createEncodedImageRequestBuilder(com.facebook.imagepipeline.request.ImageRequest imageRequest, com.facebook.fresco.vito.options.EncodedImageOptions imageOptions) {
    if (imageRequest == null) {
      return null;
    }
    return ImageRequestBuilder.fromRequest(imageRequest)
        .setRequestPriority(imageOptions.getPriority());
  }

}
