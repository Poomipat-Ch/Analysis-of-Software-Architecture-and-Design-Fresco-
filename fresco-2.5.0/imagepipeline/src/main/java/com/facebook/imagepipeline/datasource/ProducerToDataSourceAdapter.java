/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.datasource;

import com.facebook.datasource.DataSource;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.concurrent.ThreadSafe;
/**
 * DataSource<T> backed by a Producer<T>
 * 
 * @param <T>
 */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
public class ProducerToDataSourceAdapter<T> extends AbstractProducerToDataSourceAdapter<> {
  public static <T> com.facebook.datasource.DataSource<T> create(com.facebook.imagepipeline.producers.Producer<T> producer, com.facebook.imagepipeline.producers.SettableProducerContext settableProducerContext, com.facebook.imagepipeline.listener.RequestListener2 listener)
  {
    return new ProducerToDataSourceAdapter<T>(producer, settableProducerContext, listener);
  }

  private ProducerToDataSourceAdapter(com.facebook.imagepipeline.producers.Producer<T> producer, com.facebook.imagepipeline.producers.SettableProducerContext settableProducerContext, com.facebook.imagepipeline.listener.RequestListener2 listener) {
    super(producer, settableProducerContext, listener);
  }

}
