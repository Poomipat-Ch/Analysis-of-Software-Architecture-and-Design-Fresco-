/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.base;

import com.facebook.infer.annotation.Nullsafe;
/**
 *  Info per frame returned by {@link AnimatedDrawableBackend}. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class AnimatedDrawableFrameInfo {
  public enum DisposalMethod {
    DISPOSE_DO_NOT,/**
     *  Do not dipose the frame. Leave as-is. 
     */

    DISPOSE_TO_BACKGROUND,/**
     *  Dispose to the background color 
     */

    DISPOSE_TO_PREVIOUS,/**
     *  Dispose to the previous frame 
     */
;
  }

  public enum BlendOperation {
    BLEND_WITH_PREVIOUS,/**
     *  Blend * 
     */

    NO_BLEND,/**
     *  Do not blend * 
     */
;
  }

  public final int frameNumber;

  public final int xOffset;

  public final int yOffset;

  public final int width;

  public final int height;

  public final AnimatedDrawableFrameInfo.BlendOperation blendOperation;

  public final AnimatedDrawableFrameInfo.DisposalMethod disposalMethod;

  public AnimatedDrawableFrameInfo(int frameNumber, int xOffset, int yOffset, int width, int height, AnimatedDrawableFrameInfo.BlendOperation blendOperation, AnimatedDrawableFrameInfo.DisposalMethod disposalMethod) {
    this.frameNumber = frameNumber;
    this.xOffset = xOffset;
    this.yOffset = yOffset;
    this.width = width;
    this.height = height;
    this.blendOperation = blendOperation;
    this.disposalMethod = disposalMethod;
  }

}
