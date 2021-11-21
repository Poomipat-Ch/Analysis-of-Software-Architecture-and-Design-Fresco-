/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public interface VitoImagePipeline {
  VitoImageRequest createImageRequest(Resources resources, com.facebook.fresco.vito.source.ImageSource imageSource, @Nullable com.facebook.fresco.vito.options.ImageOptions options) ;

  @Nullable
  com.facebook.common.references.CloseableReference<CloseableImage> getCachedImage(VitoImageRequest imageRequest) ;

  com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> fetchDecodedImage(VitoImageRequest imageRequest, @Nullable Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener, long uiComponentId) ;

}
