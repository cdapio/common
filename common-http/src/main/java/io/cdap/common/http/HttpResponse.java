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
package io.cdap.common.http;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nullable;

/**
 * Return type for http requests executed by {@link HttpResponse}
 */
public class HttpResponse {
  private static final Logger LOG = LoggerFactory.getLogger(HttpResponse.class);
  private final int responseCode;
  private final String responseMessage;
  private byte[] responseBody;
  private final Multimap<String, String> headers;
  private InputStream inputStream;
  private HttpURLConnection conn;
  private HttpContentConsumer consumer;

  HttpResponse(int responseCode, String responseMessage,
               byte[] responseBody, Map<String, List<String>> headers) {
    this(responseCode, responseMessage, responseBody, parseHeaders(headers));
  }

  HttpResponse(int responseCode, String responseMessage,
               byte[] responseBody, Multimap<String, String> headers) {
    this.responseCode = responseCode;
    this.responseMessage = responseMessage;
    this.responseBody = responseBody;
    this.headers = headers;
  }

  HttpResponse(HttpURLConnection conn) throws IOException {
    this(conn, null);
    this.responseBody = getResponseBodyFromStream();
  }

  HttpResponse(HttpURLConnection conn, @Nullable HttpContentConsumer consumer) throws IOException {
    this.conn = conn;
    this.responseCode = conn.getResponseCode();
    this.responseMessage = conn.getResponseMessage();
    this.headers = parseHeaders(conn.getHeaderFields());
    this.inputStream = isSuccessful(responseCode) ? conn.getInputStream() : conn.getErrorStream();
    this.consumer = consumer;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public byte[] getResponseBody() {
    return responseBody;
  }

  /**
   * Decodes and return the response body based on the content encoding.
   */
  public byte[] getUncompressedResponseBody() {
    String encoding = headers.entries().stream()
      .filter(e -> HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(e.getKey()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);

    if (encoding == null) {
      return responseBody;
    }

    try {
      if ("gzip".equalsIgnoreCase(encoding)) {
        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(responseBody))) {
          return ByteStreams.toByteArray(is);
        }
      }
      if ("deflate".equalsIgnoreCase(encoding)) {
        try (InputStream is = new DeflaterInputStream(new ByteArrayInputStream(responseBody))) {
          return ByteStreams.toByteArray(is);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Failed to decompress %s encoded response body", encoding), e);
    }

    throw new IllegalStateException("Unsupported content encoding " + encoding);
  }

  public String getResponseBodyAsString() {
    return getResponseBodyAsString(Charsets.UTF_8);
  }

  public String getResponseBodyAsString(Charset charset) {
    return new String(getUncompressedResponseBody(), charset);
  }

  public Multimap<String, String> getHeaders() {
    return headers;
  }

  public void consumeContent() throws IOException {
    if (inputStream == null) {
      conn.disconnect();
      consumer.onFinished();
      return;
    }

    try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
      ByteBuffer buffer = ByteBuffer.allocate(consumer.getChunkSize());
      while (channel.read(buffer) >= 0) {
        // Flip the buffer for the consumer to read
        buffer.flip();
        boolean continueReading = consumer.onReceived(buffer);
        buffer.clear();
        if (!continueReading) {
          break;
        }
      }
    } finally {
      conn.disconnect();
      consumer.onFinished();
    }
  }

  private static Multimap<String, String> parseHeaders(Map<String, List<String>> headers) {
    ImmutableListMultimap.Builder<String, String> builder = new ImmutableListMultimap.Builder<String, String>();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      // By default, headers created by URLConnection contain an entry from null -> HTTP Response message
      if (entry.getKey() != null) {
        builder.putAll(entry.getKey(), entry.getValue());
      }
    }
    return builder.build();
  }

  private byte[] getResponseBodyFromStream() {
    try {
      if (inputStream == null) {
        return new byte[0];
      }
      return ByteStreams.toByteArray(inputStream);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      closeQuietly(inputStream);
      inputStream = null;
      conn.disconnect();
    }
  }

  private void closeQuietly(@Nullable InputStream inputStream) {
    if (inputStream == null) {
      return;
    }
    try {
      inputStream.close();
    } catch (IOException e) {
      LOG.warn("Failed to close input stream", e);
    }
  }

  private boolean isSuccessful(int responseCode) {
    return 200 <= responseCode && responseCode < 300;
  }

  @Override
  public String toString() {
    return String.format("Response code: %s, message: '%s', body: '%s'",
                         this.getResponseCode(), this.getResponseMessage(),
                         this.getResponseBody() == null ? "null" : this.getResponseBodyAsString());
  }
}
