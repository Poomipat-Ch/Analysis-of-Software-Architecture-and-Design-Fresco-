/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImagePerfRequestListener extends com.facebook.imagepipeline.listener.BaseRequestListener {
  private final com.facebook.common.time.MonotonicClock mClock;

  private final com.facebook.drawee.backends.pipeline.info.ImagePerfState mImagePerfState;

  public ImagePerfRequestListener(com.facebook.common.time.MonotonicClock monotonicClock, com.facebook.drawee.backends.pipeline.info.ImagePerfState imagePerfState) {
    mClock = monotonicClock;
    mImagePerfState = imagePerfState;
  }

  @Override
  public void onRequestStart(com.facebook.imagepipeline.request.ImageRequest request, Object callerContext, String requestId, boolean isPrefetch) {
    mImagePerfState.setImageRequestStartTimeMs(mClock.now());

    mImagePerfState.setImageRequest(request);
    mImagePerfState.setCallerContext(callerContext);
    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setPrefetch(isPrefetch);
  }

  @Override
  public void onRequestSuccess(com.facebook.imagepipeline.request.ImageRequest request, String requestId, boolean isPrefetch) {
    mImagePerfState.setImageRequestEndTimeMs(mClock.now());

    mImagePerfState.setImageRequest(request);
    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setPrefetch(isPrefetch);
  }

  @Override
  public void onRequestFailure(com.facebook.imagepipeline.request.ImageRequest request, String requestId, Throwable throwable, boolean isPrefetch) {
    mImagePerfState.setImageRequestEndTimeMs(mClock.now());

    mImagePerfState.setImageRequest(request);
    mImagePerfState.setRequestId(requestId);
    mImagePerfState.setPrefetch(isPrefetch);
  }

  @Override
  public void onRequestCancellation(String requestId) {
    mImagePerfState.setImageRequestEndTimeMs(mClock.now());

    mImagePerfState.setRequestId(requestId);
  }

}
