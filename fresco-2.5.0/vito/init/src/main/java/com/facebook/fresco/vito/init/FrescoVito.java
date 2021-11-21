/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.init;

import android.content.res.Resources;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.core.impl.DefaultImageDecodeOptionsProviderImpl;
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl;
import com.facebook.fresco.vito.core.impl.source.ImageSourceProviderImpl;
import com.facebook.fresco.vito.nativecode.NativeCircularBitmapRounding;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.provider.impl.DefaultFrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoVito {
  private static boolean sIsInitialized;

  /**
   * Initialize Fresco Vito. Note: Fresco has to be initialized already, via Fresco.initialize(...)
   * 
   * @param resources resources for the application
   */
  public static synchronized void initialize(Resources resources)
  {
    initialize(resources, null);
  }

  /**
   * Initialize Fresco Vito. Note: Fresco has to be initialized already, via Fresco.initialize(...)
   * 
   * @param resources resources for the application
   * @param debugOverlayEnabledSupplier debug overlay toggle
   */
  public static synchronized void initialize(Resources resources, @Nullable com.facebook.common.internal.Supplier<Boolean> debugOverlayEnabledSupplier)
  {
    initialize(
        resources,
        ImagePipelineFactory.getInstance().getImagePipeline(),
        debugOverlayEnabledSupplier);
  }

  /**
   * Initialize Fresco Vito. Note: Fresco has to be initialized already, via Fresco.initialize(...)
   * 
   * @param resources resources for the application
   * @param imagePipeline the image pipeline used for image loading
   * @param debugOverlayEnabledSupplier debug overlay toggle
   */
  public static synchronized void initialize(final Resources resources, final com.facebook.imagepipeline.core.ImagePipeline imagePipeline, @Nullable final com.facebook.common.internal.Supplier<Boolean> debugOverlayEnabledSupplier)
  {
    if (sIsInitialized) {
      return;
    }
    initialize(
        createDefaultProviderImplementation(resources, imagePipeline, debugOverlayEnabledSupplier));
  }

  /**
   * Initialize Fresco Vito. Note: Fresco has to be initialized already, via Fresco.initialize(...)
   * 
   * @param providerImplementation the provider implementation to be used
   */
  public static synchronized void initialize(com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation providerImplementation)
  {
    if (sIsInitialized) {
      return;
    }
    FrescoVitoProvider.setImplementation(providerImplementation);
    ImageSourceProvider.setImplementation(new ImageSourceProviderImpl());
    sIsInitialized = true;
  }

  /**
   * Create a new default Fresco Vito provider implementation
   * 
   * @param resources resources for the application
   * @param imagePipeline the image pipeline used for image loading
   * @param debugOverlayEnabledSupplier debug overlay toggle
   * @return the provider to be used
   */
  public static com.facebook.fresco.vito.provider.FrescoVitoProvider.Implementation createDefaultProviderImplementation(final Resources resources, final com.facebook.imagepipeline.core.ImagePipeline imagePipeline, @Nullable final com.facebook.common.internal.Supplier<Boolean> debugOverlayEnabledSupplier)
  {
    final Supplier<Boolean> useNativeCode = Suppliers.BOOLEAN_TRUE;
    return new DefaultFrescoVitoProvider(
        resources,
        imagePipeline,
        imagePipeline.getConfig().getExecutorSupplier().forLightweightBackgroundTasks(),
        UiThreadImmediateExecutorService.getInstance(),
        createImagePipelineUtils(useNativeCode, Suppliers.BOOLEAN_FALSE),
        debugOverlayEnabledSupplier);
  }

  private static com.facebook.fresco.vito.core.ImagePipelineUtils createImagePipelineUtils(final com.facebook.common.internal.Supplier<Boolean> useNativeRounding, final com.facebook.common.internal.Supplier<Boolean> useFastNativeRounding)
  {
    ImagePipelineUtilsImpl.CircularBitmapRounding circularBitmapRounding =
        useNativeRounding.get() ? new NativeCircularBitmapRounding(useFastNativeRounding) : null;

    return new ImagePipelineUtilsImpl(
        new DefaultImageDecodeOptionsProviderImpl(circularBitmapRounding));
  }

}
