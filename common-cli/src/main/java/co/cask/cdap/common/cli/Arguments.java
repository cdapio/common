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

package co.cask.cdap.common.cli;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Convenience class to represent a set of arguments contained within user input.
 *
 * For example, if the user input is "create dataset test", and the argument
 * is "test" and is called "dataset-name", then the {@link #arguments} map
 * would contain "dataset-name" -> "test" and the {@link #rawInput}
 * would be "create dataset test".
 */
public class Arguments {

  private final Map<String, String> arguments;
  /**
   * The full raw input that the user entered. Useful for commands that
   * don't necessarily expect a single value per argument.
   */
  private final String rawInput;

  public Arguments(Map<String, String> arguments, String rawInput) {
    this.rawInput = rawInput;
    this.arguments = ImmutableMap.copyOf(arguments);
  }

  /**
   * @return the raw user input
   */
  public String getRawInput() {
    return rawInput;
  }

  /**
   * @param key the argument name
   * @return true if there is a value associated with the argument name
   */
  public boolean hasArgument(String key) {
    return arguments.containsKey(key);
  }

  /**
   * @param key the argument name
   * @return the argument value
   */
  public String get(String key) {
    checkRequiredArgument(key);
    return arguments.get(key);
  }

  /**
   * @param key the argument name
   * @param defaultValue the value to return if the argument is missing
   * @return the argument value
   */
  public String get(String key, String defaultValue) {
    String value = arguments.get(key);
    return Objects.firstNonNull(value, defaultValue);
  }

  /**
   * Gets an argument value as an integer.
   *
   * @param key the argument name
   * @return the argument value
   */
  public int getInt(String key) {
    checkRequiredArgument(key);
    return Integer.parseInt(arguments.get(key));
  }

  /**
   * Gets an argument value as an integer.
   *
   * @param key the argument name
   * @param defaultValue the value to return if the argument is missing
   * @return the argument value
   */
  public Integer getInt(String key, int defaultValue) {
    String value = arguments.get(key);
    if (value != null) {
      return Integer.parseInt(value);
    } else {
      return defaultValue;
    }
  }

  /**
   * Gets an argument value as a long.
   *
   * @param key the argument name
   * @return the argument value
   */
  public Long getLong(String key) {
    checkRequiredArgument(key);
    return Long.parseLong(arguments.get(key));
  }

  /**
   * Gets an argument value as a long.
   *
   * @param key the argument name
   * @param defaultValue the value to return if the argument is missing
   * @return the argument value
   */
  public Long getLong(String key, long defaultValue) {
    String value = arguments.get(key);
    if (value != null) {
      return Long.parseLong(value);
    } else {
      return defaultValue;
    }
  }

  private void checkRequiredArgument(String key) {
    if (!arguments.containsKey(key)) {
      throw new NoSuchElementException("Missing required argument: " + key);
    }
  }
}
