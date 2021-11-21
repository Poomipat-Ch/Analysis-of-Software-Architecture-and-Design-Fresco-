/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 *  Class that does no stats tracking at all 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NoOpImageCacheStatsTracker implements ImageCacheStatsTracker {
  @Nullable
  private static NoOpImageCacheStatsTracker sInstance =  null;

  private NoOpImageCacheStatsTracker() {
  }

  public static synchronized NoOpImageCacheStatsTracker getInstance()
  {
    if (sInstance == null) {
      sInstance = new NoOpImageCacheStatsTracker();
    }
    return sInstance;
  }

  @Override
  public void onBitmapCachePut(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onBitmapCacheHit(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onBitmapCacheMiss(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onMemoryCachePut(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onMemoryCacheHit(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onMemoryCacheMiss(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onStagingAreaHit(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onStagingAreaMiss(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onDiskCacheHit(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onDiskCacheMiss(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onDiskCacheGetFail(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void onDiskCachePut(com.facebook.cache.common.CacheKey cacheKey) {
  }

  @Override
  public void registerBitmapMemoryCache(MemoryCache<?, ?> bitmapMemoryCache) {
  }

  @Override
  public void registerEncodedMemoryCache(MemoryCache<?, ?> encodedMemoryCache) {
  }

}
