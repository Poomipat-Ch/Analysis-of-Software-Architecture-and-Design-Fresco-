/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.image;

import android.net.Uri;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class OriginalEncodedImageInfo {
  public static final OriginalEncodedImageInfo EMPTY =  new OriginalEncodedImageInfo();

  @Nullable
  private final Uri mUri;

  @Nullable
  private final EncodedImageOrigin mOrigin;

  @Nullable
  private final Object mCallerContext;

  private final int mWidth;

  private final int mHeight;

  private final int mSize;

  private OriginalEncodedImageInfo() {
    mUri = null;
    mOrigin = EncodedImageOrigin.NOT_SET;
    mCallerContext = null;
    mWidth = -1;
    mHeight = -1;
    mSize = -1;
  }

  public OriginalEncodedImageInfo(Uri sourceUri, EncodedImageOrigin origin, @Nullable Object callerContext, int width, int height, int size) {
    mUri = sourceUri;
    mOrigin = origin;
    mCallerContext = callerContext;
    mWidth = width;
    mHeight = height;
    mSize = size;
  }

  public int getWidth() {
    return mWidth;
  }

  public int getHeight() {
    return mHeight;
  }

  public int getSize() {
    return mSize;
  }

  @Nullable
  public Uri getUri() {
    return mUri;
  }

  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Nullable
  public EncodedImageOrigin getOrigin() {
    return mOrigin;
  }

}
