/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.cache;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
/**
 *  Simple bitmap cache that keeps the last frame and reuses it if possible. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class KeepLastFrameCache implements com.facebook.fresco.animation.bitmap.BitmapFrameCache {
  private static final int FRAME_NUMBER_UNSET =  -1;

  private int mLastFrameNumber =  FRAME_NUMBER_UNSET;

  @Nullable
  private com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener mFrameCacheListener;

  @GuardedBy("this")
  @Nullable
  private com.facebook.common.references.CloseableReference<Bitmap> mLastBitmapReference;

  @Nullable
  @Override
  public synchronized com.facebook.common.references.CloseableReference<Bitmap> getCachedFrame(int frameNumber) {
    if (mLastFrameNumber == frameNumber) {
      return CloseableReference.cloneOrNull(mLastBitmapReference);
    }
    return null;
  }

  @Nullable
  @Override
  public synchronized com.facebook.common.references.CloseableReference<Bitmap> getFallbackFrame(int frameNumber) {
    return CloseableReference.cloneOrNull(mLastBitmapReference);
  }

  @Override
  @Nullable
  public synchronized com.facebook.common.references.CloseableReference<Bitmap> getBitmapToReuseForFrame(int frameNumber, int width, int height) {
    try {
      return CloseableReference.cloneOrNull(mLastBitmapReference);
    } finally {
      closeAndResetLastBitmapReference();
    }
  }

  @Override
  public synchronized boolean contains(int frameNumber) {
    return frameNumber == mLastFrameNumber && CloseableReference.isValid(mLastBitmapReference);
  }

  @Override
  public synchronized int getSizeInBytes() {
    return mLastBitmapReference == null ? 0 : BitmapUtil.getSizeInBytes(mLastBitmapReference.get());
  }

  @Override
  public synchronized void clear() {
    closeAndResetLastBitmapReference();
  }

  @Override
  public synchronized void onFrameRendered(int frameNumber, com.facebook.common.references.CloseableReference<Bitmap> bitmapReference, @BitmapAnimationBackend.FrameType int frameType) {
    if (bitmapReference != null
        && mLastBitmapReference != null
        && bitmapReference.get().equals(mLastBitmapReference.get())) {
      return;
    }
    CloseableReference.closeSafely(mLastBitmapReference);
    if (mFrameCacheListener != null && mLastFrameNumber != FRAME_NUMBER_UNSET) {
      mFrameCacheListener.onFrameEvicted(this, mLastFrameNumber);
    }
    mLastBitmapReference = CloseableReference.cloneOrNull(bitmapReference);
    if (mFrameCacheListener != null) {
      mFrameCacheListener.onFrameCached(this, frameNumber);
    }
    mLastFrameNumber = frameNumber;
  }

  @Override
  public void onFramePrepared(int frameNumber, com.facebook.common.references.CloseableReference<Bitmap> bitmapReference, @BitmapAnimationBackend.FrameType int frameType) {
  }

  @Override
  public void setFrameCacheListener(com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener frameCacheListener) {
    mFrameCacheListener = frameCacheListener;
  }

  private synchronized void closeAndResetLastBitmapReference() {
    if (mFrameCacheListener != null && mLastFrameNumber != FRAME_NUMBER_UNSET) {
      mFrameCacheListener.onFrameEvicted(this, mLastFrameNumber);
    }
    CloseableReference.closeSafely(mLastBitmapReference);
    mLastBitmapReference = null;
    mLastFrameNumber = FRAME_NUMBER_UNSET;
  }

}
