/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.samples.showcase.imageformat.color;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import com.facebook.common.internal.ByteStreams;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import java.io.IOException;
import javax.annotation.Nullable;
/**
 * Example for a simple decoder that can decode color images that have the following format:
 * 
 * <p><color>#FF5722</color>
 */
public class ColorImageExample {
  /**
   *  XML color tag that our colors must start with. 
   */
  public static final String COLOR_TAG =  "<color>";

  /**
   *  Custom {@link ImageFormat} for color images. 
   */
  public static final com.facebook.imageformat.ImageFormat IMAGE_FORMAT_COLOR = 
      new ImageFormat("IMAGE_FORMAT_COLOR", "color");

  /**
   * Create a new image format checker for {@link #IMAGE_FORMAT_COLOR}.
   * 
   * @return the image format checker
   */
  public static com.facebook.imageformat.ImageFormat.FormatChecker createFormatChecker()
  {
    return new ColorFormatChecker();
  }

  /**
   * Create a new decoder that can decode {@link #IMAGE_FORMAT_COLOR} images.
   * 
   * @return the decoder
   */
  public static com.facebook.imagepipeline.decoder.ImageDecoder createDecoder()
  {
    return new ColorDecoder();
  }

  public static class ColorDrawableFactory implements com.facebook.imagepipeline.drawable.DrawableFactory {
    @Override
    public boolean supportsImageType(com.facebook.imagepipeline.image.CloseableImage image) {
      // We can only handle CloseableColorImages
      return image instanceof CloseableColorImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(com.facebook.imagepipeline.image.CloseableImage image) {
      // Just return a simple ColorDrawable with the given color value
      return new ColorDrawable(((CloseableColorImage) image).getColor());
    }

  }

  public static ColorImageExample.ColorDrawableFactory createDrawableFactory()
  {
    return new ColorDrawableFactory();
  }

  public static class ColorFormatChecker implements com.facebook.imageformat.ImageFormat.FormatChecker {
    public static final byte[] HEADER =  ImageFormatCheckerUtils.asciiBytes(COLOR_TAG);

    @Override
    public int getHeaderSize() {
      return HEADER.length;
    }

    @Nullable
    @Override
    public com.facebook.imageformat.ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
      if (headerSize < getHeaderSize()) {
        return null;
      }
      if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, HEADER)) {
        return IMAGE_FORMAT_COLOR;
      }
      return null;
    }

  }

  public static class CloseableColorImage extends com.facebook.imagepipeline.image.CloseableImage {
    @ColorInt
    private final int mColor;

    private boolean mClosed =  false;

    public CloseableColorImage(int color) {
      mColor = color;
    }

    @ColorInt
    public int getColor() {
      return mColor;
    }

    @Override
    public int getSizeInBytes() {
      return 0;
    }

    @Override
    public void close() {
      mClosed = true;
    }

    @Override
    public boolean isClosed() {
      return mClosed;
    }

    @Override
    public int getWidth() {
      return 0;
    }

    @Override
    public int getHeight() {
      return 0;
    }

  }

  public static class ColorDecoder implements com.facebook.imagepipeline.decoder.ImageDecoder {
    @Override
    public com.facebook.imagepipeline.image.CloseableImage decode(com.facebook.imagepipeline.image.EncodedImage encodedImage, int length, com.facebook.imagepipeline.image.QualityInfo qualityInfo, com.facebook.imagepipeline.common.ImageDecodeOptions options) {
      try {
        // Read the file as a string
        String text = new String(ByteStreams.toByteArray(encodedImage.getInputStream()));

        // Check if the string matches "<color>#"
        if (!text.startsWith(COLOR_TAG + "#")) {
          return null;
        }

        // Parse the int value between # and <
        int startIndex = COLOR_TAG.length() + 1;
        int endIndex = text.lastIndexOf('<');
        int color = Integer.parseInt(text.substring(startIndex, endIndex), 16);

        // Add the alpha component so that we actually see the color
        color = ColorUtils.setAlphaComponent(color, 255);

        // Return the CloseableImage
        return new CloseableColorImage(color);
      } catch (IOException e) {
        e.printStackTrace();
      }
      // Return nothing if an error occurred
      return null;
    }

  }

}
