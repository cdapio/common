/*
 * Copyright © 2012-2014 Cask Data, Inc.
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

package io.cdap.common.cli;

import io.cdap.common.cli.exception.CLIExceptionHandler;
import io.cdap.common.cli.exception.InvalidCommandException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryTest {

  @Test
  public void testRetry3Times() throws Exception {
    testRetry(3);
  }

  @Test
  public void testNoRetry() throws Exception {
    testRetry(0);
  }

  private void testRetry(final int timesToRetry) throws IOException, InvalidCommandException {
    final AtomicInteger timesActuallyTried = new AtomicInteger(0);

    CLI cli = new CLI<Command>(new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
        timesActuallyTried.incrementAndGet();
        throw new TestException();
      }

      @Override
      public String getPattern() {
        return "sdf";
      }

      @Override
      public String getDescription() {
        return "";
      }
    });

    cli.setExceptionHandler(new CLIExceptionHandler<Exception>() {
      @Override
      public boolean handleException(PrintStream output, Exception exception, int timesRetried) {
        return timesRetried != timesToRetry;
      }
    });

    cli.execute("sdf", System.out);
    Assert.assertEquals(timesToRetry, timesActuallyTried.get() - 1);
  }

  public static final class TestException extends Exception {

  }

}
