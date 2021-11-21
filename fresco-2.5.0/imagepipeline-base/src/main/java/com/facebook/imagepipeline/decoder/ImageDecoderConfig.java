/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.decoder;

import com.facebook.imageformat.ImageFormat;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
/**
 *  Configuration for {@link ImageDecoder}. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImageDecoderConfig {
  @Nullable
  private final Map<com.facebook.imageformat.ImageFormat, ImageDecoder> mCustomImageDecoders;

  @Nullable
  private final List<com.facebook.imageformat.ImageFormat.FormatChecker> mCustomImageFormats;

  private ImageDecoderConfig(ImageDecoderConfig.Builder builder) {
    mCustomImageDecoders = builder.mCustomImageDecoders;
    mCustomImageFormats = builder.mCustomImageFormats;
  }

  public static class Builder {
    @Nullable
    private Map<com.facebook.imageformat.ImageFormat, ImageDecoder> mCustomImageDecoders;

    @Nullable
    private List<com.facebook.imageformat.ImageFormat.FormatChecker> mCustomImageFormats;

    /**
     * Add a new decoding capability for a new image format.
     * 
     * @param imageFormat the new image format
     * @param imageFormatChecker the format checker that can determine the new image format
     * @param decoder the decoder that can decode the new image format
     * @return the builder
     */
    public ImageDecoderConfig.Builder addDecodingCapability(com.facebook.imageformat.ImageFormat imageFormat, com.facebook.imageformat.ImageFormat.FormatChecker imageFormatChecker, ImageDecoder decoder) {
      if (mCustomImageFormats == null) {
        mCustomImageFormats = new ArrayList<>();
      }
      mCustomImageFormats.add(imageFormatChecker);
      overrideDecoder(imageFormat, decoder);
      return this;
    }

    /**
     * Use a different decoder for an existing image format. This can be used for example to set a
     * custom decoder for any of the {@link com.facebook.imageformat.DefaultImageFormats}
     * 
     * @param imageFormat the existing image format
     * @param decoder the decoder to use
     * @return the builder
     */
    public ImageDecoderConfig.Builder overrideDecoder(com.facebook.imageformat.ImageFormat imageFormat, ImageDecoder decoder) {
      if (mCustomImageDecoders == null) {
        mCustomImageDecoders = new HashMap<>();
      }
      mCustomImageDecoders.put(imageFormat, decoder);
      return this;
    }

    public ImageDecoderConfig build() {
      return new ImageDecoderConfig(this);
    }

  }

  @Nullable
  public Map<com.facebook.imageformat.ImageFormat, ImageDecoder> getCustomImageDecoders() {
    return mCustomImageDecoders;
  }

  @Nullable
  public List<com.facebook.imageformat.ImageFormat.FormatChecker> getCustomImageFormats() {
    return mCustomImageFormats;
  }

  public static ImageDecoderConfig.Builder newBuilder()
  {
    return new Builder();
  }

}
