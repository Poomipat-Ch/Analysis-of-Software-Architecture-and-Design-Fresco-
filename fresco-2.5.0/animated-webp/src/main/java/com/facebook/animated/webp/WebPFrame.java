/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.animated.webp;

import android.graphics.Bitmap;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.concurrent.ThreadSafe;
/**
 *  A single frame of a {@link WebPImage}. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
public class WebPFrame implements com.facebook.imagepipeline.animated.base.AnimatedImageFrame {
  /**
   *  Accessed by native methods
   */
  @SuppressWarnings("unused")
  @DoNotStrip
  private long mNativeContext;

  /**
   * Constructs the frame with the native pointer. This is called by native code.
   * 
   * @param nativeContext the native pointer
   */
  @DoNotStrip
  WebPFrame(long nativeContext) {
    mNativeContext = nativeContext;
  }

  @Override
  protected void finalize() {
    nativeFinalize();
  }

  @Override
  public void dispose() {
    nativeDispose();
  }

  @Override
  public void renderFrame(int width, int height, Bitmap bitmap) {
    nativeRenderFrame(width, height, bitmap);
  }

  @Override
  public int getDurationMs() {
    return nativeGetDurationMs();
  }

  @Override
  public int getWidth() {
    return nativeGetWidth();
  }

  @Override
  public int getHeight() {
    return nativeGetHeight();
  }

  @Override
  public int getXOffset() {
    return nativeGetXOffset();
  }

  @Override
  public int getYOffset() {
    return nativeGetYOffset();
  }

  public boolean shouldDisposeToBackgroundColor() {
    return nativeShouldDisposeToBackgroundColor();
  }

  public boolean isBlendWithPreviousFrame() {
    return nativeIsBlendWithPreviousFrame();
  }

  private native void nativeRenderFrame(int width, int height, Bitmap bitmap);
  s
  private native int nativeGetDurationMs();
  s
  private native int nativeGetWidth();
  s
  private native int nativeGetHeight();
  s
  private native int nativeGetXOffset();
  s
  private native int nativeGetYOffset();
  s
  private native boolean nativeShouldDisposeToBackgroundColor();
  s
  private native boolean nativeIsBlendWithPreviousFrame();
  s
  private native void nativeDispose();
  s
  private native void nativeFinalize();
  s
}
