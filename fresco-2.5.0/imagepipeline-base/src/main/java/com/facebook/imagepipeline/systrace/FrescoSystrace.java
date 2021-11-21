/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.systrace;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 * This is intended as a hook into {@code android.os.Trace}, but allows you to provide your own
 * functionality. Use it as
 * 
 * <p>{@code FrescoSystrace.beginSection("tag"); ... FrescoSystrace.endSection(); } As a default, it
 * simply calls {@code android.os.Trace} (see {@link DefaultFrescoSystrace}). You may supply your
 * own with {@link FrescoSystrace#provide(Systrace)}.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class FrescoSystrace {
  public interface Systrace {
    void beginSection(String name) ;

    FrescoSystrace.ArgsBuilder beginSectionWithArgs(String name) ;

    void endSection() ;

    boolean isTracing() ;

  }

  public interface ArgsBuilder {
    /**
     * Write the full message to the Systrace buffer.
     * 
     * <p>You must call this to log the trace message.
     */
    void flush() ;

    /**
     * Logs an argument whose value is any object. It will be stringified with {@link
     * String#valueOf(Object)}.
     */
    FrescoSystrace.ArgsBuilder arg(String key, Object value) ;

    /**
     * Logs an argument whose value is an int. It will be stringified with {@link
     * String#valueOf(int)}.
     */
    FrescoSystrace.ArgsBuilder arg(String key, int value) ;

    /**
     * Logs an argument whose value is a long. It will be stringified with {@link
     * String#valueOf(long)}.
     */
    FrescoSystrace.ArgsBuilder arg(String key, long value) ;

    /**
     * Logs an argument whose value is a double. It will be stringified with {@link
     * String#valueOf(double)}.
     */
    FrescoSystrace.ArgsBuilder arg(String key, double value) ;

  }

  /**
   *  Convenience implementation of ArgsBuilder to use when we aren't tracing. 
   */
  public static final FrescoSystrace.ArgsBuilder NO_OP_ARGS_BUILDER =  new NoOpArgsBuilder();

  @Nullable
  private static volatile FrescoSystrace.Systrace sInstance =  null;

  private FrescoSystrace() {
  }

  public static void provide(FrescoSystrace.Systrace instance)
  {
    sInstance = instance;
  }

  public static void beginSection(String name)
  {
    getInstance().beginSection(name);
  }

  public static FrescoSystrace.ArgsBuilder beginSectionWithArgs(String name)
  {
    return getInstance().beginSectionWithArgs(name);
  }

  public static void endSection()
  {
    getInstance().endSection();
  }

  public static boolean isTracing()
  {
    return getInstance().isTracing();
  }

  private static FrescoSystrace.Systrace getInstance()
  {
    if (sInstance == null) {
      synchronized (FrescoSystrace.class) {
        if (sInstance == null) {
          sInstance = new DefaultFrescoSystrace();
        }
      }
    }
    return sInstance;
  }

  private static final class NoOpArgsBuilder implements FrescoSystrace.ArgsBuilder {
    @Override
    public void flush() {
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, Object value) {
      return this;
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, int value) {
      return this;
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, long value) {
      return this;
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, double value) {
      return this;
    }

  }

}
