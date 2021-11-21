/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.cache.disk;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.disk.DiskTrimmable;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import javax.annotation.Nullable;
/**
 *  Interface that caches based on disk should implement. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface FileCache extends com.facebook.common.disk.DiskTrimmable {
  /**
   * Tells if this cache is enabled. It's important for some caches that can be disabled without
   * further notice (like in removable/unmountable storage). Anyway a disabled cache should just
   * ignore calls, not fail.
   * 
   * @return true if this cache is usable, false otherwise.
   */
  boolean isEnabled() ;

  /**
   *  Returns the binary resource cached with key. 
   */
  @Nullable
  com.facebook.binaryresource.BinaryResource getResource(com.facebook.cache.common.CacheKey key) ;

  /**
   * Returns true if the key is in the in-memory key index.
   * 
   * <p>Not guaranteed to be correct. The cache may yet have this key even if this returns false.
   * But if it returns true, it definitely has it.
   * 
   * <p>Avoids a disk read.
   */
  boolean hasKeySync(com.facebook.cache.common.CacheKey key) ;

  boolean hasKey(com.facebook.cache.common.CacheKey key) ;

  boolean probe(com.facebook.cache.common.CacheKey key) ;

  /**
   * Inserts resource into file with key
   * 
   * @param key cache key
   * @param writer Callback that writes to an output stream
   * @return a sequence of bytes
   * @throws IOException
   */
  @Nullable
  com.facebook.binaryresource.BinaryResource insert(com.facebook.cache.common.CacheKey key, com.facebook.cache.common.WriterCallback writer) throws IOException ;

  /**
   * Removes a resource by key from cache.
   * 
   * @param key cache key
   */
  void remove(com.facebook.cache.common.CacheKey key) ;

  /**
   *  @return the in-use size of the cache 
   */
  long getSize() ;

  /**
   *  @return the count of pictures in the cache 
   */
  long getCount() ;

  /**
   * Deletes old cache files.
   * 
   * @param cacheExpirationMs files older than this will be deleted.
   * @return the age in ms of the oldest file remaining in the cache.
   */
  long clearOldEntries(long cacheExpirationMs) ;

  void clearAll() ;

  DiskStorage.DiskDumpInfo getDumpInfo() throws IOException ;

}
