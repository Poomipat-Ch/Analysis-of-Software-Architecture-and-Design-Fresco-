/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.image;

import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 * Encapsulates the data needed in order for {@code AnimatedDrawable} to render a {@code
 * AnimatedImage}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class CloseableAnimatedImage extends CloseableImage {
  @Nullable
  private com.facebook.imagepipeline.animated.base.AnimatedImageResult mImageResult;

  private boolean mIsStateful;

  public CloseableAnimatedImage(com.facebook.imagepipeline.animated.base.AnimatedImageResult imageResult) {
    this(imageResult, true);
  }

  public CloseableAnimatedImage(com.facebook.imagepipeline.animated.base.AnimatedImageResult imageResult, boolean isStateful) {
    mImageResult = imageResult;
    mIsStateful = isStateful;
  }

  @Override
  public synchronized int getWidth() {
    return mImageResult == null ? 0 : mImageResult.getImage().getWidth();
  }

  @Override
  public synchronized int getHeight() {
    return mImageResult == null ? 0 : mImageResult.getImage().getHeight();
  }

  @Override
  public void close() {
    AnimatedImageResult imageResult;
    synchronized (this) {
      if (mImageResult == null) {
        return;
      }
      imageResult = mImageResult;
      mImageResult = null;
    }
    imageResult.dispose();
  }

  @Override
  public synchronized boolean isClosed() {
    return mImageResult == null;
  }

  @Override
  public synchronized int getSizeInBytes() {
    return mImageResult == null ? 0 : mImageResult.getImage().getSizeInBytes();
  }

  @Override
  public boolean isStateful() {
    return mIsStateful;
  }

  @Nullable
  public synchronized com.facebook.imagepipeline.animated.base.AnimatedImageResult getImageResult() {
    return mImageResult;
  }

  @Nullable
  public synchronized com.facebook.imagepipeline.animated.base.AnimatedImage getImage() {
    return mImageResult == null ? null : mImageResult.getImage();
  }

}
