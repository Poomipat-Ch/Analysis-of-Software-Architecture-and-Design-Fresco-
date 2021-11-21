/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.provider.impl;

import android.content.res.Resources;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.impl.FrescoController2Impl;
import com.facebook.fresco.vito.core.impl.FrescoVitoPrefetcherImpl;
import com.facebook.fresco.vito.core.impl.HierarcherImpl;
import com.facebook.fresco.vito.core.impl.NoOpVitoImagePerfListener;
import com.facebook.fresco.vito.core.impl.VitoImagePipelineImpl;
import com.facebook.fresco.vito.core.impl.debug.DefaultDebugOverlayFactory2;
import com.facebook.fresco.vito.core.impl.debug.NoOpDebugOverlayFactory2;
import com.facebook.fresco.vito.drawable.ArrayVitoDrawableFactory;
import com.facebook.fresco.vito.drawable.BitmapDrawableFactory;
import com.facebook.fresco.vito.draweesupport.DrawableFactoryWrapper;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultFrescoVitoProvider implements com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation {
  private final com.facebook.fresco.vito.core.FrescoController2 mFrescoController;

  private final com.facebook.fresco.vito.core.VitoImagePipeline mVitoImagePipeline;

  private final com.facebook.fresco.vito.core.FrescoVitoPrefetcher mFrescoVitoPrefetcher;

  private final com.facebook.fresco.vito.core.FrescoVitoConfig mFrescoVitoConfig;

  public DefaultFrescoVitoProvider(final Resources resources, final com.facebook.imagepipeline.core.ImagePipeline imagePipeline, final Executor lightweightBackgroundThreadExecutor, final Executor uiThreadExecutor, final com.facebook.fresco.vito.core.ImagePipelineUtils imagePipelineUtils, @Nullable final com.facebook.common.internal.Supplier<Boolean> debugOverlayEnabledSupplier) {
    this(
        resources,
        new DefaultFrescoVitoConfig(),
        imagePipeline,
        imagePipelineUtils,
        lightweightBackgroundThreadExecutor,
        uiThreadExecutor,
        debugOverlayEnabledSupplier,
        new NoOpCallerContextVerifier());
  }

  public DefaultFrescoVitoProvider(Resources resources, com.facebook.fresco.vito.core.FrescoVitoConfig config, com.facebook.imagepipeline.core.ImagePipeline imagePipeline, com.facebook.fresco.vito.core.ImagePipelineUtils imagePipelineUtils, Executor lightweightBackgroundThreadExecutor, Executor uiThreadExecutor, @Nullable com.facebook.common.internal.Supplier<Boolean> debugOverlayEnabledSupplier, com.facebook.callercontext.CallerContextVerifier callerContextVerifier) {
    if (!ImagePipelineFactory.hasBeenInitialized()) {
      throw new RuntimeException(
          "Fresco must be initialized before DefaultFrescoVitoProvider can be used!");
    }
    mFrescoVitoConfig = config;
    mFrescoVitoPrefetcher =
        new FrescoVitoPrefetcherImpl(imagePipeline, imagePipelineUtils, callerContextVerifier);
    mVitoImagePipeline = new VitoImagePipelineImpl(imagePipeline, imagePipelineUtils);
    mFrescoController =
        new FrescoController2Impl(
            mFrescoVitoConfig,
            new HierarcherImpl(createDefaultDrawableFactory(resources)),
            lightweightBackgroundThreadExecutor,
            uiThreadExecutor,
            mVitoImagePipeline,
            null,
            debugOverlayEnabledSupplier == null
                ? new NoOpDebugOverlayFactory2()
                : new DefaultDebugOverlayFactory2(debugOverlayEnabledSupplier),
            null,
            new NoOpVitoImagePerfListener());
  }

  @Override
  public com.facebook.fresco.vito.core.FrescoController2 getController() {
    return mFrescoController;
  }

  @Override
  public com.facebook.fresco.vito.core.FrescoVitoPrefetcher getPrefetcher() {
    return mFrescoVitoPrefetcher;
  }

  @Override
  public com.facebook.fresco.vito.core.VitoImagePipeline getImagePipeline() {
    return mVitoImagePipeline;
  }

  @Override
  public com.facebook.fresco.vito.core.FrescoVitoConfig getConfig() {
    return mFrescoVitoConfig;
  }

  private static com.facebook.fresco.vito.options.ImageOptionsDrawableFactory createDefaultDrawableFactory(Resources resources)
  {
    DrawableFactory animatedDrawableFactory =
        ImagePipelineFactory.getInstance().getAnimatedDrawableFactory(null);
    return new ArrayVitoDrawableFactory(
        new BitmapDrawableFactory(resources),
        animatedDrawableFactory == null
            ? null
            : new DrawableFactoryWrapper(animatedDrawableFactory));
  }

}
