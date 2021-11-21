/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.producers;

import android.util.Pair;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 *  Multiplex producer that uses the encoded cache key to combine requests. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class EncodedCacheKeyMultiplexProducer extends MultiplexProducer<, > {
  private final com.facebook.imagepipeline.cache.CacheKeyFactory mCacheKeyFactory;

  public EncodedCacheKeyMultiplexProducer(com.facebook.imagepipeline.cache.CacheKeyFactory cacheKeyFactory, boolean keepCancelledFetchAsLowPriority, Producer inputProducer) {
    super(
        inputProducer,
        "EncodedCacheKeyMultiplexProducer",
        ProducerContext.ExtraKeys.MULTIPLEX_ENCODED_COUNT,
        keepCancelledFetchAsLowPriority);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected Pair<com.facebook.cache.common.CacheKey, ImageRequest.RequestLevel> getKey(ProducerContext producerContext) {
    return Pair.create(
        mCacheKeyFactory.getEncodedCacheKey(
            producerContext.getImageRequest(), producerContext.getCallerContext()),
        producerContext.getLowestPermittedRequestLevel());
  }

  @Nullable
  public com.facebook.imagepipeline.image.EncodedImage cloneOrNull(@Nullable com.facebook.imagepipeline.image.EncodedImage encodedImage) {
    return EncodedImage.cloneOrNull(encodedImage);
  }

}
