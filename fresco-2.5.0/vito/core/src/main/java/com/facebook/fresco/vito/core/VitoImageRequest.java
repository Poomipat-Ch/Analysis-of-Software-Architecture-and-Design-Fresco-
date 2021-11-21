/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Objects;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public class VitoImageRequest {
  public final Resources resources;

  public final com.facebook.fresco.vito.source.ImageSource imageSource;

  public final com.facebook.fresco.vito.options.ImageOptions imageOptions;

  @Nullable
  public final com.facebook.imagepipeline.request.ImageRequest finalImageRequest;

  @Nullable
  public final com.facebook.cache.common.CacheKey finalImageCacheKey;

  public VitoImageRequest(Resources resources, com.facebook.fresco.vito.source.ImageSource imageSource, com.facebook.fresco.vito.options.ImageOptions imageOptions, @Nullable com.facebook.imagepipeline.request.ImageRequest finalImageRequest, @Nullable com.facebook.cache.common.CacheKey finalImageCacheKey) {
    this.resources = resources;
    this.imageSource = imageSource;
    this.imageOptions = imageOptions;
    this.finalImageRequest = finalImageRequest;
    this.finalImageCacheKey = finalImageCacheKey;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    VitoImageRequest other = (VitoImageRequest) obj;
    return resources == other.resources
        && Objects.equal(imageSource, other.imageSource)
        && Objects.equal(imageOptions, other.imageOptions);
  }

  @Override
  public int hashCode() {
    int result = resources.hashCode();
    result = 31 * result + imageSource.hashCode();
    result = 31 * result + imageOptions.hashCode();
    return result;
  }

}
