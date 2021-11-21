/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.multiuri;

import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 * Data class to enable using functionality of {@link
 * com.facebook.datasource.IncreasingQualityDataSourceSupplier} and/or {@link
 * com.facebook.datasource.FirstAvailableDataSourceSupplier} with Vito
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class MultiUri {
  @Nullable
  private com.facebook.imagepipeline.request.ImageRequest mLowResImageRequest;

  @Nullable
  private com.facebook.imagepipeline.request.ImageRequest[] mMultiImageRequests;

  @Nullable
  private com.facebook.imagepipeline.request.ImageRequest mHighResImageRequest;

  private MultiUri(MultiUri.Builder builder) {
    mLowResImageRequest = builder.mLowResImageRequest;
    mHighResImageRequest = builder.mHighResImageRequest;
    mMultiImageRequests = builder.mMultiImageRequests;
  }

  public static class Builder {
    @Nullable
    private com.facebook.imagepipeline.request.ImageRequest mLowResImageRequest;

    @Nullable
    private com.facebook.imagepipeline.request.ImageRequest mHighResImageRequest;

    @Nullable
    private com.facebook.imagepipeline.request.ImageRequest[] mMultiImageRequests;

    private Builder() {
    }

    public MultiUri build() {
      return new MultiUri(this);
    }

    public MultiUri.Builder setLowResImageRequest(@Nullable com.facebook.imagepipeline.request.ImageRequest lowResImageRequest) {
      mLowResImageRequest = lowResImageRequest;
      return this;
    }

    public MultiUri.Builder setHighResImageRequest(@Nullable com.facebook.imagepipeline.request.ImageRequest highResImageRequest) {
      mHighResImageRequest = highResImageRequest;
      return this;
    }

    public MultiUri.Builder setImageRequests(@Nullable ImageRequest...multiImageRequests ) {
      mMultiImageRequests = multiImageRequests;
      return this;
    }

  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest getLowResImageRequest() {
    return mLowResImageRequest;
  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest getHighResImageRequest() {
    return mHighResImageRequest;
  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest[] getMultiImageRequests() {
    return mMultiImageRequests;
  }

  public static MultiUri.Builder create()
  {
    return new Builder();
  }

}
