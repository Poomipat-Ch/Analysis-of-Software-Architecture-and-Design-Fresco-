/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.cache.disk;

import android.content.Context;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.NoOpCacheErrorLogger;
import com.facebook.cache.common.NoOpCacheEventListener;
import com.facebook.common.disk.DiskTrimmable;
import com.facebook.common.disk.DiskTrimmableRegistry;
import com.facebook.common.disk.NoOpDiskTrimmableRegistry;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.util.ByteConstants;
import com.facebook.infer.annotation.Nullsafe;
import java.io.File;
import javax.annotation.Nullable;
/**
 *  Configuration class for a {@link DiskStorageCache}. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class DiskCacheConfig {
  private final int mVersion;

  private final String mBaseDirectoryName;

  private final com.facebook.common.internal.Supplier<File> mBaseDirectoryPathSupplier;

  private final long mDefaultSizeLimit;

  private final long mLowDiskSpaceSizeLimit;

  private final long mMinimumSizeLimit;

  private final EntryEvictionComparatorSupplier mEntryEvictionComparatorSupplier;

  private final com.facebook.cache.common.CacheErrorLogger mCacheErrorLogger;

  private final com.facebook.cache.common.CacheEventListener mCacheEventListener;

  private final com.facebook.common.disk.DiskTrimmableRegistry mDiskTrimmableRegistry;

  @Nullable
  private final Context mContext;

  private final boolean mIndexPopulateAtStartupEnabled;

  protected DiskCacheConfig(DiskCacheConfig.Builder builder) {
    mContext = builder.mContext;
    Preconditions.checkState(
        builder.mBaseDirectoryPathSupplier != null || mContext != null,
        "Either a non-null context or a base directory path or supplier must be provided.");
    if (builder.mBaseDirectoryPathSupplier == null && mContext != null) {
      builder.mBaseDirectoryPathSupplier =
          new Supplier<File>() {
            @Override
            public File get() {
              Preconditions.checkNotNull(mContext);
              return mContext.getApplicationContext().getCacheDir();
            }
          };
    }
    mVersion = builder.mVersion;
    mBaseDirectoryName = Preconditions.checkNotNull(builder.mBaseDirectoryName);
    mBaseDirectoryPathSupplier = Preconditions.checkNotNull(builder.mBaseDirectoryPathSupplier);
    mDefaultSizeLimit = builder.mMaxCacheSize;
    mLowDiskSpaceSizeLimit = builder.mMaxCacheSizeOnLowDiskSpace;
    mMinimumSizeLimit = builder.mMaxCacheSizeOnVeryLowDiskSpace;
    mEntryEvictionComparatorSupplier =
        Preconditions.checkNotNull(builder.mEntryEvictionComparatorSupplier);
    mCacheErrorLogger =
        builder.mCacheErrorLogger == null
            ? NoOpCacheErrorLogger.getInstance()
            : builder.mCacheErrorLogger;
    mCacheEventListener =
        builder.mCacheEventListener == null
            ? NoOpCacheEventListener.getInstance()
            : builder.mCacheEventListener;
    mDiskTrimmableRegistry =
        builder.mDiskTrimmableRegistry == null
            ? NoOpDiskTrimmableRegistry.getInstance()
            : builder.mDiskTrimmableRegistry;
    mIndexPopulateAtStartupEnabled = builder.mIndexPopulateAtStartupEnabled;
  }

  public static class Builder {
    private int mVersion =  1;

    private String mBaseDirectoryName =  "image_cache";

    @Nullable
    private com.facebook.common.internal.Supplier<File> mBaseDirectoryPathSupplier;

    private long mMaxCacheSize =  40 * ByteConstants.MB;

    private long mMaxCacheSizeOnLowDiskSpace =  10 * ByteConstants.MB;

    private long mMaxCacheSizeOnVeryLowDiskSpace =  2 * ByteConstants.MB;

    private EntryEvictionComparatorSupplier mEntryEvictionComparatorSupplier = 
        new DefaultEntryEvictionComparatorSupplier();

    @Nullable
    private com.facebook.cache.common.CacheErrorLogger mCacheErrorLogger;

    @Nullable
    private com.facebook.cache.common.CacheEventListener mCacheEventListener;

    @Nullable
    private com.facebook.common.disk.DiskTrimmableRegistry mDiskTrimmableRegistry;

    private boolean mIndexPopulateAtStartupEnabled;

    @Nullable
    private final Context mContext;

    private Builder(@Nullable Context context) {
      mContext = context;
    }

    /**
     * Sets the version.
     * 
     * <p>The cache lives in a subdirectory identified by this version.
     */
    public DiskCacheConfig.Builder setVersion(int version) {
      mVersion = version;
      return this;
    }

    /**
     *  Sets the name of the directory where the cache will be located. 
     */
    public DiskCacheConfig.Builder setBaseDirectoryName(String baseDirectoryName) {
      mBaseDirectoryName = baseDirectoryName;
      return this;
    }

    /**
     * Sets the path to the base directory.
     * 
     * <p>A directory with the given base directory name (see {@code setBaseDirectoryName}) will be
     * appended to this path.
     */
    public DiskCacheConfig.Builder setBaseDirectoryPath(final File baseDirectoryPath) {
      mBaseDirectoryPathSupplier = Suppliers.of(baseDirectoryPath);
      return this;
    }

    public DiskCacheConfig.Builder setBaseDirectoryPathSupplier(com.facebook.common.internal.Supplier<File> baseDirectoryPathSupplier) {
      mBaseDirectoryPathSupplier = baseDirectoryPathSupplier;
      return this;
    }

    /**
     *  This is the default maximum size of the cache. 
     */
    public DiskCacheConfig.Builder setMaxCacheSize(long maxCacheSize) {
      mMaxCacheSize = maxCacheSize;
      return this;
    }

    /**
     * This is the maximum size of the cache that is used when the device is low on disk space.
     * 
     * <p>See {@link DiskTrimmable#trimToMinimum()}.
     */
    public DiskCacheConfig.Builder setMaxCacheSizeOnLowDiskSpace(long maxCacheSizeOnLowDiskSpace) {
      mMaxCacheSizeOnLowDiskSpace = maxCacheSizeOnLowDiskSpace;
      return this;
    }

    /**
     * This is the maximum size of the cache when the device is extremely low on disk space.
     * 
     * <p>See {@link DiskTrimmable#trimToNothing()}.
     */
    public DiskCacheConfig.Builder setMaxCacheSizeOnVeryLowDiskSpace(long maxCacheSizeOnVeryLowDiskSpace) {
      mMaxCacheSizeOnVeryLowDiskSpace = maxCacheSizeOnVeryLowDiskSpace;
      return this;
    }

    /**
     *  Provides the logic to determine the eviction order based on entry's access time and size 
     */
    public DiskCacheConfig.Builder setEntryEvictionComparatorSupplier(EntryEvictionComparatorSupplier supplier) {
      mEntryEvictionComparatorSupplier = supplier;
      return this;
    }

    /**
     *  The logger that is used to log errors made by the cache. 
     */
    public DiskCacheConfig.Builder setCacheErrorLogger(com.facebook.cache.common.CacheErrorLogger cacheErrorLogger) {
      mCacheErrorLogger = cacheErrorLogger;
      return this;
    }

    /**
     *  The listener for cache events. 
     */
    public DiskCacheConfig.Builder setCacheEventListener(com.facebook.cache.common.CacheEventListener cacheEventListener) {
      mCacheEventListener = cacheEventListener;
      return this;
    }

    /**
     * The class that will contain a registry of caches to be trimmed in low disk space conditions.
     * 
     * <p>See {@link DiskTrimmableRegistry}.
     */
    public DiskCacheConfig.Builder setDiskTrimmableRegistry(com.facebook.common.disk.DiskTrimmableRegistry diskTrimmableRegistry) {
      mDiskTrimmableRegistry = diskTrimmableRegistry;
      return this;
    }

    public DiskCacheConfig.Builder setIndexPopulateAtStartupEnabled(boolean indexEnabled) {
      mIndexPopulateAtStartupEnabled = indexEnabled;
      return this;
    }

    public DiskCacheConfig build() {
      return new DiskCacheConfig(this);
    }

  }

  public int getVersion() {
    return mVersion;
  }

  public String getBaseDirectoryName() {
    return mBaseDirectoryName;
  }

  public com.facebook.common.internal.Supplier<File> getBaseDirectoryPathSupplier() {
    return mBaseDirectoryPathSupplier;
  }

  public long getDefaultSizeLimit() {
    return mDefaultSizeLimit;
  }

  public long getLowDiskSpaceSizeLimit() {
    return mLowDiskSpaceSizeLimit;
  }

  public long getMinimumSizeLimit() {
    return mMinimumSizeLimit;
  }

  public EntryEvictionComparatorSupplier getEntryEvictionComparatorSupplier() {
    return mEntryEvictionComparatorSupplier;
  }

  public com.facebook.cache.common.CacheErrorLogger getCacheErrorLogger() {
    return mCacheErrorLogger;
  }

  public com.facebook.cache.common.CacheEventListener getCacheEventListener() {
    return mCacheEventListener;
  }

  public com.facebook.common.disk.DiskTrimmableRegistry getDiskTrimmableRegistry() {
    return mDiskTrimmableRegistry;
  }

  @Nullable
  public Context getContext() {
    return mContext;
  }

  public boolean getIndexPopulateAtStartupEnabled() {
    return mIndexPopulateAtStartupEnabled;
  }

  /**
   * Create a new builder.
   * 
   * @param context If this is null, you must explicitly call {@link
   *     Builder#setBaseDirectoryPath(File)} or {@link
   *     Builder#setBaseDirectoryPathSupplier(Supplier)} or the config won't know where to
   *     physically locate the cache.
   * @return
   */
  public static DiskCacheConfig.Builder newBuilder(@Nullable Context context)
  {
    return new Builder(context);
  }

}
