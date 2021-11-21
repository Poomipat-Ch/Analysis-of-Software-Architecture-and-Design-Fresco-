/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.debug;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.CacheKeyUtil;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfData;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
/**
 *  Fresco image tracker for Sonar 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FlipperImageTracker implements DebugImageTracker, com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener {
  private static final int MAX_IMAGES_TO_TRACK =  1000;

  private final Map<com.facebook.imagepipeline.request.ImageRequest, ImageDebugData> mImageRequestDebugDataMap;

  private final Map<com.facebook.cache.common.CacheKey, ImageDebugData> mImageDebugDataMap;

  public FlipperImageTracker() {
    mImageRequestDebugDataMap = new LruMap<>(MAX_IMAGES_TO_TRACK);
    mImageDebugDataMap = new LruMap<>(MAX_IMAGES_TO_TRACK);
  }

  @Override
  public synchronized void trackImage(com.facebook.imagepipeline.request.ImageRequest imageRequest, com.facebook.cache.common.CacheKey cacheKey) {
    ImageDebugData imageDebugData = mImageRequestDebugDataMap.get(imageRequest);
    if (imageDebugData == null) {
      imageDebugData = new ImageDebugData(imageRequest);
      mImageDebugDataMap.put(cacheKey, imageDebugData);
      mImageRequestDebugDataMap.put(imageRequest, imageDebugData);
    }
    imageDebugData.addCacheKey(cacheKey);
    imageDebugData.addResourceId(CacheKeyUtil.getFirstResourceId(cacheKey));
  }

  @Override
  public synchronized void trackImageRequest(com.facebook.imagepipeline.request.ImageRequest imageRequest, String requestId) {
    ImageDebugData imageDebugData = mImageRequestDebugDataMap.get(imageRequest);
    if (imageDebugData == null) {
      imageDebugData = new ImageDebugData(imageRequest);
      mImageRequestDebugDataMap.put(imageRequest, imageDebugData);
    }
    imageDebugData.addRequestId(requestId);
  }

  public static class ImageDebugData {
    @Nullable
    private final com.facebook.imagepipeline.request.ImageRequest mImageRequest;

    @Nullable
    private com.facebook.drawee.backends.pipeline.info.ImagePerfData mImagePerfData;

    @Nullable
    private Set<com.facebook.cache.common.CacheKey> mCacheKeys;

    @Nullable
    private Set<String> mRequestIds;

    @Nullable
    private Set<String> mResourceIds;

    @Nullable
    private String mLocalPath;

    public ImageDebugData() {
      this(null, null);
    }

    public ImageDebugData(@Nullable com.facebook.imagepipeline.request.ImageRequest imageRequest) {
      this(imageRequest, null);
    }

    public ImageDebugData(@Nullable String localPath) {
      this(null, localPath);
    }

    public ImageDebugData(@Nullable com.facebook.imagepipeline.request.ImageRequest imageRequest, @Nullable String localPath) {
      mImageRequest = imageRequest;
      mLocalPath = localPath;
    }

    @Nullable
    public com.facebook.imagepipeline.request.ImageRequest getImageRequest() {
      return mImageRequest;
    }

    @Nullable
    public Set<com.facebook.cache.common.CacheKey> getCacheKeys() {
      return mCacheKeys;
    }

    public void addCacheKey(com.facebook.cache.common.CacheKey cacheKey) {
      if (mCacheKeys == null) {
        mCacheKeys = new HashSet<>();
      }
      mCacheKeys.add(cacheKey);
    }

    @Nullable
    public Set<String> getRequestIds() {
      return mRequestIds;
    }

    public String getUniqueId() {
      return Integer.toString(hashCode());
    }

    public void addRequestId(String requestId) {
      if (mRequestIds == null) {
        mRequestIds = new HashSet<>();
      }
      mRequestIds.add(requestId);
    }

    public void addResourceId(String resourceId) {
      if (resourceId == null) {
        return;
      }
      if (mResourceIds == null) {
        mResourceIds = new HashSet<>();
      }
      mResourceIds.add(resourceId);
    }

    @Nullable
    public com.facebook.drawee.backends.pipeline.info.ImagePerfData getImagePerfData() {
      return mImagePerfData;
    }

    public void setImagePerfData(@Nullable com.facebook.drawee.backends.pipeline.info.ImagePerfData imagePerfData) {
      mImagePerfData = imagePerfData;
    }

    @Nullable
    public Set<String> getResourceIds() {
      return mResourceIds;
    }

    @Nullable
    public String getLocalPath() {
      return mLocalPath;
    }

  }

  public synchronized FlipperImageTracker.ImageDebugData trackImage(String localPath, com.facebook.cache.common.CacheKey key) {
    ImageDebugData data = new ImageDebugData(localPath);
    mImageDebugDataMap.put(key, data);
    return data;
  }

  public synchronized FlipperImageTracker.ImageDebugData trackImage(com.facebook.cache.common.CacheKey key) {
    ImageDebugData data = new ImageDebugData();
    mImageDebugDataMap.put(key, data);
    return data;
  }

  public synchronized String getUriString(com.facebook.cache.common.CacheKey key) {
    ImageDebugData imageDebugData = getImageDebugData(key);
    if (imageDebugData != null) {
      ImageRequest imageRequest = imageDebugData.getImageRequest();
      if (imageRequest != null) {
        return imageRequest.getSourceUri().toString();
      }
    }
    return key.getUriString();
  }

  @Nullable
  public synchronized String getLocalPath(com.facebook.cache.common.CacheKey key) {
    ImageDebugData imageDebugData = getImageDebugData(key);
    if (imageDebugData != null) {
      return imageDebugData.getLocalPath();
    }
    return null;
  }

  @Nullable
  public synchronized FlipperImageTracker.ImageDebugData getImageDebugData(com.facebook.cache.common.CacheKey key) {
    return mImageDebugDataMap.get(key);
  }

  @Nullable
  public synchronized FlipperImageTracker.ImageDebugData getDebugDataForRequestId(String requestId) {
    for (ImageDebugData debugData : mImageRequestDebugDataMap.values()) {
      Set<String> requestIds = debugData.getRequestIds();
      if (requestIds != null && requestIds.contains(requestId)) {
        return debugData;
      }
    }
    return null;
  }

  @Nullable
  public synchronized FlipperImageTracker.ImageDebugData getDebugDataForResourceId(String resourceId) {
    for (ImageDebugData debugData : mImageRequestDebugDataMap.values()) {
      Set<String> ids = debugData.getResourceIds();
      if (ids != null && ids.contains(resourceId)) {
        return debugData;
      }
    }
    return null;
  }

  @Nullable
  public synchronized com.facebook.cache.common.CacheKey getCacheKey(String imageId) {
    for (Map.Entry<CacheKey, ImageDebugData> entry : mImageDebugDataMap.entrySet()) {
      if (entry.getValue().getUniqueId().equals(imageId)) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public synchronized void onImageLoadStatusUpdated(com.facebook.drawee.backends.pipeline.info.ImagePerfData imagePerfData, @ImageLoadStatus int imageLoadStatus) {
    if (imagePerfData == null || imagePerfData.getImageRequest() == null) {
      return;
    }
    ImageDebugData debugData = mImageRequestDebugDataMap.get(imagePerfData.getImageRequest());
    if (debugData != null) {
      debugData.setImagePerfData(imagePerfData);
    } else {
      ImageDebugData imageDebugData = new ImageDebugData(imagePerfData.getImageRequest());
      imageDebugData.setImagePerfData(imagePerfData);
      mImageRequestDebugDataMap.put(imagePerfData.getImageRequest(), imageDebugData);
    }
  }

  @Override
  public synchronized void onImageVisibilityUpdated(com.facebook.drawee.backends.pipeline.info.ImagePerfData imagePerfData, @VisibilityState int visibilityState) {
    // ignore
  }

}
