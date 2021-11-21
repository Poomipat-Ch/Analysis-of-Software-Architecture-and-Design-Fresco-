/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.common.internal.Objects;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImagePerfData {
  public static final int UNSET =  -1;

  @Nullable
  private final String mControllerId;

  @Nullable
  private final String mRequestId;

  @Nullable
  private final Object mCallerContext;

  @Nullable
  private final com.facebook.imagepipeline.request.ImageRequest mImageRequest;

  @Nullable
  private final com.facebook.imagepipeline.image.ImageInfo mImageInfo;

  /**
   *  Controller image metadata
   */
  @Nullable
  private final com.facebook.imagepipeline.request.ImageRequest mControllerImageRequest;

  @Nullable
  private final com.facebook.imagepipeline.request.ImageRequest mControllerLowResImageRequest;

  @Nullable
  private final com.facebook.imagepipeline.request.ImageRequest[] mControllerFirstAvailableImageRequests;

  private final long mControllerSubmitTimeMs;

  private final long mControllerIntermediateImageSetTimeMs;

  private final long mControllerFinalImageSetTimeMs;

  private final long mControllerFailureTimeMs;

  private final long mControllerCancelTimeMs;

  private final long mImageRequestStartTimeMs;

  private final long mImageRequestEndTimeMs;

  @ImageOrigin
  private final int mImageOrigin;

  @Nullable
  private final String mUltimateProducerName;

  private final boolean mIsPrefetch;

  private final int mOnScreenWidthPx;

  private final int mOnScreenHeightPx;

  @Nullable
  private final Throwable mErrorThrowable;

  /**
   *  Visibility
   */
  @VisibilityState
  private final int mVisibilityState;

  private final long mVisibilityEventTimeMs;

  private final long mInvisibilityEventTimeMs;

  @Nullable
  private final String mComponentTag;

  private final long mImageDrawTimeMs;

  @Nullable
  private final com.facebook.fresco.ui.common.DimensionsInfo mDimensionsInfo;

  @Nullable
  private com.facebook.fresco.ui.common.ControllerListener2.Extras mExtraData;

  public ImagePerfData(@Nullable String controllerId, @Nullable String requestId, @Nullable com.facebook.imagepipeline.request.ImageRequest imageRequest, @Nullable Object callerContext, @Nullable com.facebook.imagepipeline.image.ImageInfo imageInfo, @Nullable com.facebook.imagepipeline.request.ImageRequest controllerImageRequest, @Nullable com.facebook.imagepipeline.request.ImageRequest controllerLowResImageRequest, @Nullable com.facebook.imagepipeline.request.ImageRequest[] controllerFirstAvailableImageRequests, long controllerSubmitTimeMs, long controllerIntermediateImageSetTimeMs, long controllerFinalImageSetTimeMs, long controllerFailureTimeMs, long controllerCancelTimeMs, long imageRequestStartTimeMs, long imageRequestEndTimeMs, @ImageOrigin int imageOrigin, @Nullable String ultimateProducerName, boolean isPrefetch, int onScreenWidthPx, int onScreenHeightPx, @Nullable Throwable errorThrowable, int visibilityState, long visibilityEventTimeMs, long invisibilityEventTime, @Nullable String componentTag, long imageDrawTimeMs, @Nullable com.facebook.fresco.ui.common.DimensionsInfo dimensionsInfo, @Nullable com.facebook.fresco.ui.common.ControllerListener2.Extras extraData) {
    mControllerId = controllerId;
    mRequestId = requestId;
    mImageRequest = imageRequest;
    mCallerContext = callerContext;
    mImageInfo = imageInfo;
    mControllerImageRequest = controllerImageRequest;
    mControllerLowResImageRequest = controllerLowResImageRequest;
    mControllerFirstAvailableImageRequests = controllerFirstAvailableImageRequests;
    mControllerSubmitTimeMs = controllerSubmitTimeMs;
    mControllerIntermediateImageSetTimeMs = controllerIntermediateImageSetTimeMs;
    mControllerFinalImageSetTimeMs = controllerFinalImageSetTimeMs;
    mControllerFailureTimeMs = controllerFailureTimeMs;
    mControllerCancelTimeMs = controllerCancelTimeMs;
    mImageRequestStartTimeMs = imageRequestStartTimeMs;
    mImageRequestEndTimeMs = imageRequestEndTimeMs;
    mImageOrigin = imageOrigin;
    mUltimateProducerName = ultimateProducerName;
    mIsPrefetch = isPrefetch;
    mOnScreenWidthPx = onScreenWidthPx;
    mOnScreenHeightPx = onScreenHeightPx;
    mErrorThrowable = errorThrowable;
    mVisibilityState = visibilityState;
    mVisibilityEventTimeMs = visibilityEventTimeMs;
    mInvisibilityEventTimeMs = invisibilityEventTime;
    mComponentTag = componentTag;
    mImageDrawTimeMs = imageDrawTimeMs;
    mDimensionsInfo = dimensionsInfo;
    mExtraData = extraData;
  }

  public long getImageDrawTimeMs() {
    return mImageDrawTimeMs;
  }

  @Nullable
  public String getControllerId() {
    return mControllerId;
  }

  @Nullable
  public String getRequestId() {
    return mRequestId;
  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest getImageRequest() {
    return mImageRequest;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Nullable
  public com.facebook.imagepipeline.image.ImageInfo getImageInfo() {
    return mImageInfo;
  }

  public long getControllerSubmitTimeMs() {
    return mControllerSubmitTimeMs;
  }

  public long getControllerIntermediateImageSetTimeMs() {
    return mControllerIntermediateImageSetTimeMs;
  }

  public long getControllerFinalImageSetTimeMs() {
    return mControllerFinalImageSetTimeMs;
  }

  public long getControllerFailureTimeMs() {
    return mControllerFailureTimeMs;
  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest getControllerImageRequest() {
    return mControllerImageRequest;
  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest getControllerLowResImageRequest() {
    return mControllerLowResImageRequest;
  }

  @Nullable
  public com.facebook.imagepipeline.request.ImageRequest[] getControllerFirstAvailableImageRequests() {
    return mControllerFirstAvailableImageRequests;
  }

  public long getImageRequestStartTimeMs() {
    return mImageRequestStartTimeMs;
  }

  public long getImageRequestEndTimeMs() {
    return mImageRequestEndTimeMs;
  }

  @ImageOrigin
  public int getImageOrigin() {
    return mImageOrigin;
  }

  @Nullable
  public String getUltimateProducerName() {
    return mUltimateProducerName;
  }

  public boolean isPrefetch() {
    return mIsPrefetch;
  }

  public int getOnScreenWidthPx() {
    return mOnScreenWidthPx;
  }

  public int getOnScreenHeightPx() {
    return mOnScreenHeightPx;
  }

  @Nullable
  public Throwable getErrorThrowable() {
    return mErrorThrowable;
  }

  public long getFinalImageLoadTimeMs() {
    if (getImageRequestEndTimeMs() == UNSET || getImageRequestStartTimeMs() == UNSET) {
      return UNSET;
    }

    return getImageRequestEndTimeMs() - getImageRequestStartTimeMs();
  }

  public long getIntermediateImageLoadTimeMs() {
    if (getControllerIntermediateImageSetTimeMs() == UNSET
        || getControllerSubmitTimeMs() == UNSET) {
      return UNSET;
    }

    return getControllerIntermediateImageSetTimeMs() - getControllerSubmitTimeMs();
  }

  public int getVisibilityState() {
    return mVisibilityState;
  }

  public long getVisibilityEventTimeMs() {
    return mVisibilityEventTimeMs;
  }

  public long getInvisibilityEventTimeMs() {
    return mInvisibilityEventTimeMs;
  }

  @Nullable
  public String getComponentTag() {
    return mComponentTag;
  }

  @Nullable
  public com.facebook.fresco.ui.common.DimensionsInfo getDimensionsInfo() {
    return mDimensionsInfo;
  }

  @Nullable
  public com.facebook.fresco.ui.common.ControllerListener2.Extras getExtraData() {
    return mExtraData;
  }

  public void setExtraData(com.facebook.fresco.ui.common.ControllerListener2.Extras extraData) {
    mExtraData = extraData;
  }

  public String createDebugString() {
    return Objects.toStringHelper(this)
        .add("controller ID", mControllerId)
        .add("request ID", mRequestId)
        .add("controller image request", mControllerImageRequest)
        .add("controller low res image request", mControllerLowResImageRequest)
        .add("controller first available image requests", mControllerFirstAvailableImageRequests)
        .add("controller submit", mControllerSubmitTimeMs)
        .add("controller final image", mControllerFinalImageSetTimeMs)
        .add("controller failure", mControllerFailureTimeMs)
        .add("controller cancel", mControllerCancelTimeMs)
        .add("start time", mImageRequestStartTimeMs)
        .add("end time", mImageRequestEndTimeMs)
        .add("origin", ImageOriginUtils.toString(mImageOrigin))
        .add("ultimateProducerName", mUltimateProducerName)
        .add("prefetch", mIsPrefetch)
        .add("caller context", mCallerContext)
        .add("image request", mImageRequest)
        .add("image info", mImageInfo)
        .add("on-screen width", mOnScreenWidthPx)
        .add("on-screen height", mOnScreenHeightPx)
        .add("visibility state", mVisibilityState)
        .add("component tag", mComponentTag)
        .add("visibility event", mVisibilityEventTimeMs)
        .add("invisibility event", mInvisibilityEventTimeMs)
        .add("image draw event", mImageDrawTimeMs)
        .add("dimensions info", mDimensionsInfo)
        .add("extra data", mExtraData)
        .toString();
  }

}
