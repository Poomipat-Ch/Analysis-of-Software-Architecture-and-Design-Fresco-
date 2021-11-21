/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.samples.comparison.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.samples.comparison.holders.BaseViewHolder;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import java.util.LinkedList;
import java.util.List;
/**
 *  Base class for RecyclerView Adapters 
 */
public abstract class ImageListAdapter extends RecyclerView.Adapter<BaseViewHolder<?>> {
  private final com.facebook.samples.comparison.instrumentation.PerfListener mPerfListener;

  private final Context mContext;

  private List<String> mModel;

  public ImageListAdapter(final Context context, final com.facebook.samples.comparison.instrumentation.PerfListener perfListener) {
    this.mContext = context;
    this.mPerfListener = perfListener;
    this.mModel = new LinkedList<String>();
  }

  public void addUrl(final String url) {
    mModel.add(url);
  }

  protected com.facebook.samples.comparison.instrumentation.PerfListener getPerfListener() {
    return mPerfListener;
  }

  protected String getItem(final int position) {
    return mModel.get(position);
  }

  @Override
  public int getItemCount() {
    return mModel.size();
  }

  protected Context getContext() {
    return mContext;
  }

  public void clear() {
    mModel.clear();
  }

  @Override
  public void onBindViewHolder(com.facebook.samples.comparison.holders.BaseViewHolder<?> holder, int position) {
    holder.bind(getItem(position));
  }

  /**
   *  Releases any resources and tears down the adapter. 
   */
  public abstract void shutDown() ;

}
