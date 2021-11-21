/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.SharedReference;
import java.io.Closeable;
import org.mockito.*;
import static org.mockito.ArgumentMatchers.*;
/**
 *  Utilities for testing {@link CloseableReference}. 
 */
public class CloseableReferences {
  private static class CloseableReferenceMatcher<T extends Closeable> extends ArgumentMatcher<CloseableReference<T>> {
    private final com.facebook.common.references.CloseableReference<T> mCloseableReference;

    public CloseableReferenceMatcher(com.facebook.common.references.CloseableReference<T> closeableReference) {
      mCloseableReference = closeableReference;
    }

    @Override
    public boolean matches(com.facebook.common.references.CloseableReference argument) {
      return mCloseableReference.getUnderlyingReferenceTestOnly()
          == argument.getUnderlyingReferenceTestOnly();
    }

  }

  /**
   * Returns a Mockito ArgumentMatcher that checks that its argument has the same underlying {@link
   * SharedReference}
   */
  public static <T extends Closeable> com.facebook.common.references.CloseableReference<T> eqUnderlying(com.facebook.common.references.CloseableReference<T> closeableReference)
  {
    return argThat(new CloseableReferenceMatcher<T>(closeableReference));
  }

}
