/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePerfImageOriginListener implements com.facebook.drawee.backends.pipeline.info.ImageOriginListener {
  private final com.facebook.drawee.backends.pipeline.info.ImagePerfState mImagePerfState;

  private final com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor mImagePerfMonitor;

  public ImagePerfImageOriginListener(com.facebook.drawee.backends.pipeline.info.ImagePerfState imagePerfState, com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor imagePerfMonitor) {
    mImagePerfState = imagePerfState;
    mImagePerfMonitor = imagePerfMonitor;
  }

  @Override
  public void onImageLoaded(String controllerId, @ImageOrigin int imageOrigin, boolean successful, @Nullable String ultimateProducerName) {
    mImagePerfState.setImageOrigin(imageOrigin);
    mImagePerfState.setUltimateProducerName(ultimateProducerName);
    mImagePerfMonitor.notifyStatusUpdated(mImagePerfState, ImageLoadStatus.ORIGIN_AVAILABLE);
  }

}
