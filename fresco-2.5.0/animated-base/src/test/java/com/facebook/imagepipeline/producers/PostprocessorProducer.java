/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.producers;

import android.graphics.Bitmap;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessorRunner;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
/**
 * Runs a caller-supplied post-processor object.
 * 
 * <p>Post-processors are only supported for static bitmaps. If the request is for an animated
 * image, the post-processor step will be skipped without warning.
 */
public class PostprocessorProducer implements Producer<> {
  class RepeatedPostprocessorConsumer extends DelegatingConsumer<, > implements com.facebook.imagepipeline.request.RepeatedPostprocessorRunner {
    @GuardedBy("RepeatedPostprocessorConsumer.this")
    private boolean mIsClosed =  false;

    @GuardedBy("RepeatedPostprocessorConsumer.this")
    @Nullable
    private com.facebook.common.references.CloseableReference<CloseableImage> mSourceImageRef =  null;

    private RepeatedPostprocessorConsumer(PostprocessorProducer.PostprocessorConsumer postprocessorConsumer, com.facebook.imagepipeline.request.RepeatedPostprocessor repeatedPostprocessor, ProducerContext context) {
      super(postprocessorConsumer);
      repeatedPostprocessor.setCallback(RepeatedPostprocessorConsumer.this);
      context.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              if (close()) {
                getConsumer().onCancellation();
              }
            }
          });
    }

    @Override
    protected void onNewResultImpl(com.facebook.common.references.CloseableReference<CloseableImage> newResult, @Status int status) {
      // ignore intermediate results
      if (isNotLast(status)) {
        return;
      }
      setSourceImageRef(newResult);
      updateInternal();
    }

    @Override
    protected void onFailureImpl(Throwable throwable) {
      if (close()) {
        getConsumer().onFailure(throwable);
      }
    }

    @Override
    protected void onCancellationImpl() {
      if (close()) {
        getConsumer().onCancellation();
      }
    }

    @Override
    public synchronized void update() {
      updateInternal();
    }

    private void updateInternal() {
      CloseableReference<CloseableImage> sourceImageRef;
      synchronized (RepeatedPostprocessorConsumer.this) {
        if (mIsClosed) {
          return;
        }
        sourceImageRef = CloseableReference.cloneOrNull(mSourceImageRef);
      }
      try {
        getConsumer().onNewResult(sourceImageRef, NO_FLAGS);
      } finally {
        CloseableReference.closeSafely(sourceImageRef);
      }
    }

    private void setSourceImageRef(com.facebook.common.references.CloseableReference<CloseableImage> sourceImageRef) {
      CloseableReference<CloseableImage> oldSourceImageRef;
      synchronized (RepeatedPostprocessorConsumer.this) {
        if (mIsClosed) {
          return;
        }
        oldSourceImageRef = mSourceImageRef;
        mSourceImageRef = CloseableReference.cloneOrNull(sourceImageRef);
      }
      CloseableReference.closeSafely(oldSourceImageRef);
    }

    private boolean close() {
      CloseableReference<CloseableImage> oldSourceImageRef;
      synchronized (RepeatedPostprocessorConsumer.this) {
        if (mIsClosed) {
          return false;
        }
        oldSourceImageRef = mSourceImageRef;
        mSourceImageRef = null;
        mIsClosed = true;
      }
      CloseableReference.closeSafely(oldSourceImageRef);
      return true;
    }

  }

  class SingleUsePostprocessorConsumer extends DelegatingConsumer<, > {
    private SingleUsePostprocessorConsumer(PostprocessorProducer.PostprocessorConsumer postprocessorConsumer) {
      super(postprocessorConsumer);
    }

    @Override
    protected void onNewResultImpl(final com.facebook.common.references.CloseableReference<CloseableImage> newResult, @Status int status) {
      // ignore intermediate results
      if (isNotLast(status)) {
        return;
      }
      getConsumer().onNewResult(newResult, status);
    }

  }

  public static final String NAME =  "PostprocessorProducer";

  @VisibleForTesting
  static final String POSTPROCESSOR =  "Postprocessor";

  private final Producer<CloseableReference<CloseableImage>> mInputProducer;

  private final com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory mBitmapFactory;

  private final Executor mExecutor;

  public PostprocessorProducer(Producer<CloseableReference<CloseableImage>> inputProducer, com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory platformBitmapFactory, Executor executor) {
    mInputProducer = Preconditions.checkNotNull(inputProducer);
    mBitmapFactory = platformBitmapFactory;
    mExecutor = Preconditions.checkNotNull(executor);
  }

  @Override
  public void produceResults(final Consumer<CloseableReference<CloseableImage>> consumer, ProducerContext context) {
    final ProducerListener2 listener = context.getProducerListener();
    final Postprocessor postprocessor = context.getImageRequest().getPostprocessor();
    final PostprocessorConsumer basePostprocessorConsumer =
        new PostprocessorConsumer(consumer, listener, postprocessor, context);
    final Consumer<CloseableReference<CloseableImage>> postprocessorConsumer;
    if (postprocessor instanceof RepeatedPostprocessor) {
      postprocessorConsumer =
          new RepeatedPostprocessorConsumer(
              basePostprocessorConsumer, (RepeatedPostprocessor) postprocessor, context);
    } else {
      postprocessorConsumer = new SingleUsePostprocessorConsumer(basePostprocessorConsumer);
    }
    mInputProducer.produceResults(postprocessorConsumer, context);
  }

  private class PostprocessorConsumer extends DelegatingConsumer<, > {
    private final ProducerListener2 mListener;

    private final ProducerContext mProducerContext;

    private final com.facebook.imagepipeline.request.Postprocessor mPostprocessor;

    @GuardedBy("PostprocessorConsumer.this")
    private boolean mIsClosed;

    @GuardedBy("PostprocessorConsumer.this")
    @Nullable
    private com.facebook.common.references.CloseableReference<CloseableImage> mSourceImageRef =  null;

    @GuardedBy("PostprocessorConsumer.this")
    private int mStatus =  0;

    @GuardedBy("PostprocessorConsumer.this")
    private boolean mIsDirty =  false;

    @GuardedBy("PostprocessorConsumer.this")
    private boolean mIsPostProcessingRunning =  false;

    public PostprocessorConsumer(Consumer<CloseableReference<CloseableImage>> consumer, ProducerListener2 listener, com.facebook.imagepipeline.request.Postprocessor postprocessor, ProducerContext producerContext) {
      super(consumer);
      mListener = listener;
      mPostprocessor = postprocessor;
      mProducerContext = producerContext;
      producerContext.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              maybeNotifyOnCancellation();
            }
          });
    }

    @Override
    protected void onNewResultImpl(@Nullable com.facebook.common.references.CloseableReference<CloseableImage> newResult, @Status int status) {
      if (!CloseableReference.isValid(newResult)) {
        // try to propagate if the last result is invalid
        if (isLast(status)) {
          maybeNotifyOnNewResult(null, status);
        }
        // ignore if invalid
        return;
      }
      updateSourceImageRef(newResult, status);
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      maybeNotifyOnFailure(t);
    }

    @Override
    protected void onCancellationImpl() {
      maybeNotifyOnCancellation();
    }

    private void updateSourceImageRef(@Nullable com.facebook.common.references.CloseableReference<CloseableImage> sourceImageRef, int status) {
      CloseableReference<CloseableImage> oldSourceImageRef;
      boolean shouldSubmit;
      synchronized (PostprocessorConsumer.this) {
        if (mIsClosed) {
          return;
        }
        oldSourceImageRef = mSourceImageRef;
        mSourceImageRef = CloseableReference.cloneOrNull(sourceImageRef);
        mStatus = status;
        mIsDirty = true;
        shouldSubmit = setRunningIfDirtyAndNotRunning();
      }
      CloseableReference.closeSafely(oldSourceImageRef);
      if (shouldSubmit) {
        submitPostprocessing();
      }
    }

    private void submitPostprocessing() {
      mExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              CloseableReference<CloseableImage> closeableImageRef;
              int status;
              synchronized (PostprocessorConsumer.this) {
                // instead of cloning and closing the reference, we do a more efficient move.
                closeableImageRef = mSourceImageRef;
                status = mStatus;
                mSourceImageRef = null;
                mIsDirty = false;
              }

              if (CloseableReference.isValid(closeableImageRef)) {
                try {
                  doPostprocessing(closeableImageRef, status);
                } finally {
                  CloseableReference.closeSafely(closeableImageRef);
                }
              }
              clearRunningAndStartIfDirty();
            }
          });
    }

    private void clearRunningAndStartIfDirty() {
      boolean shouldExecuteAgain;
      synchronized (PostprocessorConsumer.this) {
        mIsPostProcessingRunning = false;
        shouldExecuteAgain = setRunningIfDirtyAndNotRunning();
      }
      if (shouldExecuteAgain) {
        submitPostprocessing();
      }
    }

    private synchronized boolean setRunningIfDirtyAndNotRunning() {
      if (!mIsClosed
          && mIsDirty
          && !mIsPostProcessingRunning
          && CloseableReference.isValid(mSourceImageRef)) {
        mIsPostProcessingRunning = true;
        return true;
      }
      return false;
    }

    private void doPostprocessing(com.facebook.common.references.CloseableReference<CloseableImage> sourceImageRef, int status) {
      Preconditions.checkArgument(CloseableReference.isValid(sourceImageRef));
      if (!shouldPostprocess(sourceImageRef.get())) {
        maybeNotifyOnNewResult(sourceImageRef, status);
        return;
      }
      mListener.onProducerStart(mProducerContext, NAME);
      CloseableReference<CloseableImage> destImageRef = null;
      try {
        try {
          destImageRef = postprocessInternal(sourceImageRef.get());
        } catch (Exception e) {
          mListener.onProducerFinishWithFailure(
              mProducerContext, NAME, e, getExtraMap(mListener, mProducerContext, mPostprocessor));
          maybeNotifyOnFailure(e);
          return;
        }
        mListener.onProducerFinishWithSuccess(
            mProducerContext, NAME, getExtraMap(mListener, mProducerContext, mPostprocessor));
        maybeNotifyOnNewResult(destImageRef, status);
      } finally {
        CloseableReference.closeSafely(destImageRef);
      }
    }

    @Nullable
    private Map<String, String> getExtraMap(ProducerListener2 listener, ProducerContext producerContext, com.facebook.imagepipeline.request.Postprocessor postprocessor) {
      if (!listener.requiresExtraMap(producerContext, NAME)) {
        return null;
      }
      return ImmutableMap.of(POSTPROCESSOR, postprocessor.getName());
    }

    private boolean shouldPostprocess(com.facebook.imagepipeline.image.CloseableImage sourceImage) {
      return (sourceImage instanceof CloseableStaticBitmap);
    }

    private com.facebook.common.references.CloseableReference<CloseableImage> postprocessInternal(com.facebook.imagepipeline.image.CloseableImage sourceImage) {
      CloseableStaticBitmap staticBitmap = (CloseableStaticBitmap) sourceImage;
      Bitmap sourceBitmap = staticBitmap.getUnderlyingBitmap();
      CloseableReference<Bitmap> bitmapRef = mPostprocessor.process(sourceBitmap, mBitmapFactory);
      int rotationAngle = staticBitmap.getRotationAngle();
      int exifOrientation = staticBitmap.getExifOrientation();
      try {
        CloseableStaticBitmap closeableStaticBitmap =
            new CloseableStaticBitmap(
                bitmapRef, sourceImage.getQualityInfo(), rotationAngle, exifOrientation);
        closeableStaticBitmap.setImageExtras(staticBitmap.getExtras());
        return CloseableReference.<CloseableImage>of(closeableStaticBitmap);
      } finally {
        CloseableReference.closeSafely(bitmapRef);
      }
    }

    private void maybeNotifyOnNewResult(com.facebook.common.references.CloseableReference<CloseableImage> newRef, int status) {
      boolean isLast = isLast(status);
      if ((!isLast && !isClosed()) || (isLast && close())) {
        getConsumer().onNewResult(newRef, status);
      }
    }

    private void maybeNotifyOnFailure(Throwable throwable) {
      if (close()) {
        getConsumer().onFailure(throwable);
      }
    }

    private void maybeNotifyOnCancellation() {
      if (close()) {
        getConsumer().onCancellation();
      }
    }

    private synchronized boolean isClosed() {
      return mIsClosed;
    }

    private boolean close() {
      CloseableReference<CloseableImage> oldSourceImageRef;
      synchronized (PostprocessorConsumer.this) {
        if (mIsClosed) {
          return false;
        }
        oldSourceImageRef = mSourceImageRef;
        mSourceImageRef = null;
        mIsClosed = true;
      }
      CloseableReference.closeSafely(oldSourceImageRef);
      return true;
    }

  }

}
