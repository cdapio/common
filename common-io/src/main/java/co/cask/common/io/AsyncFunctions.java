/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package co.cask.common.io;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.Executor;

/**
 * Static utility methods for operating with {@link AsyncFunction}.
 */
public final class AsyncFunctions {

  /**
   * Converts a {@link Function} into {@link AsyncFunction} by performing the operation in the given executor.
   *
   * @param function Function to apply
   * @param executor Executor for the function to execute in
   * @param <I> Input type
   * @param <O> Output type
   * @return A {@link AsyncFunction} that will call the function in the given executor.
   */
  public static <I, O> AsyncFunction<I, O> asyncWrap(final Function<I, O> function, final Executor executor) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(final I input) throws Exception {
        final SettableFuture<O> resultFuture = SettableFuture.create();
        executor.execute(new Runnable() {

          @Override
          public void run() {
            try {
              resultFuture.set(function.apply(input));
            } catch (Throwable t) {
              resultFuture.setException(t);
            }
          }
        });
        return resultFuture;
      }
    };
  }

  /**
   * Converts a {@link Function} into {@link AsyncFunction} by performing the operation in the same thread.
   *
   * @param function Function to apply
   * @param <I> Input type
   * @param <O> Output type
   * @return A {@link AsyncFunction} that will call the function in the same thread as the caller thread.
   */
  public static <I, O> AsyncFunction<I, O> asyncWrap(final Function<I, O> function) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) throws Exception {
        return Futures.immediateFuture(function.apply(input));
      }
    };
  }

  private AsyncFunctions() {
  }
}
