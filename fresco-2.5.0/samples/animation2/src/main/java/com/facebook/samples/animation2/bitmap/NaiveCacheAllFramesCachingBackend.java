/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.samples.animation2.bitmap;

import android.graphics.Bitmap;
import android.util.SparseArray;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.imageutils.BitmapUtil;
import javax.annotation.Nullable;
/**
 * This naive implementation does not share Fresco's bitmap cache but has its own LRU. This should
 * just be used for testing. The cache does not support fallback frames or frame re-using.
 */
public class NaiveCacheAllFramesCachingBackend implements com.facebook.fresco.animation.bitmap.BitmapFrameCache {
  private final SparseArray<com.facebook.common.references.CloseableReference<Bitmap>> mBitmapSparseArray =  new SparseArray<>();

  @Nullable
  private com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener mFrameCacheListener;

  @Nullable
  @Override
  public synchronized com.facebook.common.references.CloseableReference<Bitmap> getCachedFrame(int frameNumber) {
    return CloseableReference.cloneOrNull(mBitmapSparseArray.get(frameNumber));
  }

  @Nullable
  @Override
  public com.facebook.common.references.CloseableReference<Bitmap> getFallbackFrame(int frameNumber) {
    // Not supported
    return null;
  }

  @Nullable
  @Override
  public com.facebook.common.references.CloseableReference<Bitmap> getBitmapToReuseForFrame(int frameNumber, int width, int height) {
    // Not supported
    return null;
  }

  @Override
  public synchronized boolean contains(int frameNumber) {
    return CloseableReference.isValid(mBitmapSparseArray.get(frameNumber));
  }

  @Override
  public synchronized int getSizeInBytes() {
    int size = 0;
    for (int i = 0; i < mBitmapSparseArray.size(); i++) {
      size += BitmapUtil.getSizeInBytes(mBitmapSparseArray.valueAt(i).get());
    }
    return size;
  }

  @Override
  public synchronized void clear() {
    for (int i = 0; i < mBitmapSparseArray.size(); i++) {
      CloseableReference.closeSafely(mBitmapSparseArray.valueAt(i));
      if (mFrameCacheListener != null) {
        mFrameCacheListener.onFrameEvicted(this, mBitmapSparseArray.keyAt(i));
      }
    }
    mBitmapSparseArray.clear();
  }

  @Override
  public synchronized void onFrameRendered(int frameNumber, com.facebook.common.references.CloseableReference<Bitmap> bitmapReference, @BitmapAnimationBackend.FrameType int frameType) {
    mBitmapSparseArray.put(frameNumber, CloseableReference.cloneOrNull(bitmapReference));
    if (mFrameCacheListener != null) {
      mFrameCacheListener.onFrameCached(this, frameNumber);
    }
  }

  @Override
  public void onFramePrepared(int frameNumber, com.facebook.common.references.CloseableReference<Bitmap> bitmapReference, @BitmapAnimationBackend.FrameType int frameType) {
  }

  @Override
  public void setFrameCacheListener(com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener frameCacheListener) {
    mFrameCacheListener = frameCacheListener;
  }

}
