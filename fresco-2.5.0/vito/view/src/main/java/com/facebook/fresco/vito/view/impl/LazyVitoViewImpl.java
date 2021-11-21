/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.view.impl;

import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.STRICT)
public abstract class LazyVitoViewImpl implements com.facebook.fresco.vito.view.VitoView.Implementation {
  private final com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation mProvider;

  @Nullable
  private com.facebook.fresco.vito.view.VitoView.Implementation mImplementation;

  public LazyVitoViewImpl(com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation provider) {
    mProvider = provider;
  }

  @Override
  public void show(com.facebook.fresco.vito.source.ImageSource imageSource, com.facebook.fresco.vito.options.ImageOptions imageOptions, @Nullable Object callerContext, @Nullable com.facebook.fresco.vito.listener.ImageListener imageListener, View target) {
    get().show(imageSource, imageOptions, callerContext, imageListener, target);
  }

  private synchronized com.facebook.fresco.vito.view.VitoView.Implementation get() {
    if (mImplementation == null) {
      mImplementation = create(mProvider);
    }
    return mImplementation;
  }

  protected abstract com.facebook.fresco.vito.view.VitoView.Implementation create(com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation provider) ;

}
