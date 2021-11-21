/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl.source;

import androidx.annotation.Nullable;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.STRICT)
public interface VitoImageSource extends com.facebook.fresco.vito.source.ImageSource {
  /**
   * Get the final image request for the last image if known. In some cases, like for @{link
 * FirstAvailableImageSource}, we do not know the final image request, so this method will return
   * null.
   * 
   * @param imagePipelineUtils util class to create the final image request
   * @param imageOptions the image options to use, important if for example rounding is done at
   *     decode time
   * @return the final image request or null if not possible to determine
   */
  @Nullable
  com.facebook.imagepipeline.request.ImageRequest maybeExtractFinalImageRequest(com.facebook.fresco.vito.core.ImagePipelineUtils imagePipelineUtils, com.facebook.fresco.vito.options.ImageOptions imageOptions) ;

  com.facebook.common.internal.Supplier<DataSource<CloseableReference<CloseableImage>>> createDataSourceSupplier(final com.facebook.imagepipeline.core.ImagePipeline imagePipeline, final com.facebook.fresco.vito.core.ImagePipelineUtils imagePipelineUtils, final com.facebook.fresco.vito.options.ImageOptions imageOptions, @Nullable final Object callerContext, @Nullable final com.facebook.imagepipeline.listener.RequestListener requestListener, final String uiComponentId) ;

}
