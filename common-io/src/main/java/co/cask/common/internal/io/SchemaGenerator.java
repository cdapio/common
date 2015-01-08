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

package co.cask.common.internal.io;

import java.lang.reflect.Type;

/**
 * Interface for generating data {@link Schema}.
 */
public interface SchemaGenerator {

  /**
   * Generate a {@link Schema} for the given java {@link java.lang.reflect.Type}.
   *
   * @param type The java {@link java.lang.reflect.Type} for generating a {@link Schema}.
   * @return A {@link Schema} representing the given java {@link java.lang.reflect.Type}.
   * @throws UnsupportedTypeException Indicates schema generation is not supported for the given java
   * {@link java.lang.reflect.Type}.
   */
  Schema generate(Type type) throws UnsupportedTypeException;

  /**
   * Generate a {@link Schema} for the given java {@link java.lang.reflect.Type}.
   *
   * @param type The java {@link java.lang.reflect.Type} for generating a {@link Schema}.
   * @param acceptRecursiveTypes Whether to tolerate type recursion. If false, will throw UnsupportedTypeException if
   *                             a recursive type is encountered.
   * @return A {@link Schema} representing the given java {@link java.lang.reflect.Type}.
   * @throws UnsupportedTypeException Indicates schema generation is not supported for the given java
   * {@link java.lang.reflect.Type}.
   */
  Schema generate(Type type, boolean acceptRecursiveTypes) throws UnsupportedTypeException;
}
