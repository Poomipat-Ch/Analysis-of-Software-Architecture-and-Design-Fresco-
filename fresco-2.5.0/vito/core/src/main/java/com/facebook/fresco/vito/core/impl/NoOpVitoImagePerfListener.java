/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl;

import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpVitoImagePerfListener implements com.facebook.fresco.vito.core.VitoImagePerfListener {
  @Override
  public void onImageMount(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onImageUnmount(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onImageBind(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onImageUnbind(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onImageFetch(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onImageSuccess(com.facebook.fresco.vito.core.FrescoDrawable2 drawable, boolean wasImmediate) {
  }

  @Override
  public void onImageError(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onImageRelease(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onScheduleReleaseDelayed(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onScheduleReleaseNextFrame(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onReleaseImmediately(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onDrawableReconfigured(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onIgnoreResult(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

  @Override
  public void onIgnoreFailure(com.facebook.fresco.vito.core.FrescoDrawable2 drawable) {
  }

}
