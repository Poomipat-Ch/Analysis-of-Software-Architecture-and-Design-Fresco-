/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.PrefetchTarget;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NoOpFrescoVitoPrefetcher implements com.facebook.fresco.vito.core.FrescoVitoPrefetcher {
  private static final String EXCEPTION_MSG =  "Image prefetching with Fresco Vito is disabled!";

  @Override
  public com.facebook.datasource.DataSource<Void> prefetch(com.facebook.fresco.vito.core.PrefetchTarget prefetchTarget, Uri uri, @Nullable com.facebook.fresco.vito.options.ImageOptions imageOptions, @Nullable Object callerContext, String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public com.facebook.datasource.DataSource<Void> prefetchToBitmapCache(Uri uri, @Nullable com.facebook.fresco.vito.options.DecodedImageOptions imageOptions, @Nullable Object callerContext, String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public com.facebook.datasource.DataSource<Void> prefetchToEncodedCache(Uri uri, @Nullable com.facebook.fresco.vito.options.EncodedImageOptions imageOptions, @Nullable Object callerContext, String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public com.facebook.datasource.DataSource<Void> prefetchToDiskCache(Uri uri, @Nullable com.facebook.fresco.vito.options.ImageOptions imageOptions, @Nullable Object callerContext, String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

  @Override
  public com.facebook.datasource.DataSource<Void> prefetch(com.facebook.fresco.vito.core.PrefetchTarget prefetchTarget, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable Object callerContext, @Nullable com.facebook.imagepipeline.listener.RequestListener requestListener, String callsite) {
    throw new UnsupportedOperationException(EXCEPTION_MSG);
  }

}
