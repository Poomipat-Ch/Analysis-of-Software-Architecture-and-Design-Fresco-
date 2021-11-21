/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.view.View;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.configs.glide.GlideApp;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
/**
 *  This is the Holder class for the RecycleView to use with Glide 
 */
public class GlideHolder extends BaseViewHolder<> {
  public GlideHolder(Context context, View layoutView, com.facebook.samples.comparison.instrumentation.InstrumentedImageView instrumentedImageView, com.facebook.samples.comparison.instrumentation.PerfListener perfListener) {
    super(context, layoutView, instrumentedImageView, perfListener);
  }

  @Override
  protected void onBind(String uri) {
    GlideApp.with(mImageView.getContext())
        .load(uri)
        .placeholder(Drawables.sPlaceholderDrawable)
        .error(Drawables.sErrorDrawable)
        .transition(withCrossFade())
        .into(mImageView);
  }

}
