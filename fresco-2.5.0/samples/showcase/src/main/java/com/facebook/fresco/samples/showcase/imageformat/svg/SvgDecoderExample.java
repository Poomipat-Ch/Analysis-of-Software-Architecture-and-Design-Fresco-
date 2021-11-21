/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.samples.showcase.imageformat.svg;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import javax.annotation.Nullable;
/**
 *  SVG example that defines all classes required to decode and render SVG images. 
 */
public class SvgDecoderExample {
  public static final com.facebook.imageformat.ImageFormat SVG_FORMAT =  new ImageFormat("SVG_FORMAT", "svg");

  /**
   *  We do not include the closing ">" since there can be additional information
   */
  private static final String HEADER_TAG =  "<svg";

  private static final byte[][] POSSIBLE_HEADER_TAGS =  {
    ImageFormatCheckerUtils.asciiBytes("<?xml")
  };

  public static class SvgFormatChecker implements com.facebook.imageformat.ImageFormat.FormatChecker {
    public static final byte[] HEADER =  ImageFormatCheckerUtils.asciiBytes(HEADER_TAG);

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
        return SVG_FORMAT;
      }
      for (byte[] possibleHeaderTag : POSSIBLE_HEADER_TAGS) {
        if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, possibleHeaderTag)
            && ImageFormatCheckerUtils.indexOfPattern(
                    headerBytes, headerBytes.length, HEADER, HEADER.length)
                > -1) {
          return SVG_FORMAT;
        }
      }
      return null;
    }

  }

  public static class CloseableSvgImage extends com.facebook.imagepipeline.image.CloseableImage {
    private final SVG mSvg;

    private boolean mClosed =  false;

    public CloseableSvgImage(SVG svg) {
      mSvg = svg;
    }

    public SVG getSvg() {
      return mSvg;
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

  public static class SvgDecoder implements com.facebook.imagepipeline.decoder.ImageDecoder {
    @Override
    public com.facebook.imagepipeline.image.CloseableImage decode(com.facebook.imagepipeline.image.EncodedImage encodedImage, int length, com.facebook.imagepipeline.image.QualityInfo qualityInfo, com.facebook.imagepipeline.common.ImageDecodeOptions options) {
      try {
        SVG svg = SVG.getFromInputStream(encodedImage.getInputStream());
        return new CloseableSvgImage(svg);
      } catch (SVGParseException e) {
        e.printStackTrace();
      }
      return null;
    }

  }

  public static class SvgDrawableFactory implements com.facebook.imagepipeline.drawable.DrawableFactory {
    @Override
    public boolean supportsImageType(com.facebook.imagepipeline.image.CloseableImage image) {
      return image instanceof CloseableSvgImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(com.facebook.imagepipeline.image.CloseableImage image) {
      return new SvgPictureDrawable(((CloseableSvgImage) image).getSvg());
    }

  }

  public static class SvgPictureDrawable extends PictureDrawable {
    private final SVG mSvg;

    public SvgPictureDrawable(SVG svg) {
      super(null);
      mSvg = svg;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      setPicture(mSvg.renderToPicture(bounds.width(), bounds.height()));
    }

  }

}
