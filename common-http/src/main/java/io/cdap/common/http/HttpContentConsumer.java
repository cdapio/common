/*
 * Copyright © 2021 Cask Data, Inc.
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

package io.cdap.common.http;

import java.nio.ByteBuffer;

/**
 * Consumer for {@link HttpResponse} body.
 */
public abstract class HttpContentConsumer {
  // Default 64K chunk size
  private static final int DEFAULT_CHUNK_SIZE = 65536;
  private int chunkSize;

  public HttpContentConsumer() {
    this.chunkSize = DEFAULT_CHUNK_SIZE;
  }

  public HttpContentConsumer(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  /**
   * This method is invoked when a new chunk of the response body is available to be consumed.
   *
   * @param chunk a {@link ByteBuffer} containing a chunk of the response body
   * @return true to continue reading from the response stream, false to stop reading and close the connection.
   */
  public abstract boolean onReceived(ByteBuffer chunk);

  /**
   * This method is invoked when the end of the response body is reached.
   */
  public abstract void onFinished();

  int getChunkSize() {
    return chunkSize;
  }
}
