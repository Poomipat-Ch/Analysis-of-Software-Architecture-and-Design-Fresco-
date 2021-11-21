/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.samples.animation2.bitmap;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.bitmap.cache.FrescoFrameCache;
import com.facebook.fresco.animation.bitmap.cache.KeepLastFrameCache;
import com.facebook.fresco.animation.bitmap.cache.NoOpCache;
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache;
import com.facebook.samples.animation2.R;
/**
 * Manages a {@link Spinner} that can be used to switch the used caching implementation for {@link
 * BitmapAnimationBackend}s.
 */
public class BitmapAnimationCacheSelectorConfigurator {
  private final Spinner mSpinner;

  private abstract class CachingStrategyEntry {
    public abstract com.facebook.fresco.animation.bitmap.BitmapFrameCache createBitmapFrameCache() ;

    public abstract int getTitleResId() ;

    @Override
    public String toString() {
      return mContext.getString(getTitleResId());
    }

  }

  private final ArrayAdapter<BitmapAnimationCacheSelectorConfigurator.CachingStrategyEntry> mArrayAdapter;

  public interface BitmapFrameCacheChangedListener {
    void onBitmapFrameCacheChanged(com.facebook.fresco.animation.bitmap.BitmapFrameCache bitmapFrameCache) ;

  }

  private final BitmapAnimationCacheSelectorConfigurator.BitmapFrameCacheChangedListener mBitmapFrameCacheChangedListener;

  private final Context mContext;

  private final com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener mFrameCacheListener;

  public BitmapAnimationCacheSelectorConfigurator(Spinner spinner, BitmapAnimationCacheSelectorConfigurator.BitmapFrameCacheChangedListener bitmapFrameCacheChangedListener, com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener frameCacheListener) {
    mSpinner = spinner;
    mBitmapFrameCacheChangedListener = bitmapFrameCacheChangedListener;
    mFrameCacheListener = frameCacheListener;

    mContext = mSpinner.getContext();
    mArrayAdapter = new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_item);
    mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(mArrayAdapter);

    addSampleBackends();
    setupSelector();
  }

  private void addSampleBackends() {
    mArrayAdapter.add(createNoOpCachingStrategy());
    mArrayAdapter.add(createKeepLastCachingStrategy());
    mArrayAdapter.add(createNaiveCacheAllFramesCachingStrategy());
    mArrayAdapter.add(createFrescoFrameCache(false));
    mArrayAdapter.add(createFrescoFrameCache(true));
  }

  private BitmapAnimationCacheSelectorConfigurator.CachingStrategyEntry createNoOpCachingStrategy() {
    return new CachingStrategyEntry() {

      @Override
      public BitmapFrameCache createBitmapFrameCache() {
        return new NoOpCache();
      }

      @Override
      public int getTitleResId() {
        return R.string.cache_noop;
      }
    };
  }

  private BitmapAnimationCacheSelectorConfigurator.CachingStrategyEntry createKeepLastCachingStrategy() {
    return new CachingStrategyEntry() {

      @Override
      public BitmapFrameCache createBitmapFrameCache() {
        return new KeepLastFrameCache();
      }

      @Override
      public int getTitleResId() {
        return R.string.cache_keep_last;
      }
    };
  }

  private BitmapAnimationCacheSelectorConfigurator.CachingStrategyEntry createNaiveCacheAllFramesCachingStrategy() {
    return new CachingStrategyEntry() {

      @Override
      public BitmapFrameCache createBitmapFrameCache() {
        return new NaiveCacheAllFramesCachingBackend();
      }

      @Override
      public int getTitleResId() {
        return R.string.cache_naive_cache_all;
      }
    };
  }

  private BitmapAnimationCacheSelectorConfigurator.CachingStrategyEntry createFrescoFrameCache(final boolean reuseBitmaps) {
    return new CachingStrategyEntry() {
      @Override
      public BitmapFrameCache createBitmapFrameCache() {
        return new FrescoFrameCache(createAnimatedFrameCache(), reuseBitmaps);
      }

      @Override
      public int getTitleResId() {
        return reuseBitmaps ? R.string.cache_fresco_reuse : R.string.cache_fresco;
      }
    };
  }

  private com.facebook.imagepipeline.animated.impl.AnimatedFrameCache createAnimatedFrameCache() {
    return new AnimatedFrameCache(
        new SimpleCacheKey("Sample"),
        Fresco.getImagePipelineFactory().getBitmapCountingMemoryCache());
  }

  private void setupSelector() {
    mSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            BitmapFrameCache bitmapFrameCache =
                mArrayAdapter.getItem(position).createBitmapFrameCache();
            bitmapFrameCache.setFrameCacheListener(mFrameCacheListener);
            updateBitmapFrameCache(bitmapFrameCache);
          }

          @Override
          public void onNothingSelected(AdapterView<?> adapterView) {}
        });
  }

  private void updateBitmapFrameCache(com.facebook.fresco.animation.bitmap.BitmapFrameCache bitmapFrameCache) {
    if (mBitmapFrameCacheChangedListener != null) {
      mBitmapFrameCacheChangedListener.onBitmapFrameCacheChanged(bitmapFrameCache);
    }
  }

}
