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

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

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
   * Returns the number of arguments in this {@code Arguments} object
   *
   * @return the number of arguments
   */
  public int size() {
    return arguments.size();
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
   *
   * @deprecated As of 0.8.0, use {@link #getOptional(String, String)}.
   */
  @Deprecated
  @Nullable
  public String get(String key, String defaultValue) {
    return getOptional(key, defaultValue);
  }

  /**
   * Gets an optional argument.
   *
   * @param key the argument name
   * @param defaultValue the value to return if the argument is missing
   * @return the argument value
   */
  @Nullable
  public String getOptional(String key, String defaultValue) {
    String value = arguments.get(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /**
   * Gets an optional argument.
   *
   * @param key the argument name
   * @return the argument value
   */
  @Nullable
  public String getOptional(String key) {
    return getOptional(key, null);
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
   *
   * @deprecated As of 0.8.0, use {@link #getIntOptional(String, Integer)}.
   */
  public Integer getInt(String key, int defaultValue) {
    return getIntOptional(key, defaultValue);
  }

  /**
   * Gets an optional argument as an int.
   *
   * @param key the argument name
   * @param defaultValue the value to return if the argument is missing
   * @return the argument value
   */
  @Nullable
  public Integer getIntOptional(String key, Integer defaultValue) {
    String value = arguments.get(key);
    if (value != null) {
      return Integer.parseInt(value);
    } else {
      return defaultValue;
    }
  }

  /**
   * Gets an optional argument as an int.
   *
   * @param key the argument name
   * @return the argument value
   */
  @Nullable
  public Integer getIntOptional(String key) {
    return getIntOptional(key, null);
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
   *
   * @deprecated As of 0.8.0, use {@link #getLongOptional(String, Long)}.
   */
  public Long getLong(String key, long defaultValue) {
    return getLongOptional(key, defaultValue);
  }

  /**
   * Gets an optional argument as a long.
   *
   * @param key the argument name
   * @param defaultValue the value to return if the argument is missing
   * @return the argument value
   */
  @Nullable
  public Long getLongOptional(String key, @Nullable Long defaultValue) {
    String value = arguments.get(key);
    if (value != null) {
      return Long.parseLong(value);
    } else {
      return defaultValue;
    }
  }

  /**
   * Gets an optional argument as a long.
   *
   * @param key the argument name
   * @return the argument value
   */
  @Nullable
  public Long getLongOptional(String key) {
    return getLongOptional(key, null);
  }

  private void checkRequiredArgument(String key) {
    if (!arguments.containsKey(key)) {
      throw new NoSuchElementException("Missing required argument: " + key);
    }
  }
}
