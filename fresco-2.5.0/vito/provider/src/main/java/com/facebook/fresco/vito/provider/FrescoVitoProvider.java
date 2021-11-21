/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.provider;

import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public class FrescoVitoProvider {
  public interface Implementation {
    com.facebook.fresco.vito.core.FrescoController2 getController() ;

    com.facebook.fresco.vito.core.FrescoVitoPrefetcher getPrefetcher() ;

    com.facebook.fresco.vito.core.VitoImagePipeline getImagePipeline() ;

    com.facebook.fresco.vito.core.FrescoVitoConfig getConfig() ;

  }

  @Nullable
  private static FrescoVitoProvider.Implementation sImplementation;

  public static synchronized com.facebook.fresco.vito.core.FrescoController2 getController()
  {
    return getImplementation().getController();
  }

  public static synchronized com.facebook.fresco.vito.core.FrescoVitoPrefetcher getPrefetcher()
  {
    return getImplementation().getPrefetcher();
  }

  public static synchronized com.facebook.fresco.vito.core.VitoImagePipeline getImagePipeline()
  {
    return getImplementation().getImagePipeline();
  }

  public static synchronized com.facebook.fresco.vito.core.FrescoVitoConfig getConfig()
  {
    return getImplementation().getConfig();
  }

  public static synchronized void setImplementation(FrescoVitoProvider.Implementation implementation)
  {
    sImplementation = implementation;
  }

  public static synchronized FrescoVitoProvider.Implementation getImplementation()
  {
    if (sImplementation == null) {
      throw new RuntimeException("Fresco context provider must be set");
    }
    return sImplementation;
  }

}
