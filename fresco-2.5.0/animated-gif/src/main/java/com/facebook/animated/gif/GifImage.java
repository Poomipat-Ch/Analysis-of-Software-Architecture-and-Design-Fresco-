/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.animated.gif;

import android.graphics.Bitmap;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.BlendOperation;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.factory.AnimatedImageDecoder;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.soloader.nativeloader.NativeLoader;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
/**
 * A representation of a GIF image. An instance of this class will hold a copy of the encoded data
 * in memory along with the parsed header data. Frames are decoded on demand via {@link GifFrame}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@ThreadSafe
@DoNotStrip
public class GifImage implements com.facebook.imagepipeline.animated.base.AnimatedImage, com.facebook.imagepipeline.animated.factory.AnimatedImageDecoder {
  private static final int LOOP_COUNT_FOREVER =  0;

  private static final int LOOP_COUNT_MISSING =  -1;

  private static volatile boolean sInitialized;

  /**
   *  Accessed by native methods
   */
  @SuppressWarnings("unused")
  @DoNotStrip
  private long mNativeContext;

  @Nullable
  private Bitmap.Config mDecodeBitmapConfig =  null;

  private static synchronized void ensure()
  {
    if (!sInitialized) {
      sInitialized = true;
      NativeLoader.loadLibrary("gifimage");
    }
  }

  /**
   * Creates a {@link GifImage} from the specified encoded data. This will throw if it fails to
   * create. This is meant to be called on a worker thread.
   * 
   * @param source the data to the image (a copy will be made)
   */
  public static GifImage createFromByteArray(byte[] source)
  {
    Preconditions.checkNotNull(source);

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(source.length);
    byteBuffer.put(source);
    byteBuffer.rewind();

    return createFromByteBuffer(byteBuffer, ImageDecodeOptions.defaults());
  }

  /**
   * Creates a {@link GifImage} from a ByteBuffer containing the image. This will throw if it fails
   * to create.
   * 
   * @param byteBuffer the ByteBuffer containing the image (a copy will be made)
   */
  public static GifImage createFromByteBuffer(ByteBuffer byteBuffer)
  {
    return createFromByteBuffer(byteBuffer, ImageDecodeOptions.defaults());
  }

  /**
   * Creates a {@link GifImage} from a ByteBuffer containing the image. This will throw if it fails
   * to create.
   * 
   * @param byteBuffer the ByteBuffer containing the image (a copy will be made)
   */
  public static GifImage createFromByteBuffer(ByteBuffer byteBuffer, com.facebook.imagepipeline.common.ImageDecodeOptions options)
  {
    ensure();
    byteBuffer.rewind();

    GifImage image =
        nativeCreateFromDirectByteBuffer(
            byteBuffer, options.maxDimensionPx, options.forceStaticImage);
    image.mDecodeBitmapConfig = options.animatedBitmapConfig;
    return image;
  }

  public static GifImage createFromNativeMemory(long nativePtr, int sizeInBytes, com.facebook.imagepipeline.common.ImageDecodeOptions options)
  {
    ensure();
    Preconditions.checkArgument(nativePtr != 0);
    GifImage image =
        nativeCreateFromNativeMemory(
            nativePtr, sizeInBytes, options.maxDimensionPx, options.forceStaticImage);
    image.mDecodeBitmapConfig = options.animatedBitmapConfig;
    return image;
  }

  /**
   * Creates a {@link GifImage} from a file descriptor containing the image. This will throw if it
   * fails to create.
   * 
   * @param fileDescriptor the file descriptor containing the image (a copy will be made)
   */
  public static GifImage createFromFileDescriptor(int fileDescriptor, com.facebook.imagepipeline.common.ImageDecodeOptions options)
  {
    ensure();

    return nativeCreateFromFileDescriptor(
        fileDescriptor, options.maxDimensionPx, options.forceStaticImage);
  }

  @Override
  public com.facebook.imagepipeline.animated.base.AnimatedImage decodeFromNativeMemory(long nativePtr, int sizeInBytes, com.facebook.imagepipeline.common.ImageDecodeOptions options) {
    return GifImage.createFromNativeMemory(nativePtr, sizeInBytes, options);
  }

  @Override
  public com.facebook.imagepipeline.animated.base.AnimatedImage decodeFromByteBuffer(ByteBuffer byteBuffer, com.facebook.imagepipeline.common.ImageDecodeOptions options) {
    return GifImage.createFromByteBuffer(byteBuffer, options);
  }

  @DoNotStrip
  public GifImage() {
  }

  /**
   * Constructs the image with the native pointer. This is called by native code.
   * 
   * @param nativeContext the native pointer
   */
  @DoNotStrip
  GifImage(long nativeContext) {
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
  public int getWidth() {
    return nativeGetWidth();
  }

  @Override
  public int getHeight() {
    return nativeGetHeight();
  }

  @Override
  public int getFrameCount() {
    return nativeGetFrameCount();
  }

  @Override
  public int getDuration() {
    return nativeGetDuration();
  }

  @Override
  public int[] getFrameDurations() {
    return nativeGetFrameDurations();
  }

  @Override
  public int getLoopCount() {
    // If a GIF image has no Netscape 2.0 loop extension, it is meant to play once and then stop. A
    // loop count of 0 indicates an endless looping of the animation. Any loop count X>0 indicates
    // that the animation shall be repeated X times, resulting in the animation to play X+1 times.
    final int loopCount = nativeGetLoopCount();
    switch (loopCount) {
      case LOOP_COUNT_FOREVER:
        return AnimatedImage.LOOP_COUNT_INFINITE;

      case LOOP_COUNT_MISSING:
        return 1;

      default:
        return loopCount + 1;
    }
  }

  @Override
  public GifFrame getFrame(int frameNumber) {
    return nativeGetFrame(frameNumber);
  }

  @Override
  public boolean doesRenderSupportScaling() {
    return false;
  }

  @Override
  public int getSizeInBytes() {
    return nativeGetSizeInBytes();
  }

  public boolean isAnimated() {
    return nativeIsAnimated();
  }

  @Override
  public com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    GifFrame frame = getFrame(frameNumber);
    try {
      return new AnimatedDrawableFrameInfo(
          frameNumber,
          frame.getXOffset(),
          frame.getYOffset(),
          frame.getWidth(),
          frame.getHeight(),
          BlendOperation.BLEND_WITH_PREVIOUS,
          fromGifDisposalMethod(frame.getDisposalMode()));
    } finally {
      frame.dispose();
    }
  }

  @Override
  @Nullable
  public Bitmap.Config getAnimatedBitmapConfig() {
    return mDecodeBitmapConfig;
  }

  private static com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.DisposalMethod fromGifDisposalMethod(int disposalMode)
  {
    if (disposalMode == 0 /* DISPOSAL_UNSPECIFIED */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    } else if (disposalMode == 1 /* DISPOSE_DO_NOT */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    } else if (disposalMode == 2 /* DISPOSE_BACKGROUND */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_TO_BACKGROUND;
    } else if (disposalMode == 3 /* DISPOSE_PREVIOUS */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_TO_PREVIOUS;
    } else {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    }
  }

  @DoNotStrip
  private static native GifImage nativeCreateFromDirectByteBuffer(ByteBuffer buffer, int maxDimension, boolean forceStatic);
  s
  @DoNotStrip
  private static native GifImage nativeCreateFromNativeMemory(long nativePtr, int sizeInBytes, int maxDimension, boolean forceStatic);
  s
  @DoNotStrip
  private static native GifImage nativeCreateFromFileDescriptor(int fileDescriptor, int maxDimension, boolean forceStatic);
  s
  @DoNotStrip
  private native int nativeGetWidth();
  s
  @DoNotStrip
  private native int nativeGetHeight();
  s
  @DoNotStrip
  private native int nativeGetDuration();
  s
  @DoNotStrip
  private native int nativeGetFrameCount();
  s
  @DoNotStrip
  private native int[] nativeGetFrameDurations();
  s
  @DoNotStrip
  private native int nativeGetLoopCount();
  s
  @DoNotStrip
  private native GifFrame nativeGetFrame(int frameNumber);
  s
  @DoNotStrip
  private native int nativeGetSizeInBytes();
  s
  @DoNotStrip
  private native boolean nativeIsAnimated();
  s
  @DoNotStrip
  private native void nativeDispose();
  s
  @DoNotStrip
  private native void nativeFinalize();
  s
}
