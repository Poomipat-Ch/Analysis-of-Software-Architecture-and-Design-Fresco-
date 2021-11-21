/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.ui.common.BaseControllerListener2;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.CombinedImageListener;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class CombinedImageListenerImpl implements com.facebook.fresco.vito.core.CombinedImageListener {
  @Nullable
  private com.facebook.fresco.vito.core.VitoImageRequestListener mVitoImageRequestListener;

  @Nullable
  private com.facebook.fresco.vito.listener.ImageListener mImageListener;

  @Nullable
  private com.facebook.fresco.ui.common.ControllerListener2<ImageInfo> mControllerListener2 = 
      BaseControllerListener2.getNoOpListener();

  @Nullable
  private com.facebook.fresco.ui.common.ControllerListener2<ImageInfo> mImagePerfControllerListener;

  @Override
  public void setImageListener(@Nullable com.facebook.fresco.vito.listener.ImageListener imageListener) {
    mImageListener = imageListener;
  }

  @Override
  public void setVitoImageRequestListener(@Nullable com.facebook.fresco.vito.core.VitoImageRequestListener vitoImageRequestListener) {
    mVitoImageRequestListener = vitoImageRequestListener;
  }

  @Override
  @Nullable
  public com.facebook.fresco.vito.listener.ImageListener getImageListener() {
    return mImageListener;
  }

  @Override
  public void setControllerListener2(@Nullable com.facebook.fresco.ui.common.ControllerListener2<ImageInfo> controllerListener2) {
    mControllerListener2 = controllerListener2;
  }

  public void setImagePerfControllerListener(@Nullable com.facebook.fresco.ui.common.ControllerListener2<ImageInfo> imagePerfControllerListener) {
    mImagePerfControllerListener = imagePerfControllerListener;
  }

  @Override
  public void onSubmit(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable Object callerContext, @Nullable com.facebook.fresco.ui.common.ControllerListener2.Extras extras) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onSubmit(id, imageRequest, callerContext, extras);
    }
    if (mImageListener != null) {
      mImageListener.onSubmit(id, callerContext);
    }
    String stringId = VitoUtils.getStringId(id);
    if (mControllerListener2 != null) {
      mControllerListener2.onSubmit(stringId, callerContext, extras);
    }
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onSubmit(stringId, callerContext, extras);
    }
  }

  @Override
  public void onPlaceholderSet(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable Drawable placeholder) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onPlaceholderSet(id, imageRequest, placeholder);
    }
    if (mImageListener != null) {
      mImageListener.onPlaceholderSet(id, placeholder);
    }
  }

  @Override
  public void onFinalImageSet(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @ImageOrigin int imageOrigin, @Nullable com.facebook.imagepipeline.image.ImageInfo imageInfo, @Nullable com.facebook.fresco.ui.common.ControllerListener2.Extras extras, @Nullable Drawable drawable) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onFinalImageSet(
          id, imageRequest, imageOrigin, imageInfo, extras, drawable);
    }
    if (mImageListener != null) {
      mImageListener.onFinalImageSet(id, imageOrigin, imageInfo, drawable);
    }
    String stringId = VitoUtils.getStringId(id);
    if (mControllerListener2 != null) {
      mControllerListener2.onFinalImageSet(stringId, imageInfo, extras);
    }
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onFinalImageSet(stringId, imageInfo, extras);
    }
  }

  @Override
  public void onIntermediateImageSet(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable com.facebook.imagepipeline.image.ImageInfo imageInfo) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onIntermediateImageSet(id, imageRequest, imageInfo);
    }
    if (mImageListener != null) {
      mImageListener.onIntermediateImageSet(id, imageInfo);
    }
    String stringId = VitoUtils.getStringId(id);
    if (mControllerListener2 != null) {
      mControllerListener2.onIntermediateImageSet(stringId, imageInfo);
    }
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onIntermediateImageSet(stringId, imageInfo);
    }
  }

  @Override
  public void onIntermediateImageFailed(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable Throwable throwable) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onIntermediateImageFailed(id, imageRequest, throwable);
    }
    if (mImageListener != null) {
      mImageListener.onIntermediateImageFailed(id, throwable);
    }
    String stringId = VitoUtils.getStringId(id);
    if (mControllerListener2 != null) {
      mControllerListener2.onIntermediateImageFailed(stringId);
    }
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onIntermediateImageFailed(stringId);
    }
  }

  @Override
  public void onFailure(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, @Nullable Drawable error, @Nullable Throwable throwable, @Nullable com.facebook.fresco.ui.common.ControllerListener2.Extras extras) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onFailure(id, imageRequest, error, throwable, extras);
    }
    if (mImageListener != null) {
      mImageListener.onFailure(id, error, throwable);
    }
    String stringId = VitoUtils.getStringId(id);
    if (mControllerListener2 != null) {
      mControllerListener2.onFailure(stringId, throwable, extras);
    }
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onFailure(stringId, throwable, extras);
    }
  }

  @Override
  public void onRelease(long id, com.facebook.fresco.vito.core.VitoImageRequest imageRequest, com.facebook.fresco.ui.common.ControllerListener2.Extras extras) {
    if (mVitoImageRequestListener != null) {
      mVitoImageRequestListener.onRelease(id, imageRequest, extras);
    }
    if (mImageListener != null) {
      mImageListener.onRelease(id);
    }
    String stringId = VitoUtils.getStringId(id);
    if (mControllerListener2 != null) {
      mControllerListener2.onRelease(stringId, extras);
    }
    if (mImagePerfControllerListener != null) {
      mImagePerfControllerListener.onRelease(stringId, extras);
    }
  }

  @Override
  public void onReset() {
    if (mImagePerfControllerListener instanceof Closeable) {
      try {
        ((Closeable) mImagePerfControllerListener).close();
      } catch (IOException e) {
      }
    }
  }

}
