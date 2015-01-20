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

package co.cask.common.cli.exception;

import java.io.PrintStream;

/**
 * Exception handler for the {@link co.cask.common.cli.CLI}.
 *
 * @param <ExceptionType> type of exception to handle
 */
public interface CLIExceptionHandler<ExceptionType> {
  /**
   * Called by {@link co.cask.common.cli.CLI} when an exception is thrown while processing user input.
   *
   * @return true if the CLI command should be retried
   * @param output the {@link PrintStream} that the {@link co.cask.common.cli.CLI} is configured to output to
   * @param exception the exception that occurred
   * @param timesRetried number of times that the CLI command has already been retried
   */
  boolean handleException(PrintStream output, ExceptionType exception, int timesRetried);
}
