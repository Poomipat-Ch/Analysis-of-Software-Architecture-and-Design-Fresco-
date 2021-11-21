/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.ui.common;

import android.net.Uri;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.PropagatesNullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public interface ControllerListener2<INFO> {
  public class Extras {
    @Nullable
    public Map<String, Object> componentExtras;

    @Nullable
    public Map<String, Object> shortcutExtras;

    @Nullable
    public Map<String, Object> datasourceExtras;

    @Nullable
    public Map<String, Object> imageExtras;

    @Nullable
    public Object callerContext;

    @Nullable
    public Uri mainUri;

    public int viewportWidth =  -1;

    public int viewportHeight =  -1;

    @Nullable
    public Object scaleType;

    public float focusX =  -1;

    public float focusY =  -1;

    public static ControllerListener2.Extras of(@Nullable Map<String, Object> componentExtras)
    {
      Extras extras = new Extras();
      extras.componentExtras = componentExtras;
      return extras;
    }

    public ControllerListener2.Extras makeExtrasCopy() {
      Extras extras = new Extras();
      extras.componentExtras = copyMap(this.componentExtras);
      extras.shortcutExtras = copyMap(this.shortcutExtras);
      extras.datasourceExtras = copyMap(this.datasourceExtras);
      extras.imageExtras = copyMap(this.imageExtras);
      extras.callerContext = this.callerContext;
      extras.mainUri = this.mainUri;
      extras.viewportWidth = this.viewportWidth;
      extras.viewportHeight = this.viewportHeight;
      extras.scaleType = this.scaleType;
      extras.focusX = this.focusX;
      extras.focusY = this.focusY;

      return extras;
    }

    private static Map<String, Object> copyMap(@PropagatesNullable Map<String, Object> map)
    {
      if (map == null) {
        return null;
      }

      return new ConcurrentHashMap<>(map);
    }

  }

  /**
   * Called before the image request is submitted.
   * 
   * <p>IMPORTANT: It is not safe to reuse the controller from within this callback!
   * 
   * @param id controller id
   * @param callerContext caller context
   * @param extraData extra data
   */
  void onSubmit(String id, @Nullable Object callerContext, @Nullable ControllerListener2.Extras extraData) ;

  /**
   * Called after the final image has been set.
   * 
   * @param id controller id
   * @param imageInfo image info
   * @param extraData extra data
   */
  void onFinalImageSet(String id, @Nullable INFO imageInfo, @Nullable ControllerListener2.Extras extraData) ;

  /**
   * Called after any intermediate image has been set.
   * 
   * @param id controller id
   */
  void onIntermediateImageSet(String id, @Nullable INFO imageInfo) ;

  /**
   * Called after the fetch of the intermediate image failed.
   * 
   * @param id controller id
   */
  void onIntermediateImageFailed(String id) ;

  /**
   * Called after the fetch of the final image failed.
   * 
   * @param id controller id
   * @param throwable failure cause
   * @param extraData extra data
   */
  void onFailure(String id, @Nullable Throwable throwable, @Nullable ControllerListener2.Extras extraData) ;

  /**
   * Called after the controller released the fetched image.
   * 
   * <p>IMPORTANT: It is not safe to reuse the controller from within this callback!
   * 
   * @param id controller id
   * @param extraData extra data
   */
  void onRelease(String id, @Nullable ControllerListener2.Extras extraData) ;

}
