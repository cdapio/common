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

import com.google.common.reflect.TypeToken;

/**
 * Factory for creating instance of {@link DatumReader}.
 */
public interface DatumReaderFactory {

  /**
   * Creates a {@link DatumReader} that can decode object of type {@code T}.
   * @param type The object type to decode.
   * @param schema Schema of the object to decode to.
   * @param <T> Type of the object.
   * @return A {@link DatumReader}.
   */
  <T> DatumReader<T> create(TypeToken<T> type, Schema schema);
}
