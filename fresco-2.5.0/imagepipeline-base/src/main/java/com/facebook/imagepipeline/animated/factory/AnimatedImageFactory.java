/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.factory;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.infer.annotation.Nullsafe;
/**
 *  Decoder for animated images. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AnimatedImageFactory {
  /**
   * Decodes a GIF into a CloseableImage.
   * 
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a {@link CloseableImage} for the GIF image
   */
  com.facebook.imagepipeline.image.CloseableImage decodeGif(final com.facebook.imagepipeline.image.EncodedImage encodedImage, final com.facebook.imagepipeline.common.ImageDecodeOptions options, final Bitmap.Config bitmapConfig) ;

  /**
   * Decode a WebP into a CloseableImage.
   * 
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a {@link CloseableImage} for the WebP image
   */
  com.facebook.imagepipeline.image.CloseableImage decodeWebP(final com.facebook.imagepipeline.image.EncodedImage encodedImage, final com.facebook.imagepipeline.common.ImageDecodeOptions options, final Bitmap.Config bitmapConfig) ;

}
