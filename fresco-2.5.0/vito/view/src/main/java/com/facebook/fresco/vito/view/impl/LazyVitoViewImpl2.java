/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.view.impl;

import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LazyVitoViewImpl2 extends LazyVitoViewImpl {
  public LazyVitoViewImpl2(com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation provider) {
    super(provider);
  }

  @Override
  protected com.facebook.fresco.vito.view.VitoView.Implementation create(com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation provider) {
    return new VitoViewImpl2(provider.getController(), provider.getImagePipeline());
  }

}
