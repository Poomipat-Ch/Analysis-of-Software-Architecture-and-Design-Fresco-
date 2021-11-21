/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.STRICT)
public class DecodeException extends RuntimeException {
  private final com.facebook.imagepipeline.image.EncodedImage mEncodedImage;

  public DecodeException(String message, com.facebook.imagepipeline.image.EncodedImage encodedImage) {
    super(message);
    mEncodedImage = encodedImage;
  }

  public DecodeException(String message, Throwable t, com.facebook.imagepipeline.image.EncodedImage encodedImage) {
    super(message, t);
    mEncodedImage = encodedImage;
  }

  public com.facebook.imagepipeline.image.EncodedImage getEncodedImage() {
    return mEncodedImage;
  }

}
