/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.drawable;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 *  Performs scale type calculations. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ScalingUtils {
  public interface ScaleType {
    /**
     * 
     * Scales width and height independently, so that the child matches the parent exactly. This may
     * change the aspect ratio of the child.
     * 
     */
    ScalingUtils.ScaleType FIT_XY =  ScaleTypeFitXY.INSTANCE;

    /**
     * 
     * Scales the child so that the child's width fits exactly. The height will be cropped if it
     * exceeds parent's bounds. Aspect ratio is preserved. Child is centered within the parent's
     * bounds.
     * 
     */
    ScalingUtils.ScaleType FIT_X =  ScaleTypeFitX.INSTANCE;

    /**
     * 
     * Scales the child so that the child's height fits exactly. The width will be cropped if it
     * exceeds parent's bounds. Aspect ratio is preserved. Child is centered within the parent's
     * bounds.
     * 
     */
    ScalingUtils.ScaleType FIT_Y =  ScaleTypeFitY.INSTANCE;

    /**
     * 
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved. Child is aligned to the top-left corner
     * of the parent.
     * 
     */
    ScalingUtils.ScaleType FIT_START =  ScaleTypeFitStart.INSTANCE;

    /**
     * 
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved. Child is centered within the parent's
     * bounds.
     * 
     */
    ScalingUtils.ScaleType FIT_CENTER =  ScaleTypeFitCenter.INSTANCE;

    /**
     * 
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved. Child is aligned to the bottom-right
     * corner of the parent.
     * 
     */
    ScalingUtils.ScaleType FIT_END =  ScaleTypeFitEnd.INSTANCE;

    /**
     *  Performs no scaling. Child is centered within parent's bounds. 
     */
    ScalingUtils.ScaleType CENTER =  ScaleTypeCenter.INSTANCE;

    /**
     * 
     * Scales the child so that it fits entirely inside the parent. Unlike FIT_CENTER, if the child
     * is smaller, no up-scaling will be performed. Aspect ratio is preserved. Child is centered
     * within parent's bounds.
     * 
     */
    ScalingUtils.ScaleType CENTER_INSIDE =  ScaleTypeCenterInside.INSTANCE;

    /**
     * 
     * Scales the child so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. At least one dimension (width or height) will fit exactly. Child is
     * centered within parent's bounds.
     * 
     */
    ScalingUtils.ScaleType CENTER_CROP =  ScaleTypeCenterCrop.INSTANCE;

    /**
     * 
     * Scales the child so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. At least one dimension (width or height) will fit exactly. The
     * child's focus point will be centered within the parent's bounds as much as possible without
     * leaving empty space. It is guaranteed that the focus point will be visible and centered as
     * much as possible. If the focus point is set to (0.5f, 0.5f), result will be equivalent to
     * CENTER_CROP.
     * 
     */
    ScalingUtils.ScaleType FOCUS_CROP =  ScaleTypeFocusCrop.INSTANCE;

    /**
     * 
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved. Child is aligned to the bottom-left
     * corner of the parent.
     * 
     */
    ScalingUtils.ScaleType FIT_BOTTOM_START =  ScaleTypeFitBottomStart.INSTANCE;

    /**
     * Gets transformation matrix based on the scale type.
     * 
     * @param outTransform out matrix to store result
     * @param parentBounds parent bounds
     * @param childWidth child width
     * @param childHeight child height
     * @param focusX focus point x coordinate, relative [0...1]
     * @param focusY focus point y coordinate, relative [0...1]
     * @return same reference to the out matrix for convenience
     */
    Matrix getTransform(Matrix outTransform, Rect parentBounds, int childWidth, int childHeight, float focusX, float focusY) ;

  }

  @Nullable
  public static ScaleTypeDrawable getActiveScaleTypeDrawable(@Nullable Drawable drawable)
  {
    if (drawable == null) {
      return null;
    } else if (drawable instanceof ScaleTypeDrawable) {
      return (ScaleTypeDrawable) drawable;
    } else if (drawable instanceof DrawableParent) {
      final Drawable childDrawable = ((DrawableParent) drawable).getDrawable();
      return getActiveScaleTypeDrawable(childDrawable);
    } else if (drawable instanceof ArrayDrawable) {
      final ArrayDrawable fadeDrawable = (ArrayDrawable) drawable;
      final int numLayers = fadeDrawable.getNumberOfLayers();

      for (int i = 0; i < numLayers; i++) {
        final Drawable childDrawable = fadeDrawable.getDrawable(i);
        final ScaleTypeDrawable result = getActiveScaleTypeDrawable(childDrawable);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  public static abstract class AbstractScaleType implements ScalingUtils.ScaleType {
    @Override
    public Matrix getTransform(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY) {
      final float sX = (float) parentRect.width() / (float) childWidth;
      final float sY = (float) parentRect.height() / (float) childHeight;
      getTransformImpl(outTransform, parentRect, childWidth, childHeight, focusX, focusY, sX, sY);
      return outTransform;
    }

    public abstract void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) ;

  }

  private static class ScaleTypeFitXY extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitXY();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float dx = parentRect.left;
      float dy = parentRect.top;
      outTransform.setScale(scaleX, scaleY);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_xy";
    }

  }

  private static class ScaleTypeFitStart extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitStart();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left;
      float dy = parentRect.top;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_start";
    }

  }

  private static class ScaleTypeFitBottomStart extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitBottomStart();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left;
      float dy = parentRect.top + (parentRect.height() - childHeight * scale);
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_bottom_start";
    }

  }

  private static class ScaleTypeFitCenter extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitCenter();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      float dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_center";
    }

  }

  private static class ScaleTypeFitEnd extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitEnd();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left + (parentRect.width() - childWidth * scale);
      float dy = parentRect.top + (parentRect.height() - childHeight * scale);
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_end";
    }

  }

  private static class ScaleTypeCenter extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeCenter();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float dx = parentRect.left + (parentRect.width() - childWidth) * 0.5f;
      float dy = parentRect.top + (parentRect.height() - childHeight) * 0.5f;
      outTransform.setTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "center";
    }

  }

  private static class ScaleTypeCenterInside extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeCenterInside();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
      float dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      float dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "center_inside";
    }

  }

  private static class ScaleTypeCenterCrop extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeCenterCrop();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale, dx, dy;
      if (scaleY > scaleX) {
        scale = scaleY;
        dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
        dy = parentRect.top;
      } else {
        scale = scaleX;
        dx = parentRect.left;
        dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      }
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "center_crop";
    }

  }

  private static class ScaleTypeFocusCrop extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFocusCrop();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale, dx, dy;
      if (scaleY > scaleX) {
        scale = scaleY;
        dx = parentRect.width() * 0.5f - childWidth * scale * focusX;
        dx = parentRect.left + Math.max(Math.min(dx, 0), parentRect.width() - childWidth * scale);
        dy = parentRect.top;
      } else {
        scale = scaleX;
        dx = parentRect.left;
        dy = parentRect.height() * 0.5f - childHeight * scale * focusY;
        dy = parentRect.top + Math.max(Math.min(dy, 0), parentRect.height() - childHeight * scale);
      }
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "focus_crop";
    }

  }

  private static class ScaleTypeFitX extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitX();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale, dx, dy;
      scale = scaleX;
      dx = parentRect.left;
      dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_x";
    }

  }

  private static class ScaleTypeFitY extends ScalingUtils.AbstractScaleType {
    public static final ScalingUtils.ScaleType INSTANCE =  new ScaleTypeFitY();

    @Override
    public void getTransformImpl(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY, float scaleX, float scaleY) {
      float scale, dx, dy;
      scale = scaleY;
      dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      dy = parentRect.top;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    @Override
    public String toString() {
      return "fit_y";
    }

  }

  public interface StatefulScaleType {
    /**
     * Returns the internal state. The returned object must be immutable!
     * 
     * <p>The returned state may be used for caching the result of {@code ScaleType.getTransform}.
     * If null state is returned, the result will not be cached. If non-null state is returned, the
     * old transformation may be used if produced with an equal state.
     */
    Object getState() ;

  }

  public static class InterpolatingScaleType implements ScalingUtils.ScaleType, ScalingUtils.StatefulScaleType {
    private final ScalingUtils.ScaleType mScaleTypeFrom;

    private final ScalingUtils.ScaleType mScaleTypeTo;

    @Nullable
    private final Rect mBoundsFrom;

    @Nullable
    private final Rect mBoundsTo;

    @Nullable
    private final PointF mFocusPointFrom;

    @Nullable
    private final PointF mFocusPointTo;

    private final float[] mMatrixValuesFrom =  new float[9];

    private final float[] mMatrixValuesTo =  new float[9];

    private final float[] mMatrixValuesInterpolated =  new float[9];

    private float mInterpolatingValue;

    public InterpolatingScaleType(ScalingUtils.ScaleType scaleTypeFrom, ScalingUtils.ScaleType scaleTypeTo, @Nullable Rect boundsFrom, @Nullable Rect boundsTo, @Nullable PointF focusPointFrom, @Nullable PointF focusPointTo) {
      mScaleTypeFrom = scaleTypeFrom;
      mScaleTypeTo = scaleTypeTo;
      mBoundsFrom = boundsFrom;
      mBoundsTo = boundsTo;
      mFocusPointFrom = focusPointFrom;
      mFocusPointTo = focusPointTo;
    }

    public InterpolatingScaleType(ScalingUtils.ScaleType scaleTypeFrom, ScalingUtils.ScaleType scaleTypeTo, @Nullable Rect boundsFrom, @Nullable Rect boundsTo) {
      this(scaleTypeFrom, scaleTypeTo, boundsFrom, boundsTo, null, null);
    }

    public InterpolatingScaleType(ScalingUtils.ScaleType scaleTypeFrom, ScalingUtils.ScaleType scaleTypeTo) {
      this(scaleTypeFrom, scaleTypeTo, null, null);
    }

    public ScalingUtils.ScaleType getScaleTypeFrom() {
      return mScaleTypeFrom;
    }

    public ScalingUtils.ScaleType getScaleTypeTo() {
      return mScaleTypeTo;
    }

    @Nullable
    public Rect getBoundsFrom() {
      return mBoundsFrom;
    }

    @Nullable
    public Rect getBoundsTo() {
      return mBoundsTo;
    }

    @Nullable
    public PointF getFocusPointFrom() {
      return mFocusPointFrom;
    }

    @Nullable
    public PointF getFocusPointTo() {
      return mFocusPointTo;
    }

    /**
     * Sets the interpolating value.
     * 
     * <p>Value of 0.0 will produce the transform same as ScaleTypeFrom. Value of 1.0 will produce
     * the transform same as ScaleTypeTo. Inbetween values will produce a transform that is a linear
     * combination between the two.
     */
    public void setValue(float value) {
      mInterpolatingValue = value;
    }

    /**
     *  Gets the interpolating value. 
     */
    public float getValue() {
      return mInterpolatingValue;
    }

    @Override
    public Object getState() {
      return mInterpolatingValue;
    }

    @Override
    public Matrix getTransform(Matrix transform, Rect parentBounds, int childWidth, int childHeight, float focusX, float focusY) {
      Rect boundsFrom = (mBoundsFrom != null) ? mBoundsFrom : parentBounds;
      Rect boundsTo = (mBoundsTo != null) ? mBoundsTo : parentBounds;

      mScaleTypeFrom.getTransform(
          transform,
          boundsFrom,
          childWidth,
          childHeight,
          mFocusPointFrom == null ? focusX : mFocusPointFrom.x,
          mFocusPointFrom == null ? focusY : mFocusPointFrom.y);
      transform.getValues(mMatrixValuesFrom);
      mScaleTypeTo.getTransform(
          transform,
          boundsTo,
          childWidth,
          childHeight,
          mFocusPointTo == null ? focusX : mFocusPointTo.x,
          mFocusPointTo == null ? focusY : mFocusPointTo.y);
      transform.getValues(mMatrixValuesTo);

      for (int i = 0; i < 9; i++) {
        mMatrixValuesInterpolated[i] =
            mMatrixValuesFrom[i] * (1 - mInterpolatingValue)
                + mMatrixValuesTo[i] * mInterpolatingValue;
      }
      transform.setValues(mMatrixValuesInterpolated);
      return transform;
    }

    @Override
    public String toString() {
      return String.format(
          "InterpolatingScaleType(%s (%s) -> %s (%s))",
          String.valueOf(mScaleTypeFrom),
          String.valueOf(mFocusPointFrom),
          String.valueOf(mScaleTypeTo),
          String.valueOf(mFocusPointTo));
    }

  }

}
