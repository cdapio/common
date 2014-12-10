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

package co.cask.common.http;

import co.cask.http.NettyHttpService;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.AbstractIdleService;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;

import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.only;

/**
 * Test for {@link HttpRequests} against HTTP.
 */
public class HttpRequestsTest extends HttpRequestsTestBase {

  private static TestHttpService httpService;

  @Before
  public void setUp() {
    httpService = new TestHttpService();
    httpService.startAndWait();
  }

  @After
  public void tearDown() {
    httpService.stopAndWait();
  }

  @Override
  protected URI getBaseURI() throws URISyntaxException {
    InetSocketAddress bindAddress = httpService.getBindAddress();
    return new URI("http://" + bindAddress.getHostName() + ":" + bindAddress.getPort());
  }

  @Test
  public void testChunk() throws Exception {
    // This test only works in Http, but not in Https, as apparently Java SSLSocket implementation
    // will throw away all pending bytes in the socket buffer when the ssl socket is closed.

    // Send a request with never ending chunk, so that server will send 400
    // and close connection upon seeing the request header
    URL url = getBaseURI().resolve("/api/testChunk").toURL();
    HttpRequest request = HttpRequest
      .post(url)
      .addHeader(HttpHeaders.Names.EXPECT, HttpHeaders.Values.CONTINUE)
      .withBody(new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() throws IOException {
          return new InputStream() {

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
              ByteBuffer encoded = Charsets.UTF_8.encode(Strings.repeat("0", len));
              encoded.get(b, off, len);
              return len;
            }

            @Override
            public int read() throws IOException {
              // This method never get called because the read(byte[], int, int) is overridden
              return Character.forDigit(0, 10);
            }
          };
        }
      })
      .build();

    HttpResponse response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, only(400), any(), any(), any());
  }

  @Override
  protected HttpRequestConfig getHttpRequestsConfig() {
    return HttpRequestConfig.DEFAULT;
  }

  public static final class TestHttpService extends AbstractIdleService {

    private final NettyHttpService httpService;

    public TestHttpService() {
      this.httpService = NettyHttpService.builder()
        .setHost("localhost")
        .addHttpHandlers(Sets.newHashSet(new HttpRequestsTestBase.TestHandler()))
        .setWorkerThreadPoolSize(10)
        .setExecThreadPoolSize(10)
        .setConnectionBacklog(20000)
        .build();
    }

    public InetSocketAddress getBindAddress() {
      return httpService.getBindAddress();
    }

    @Override
    protected void startUp() throws Exception {
      httpService.startAndWait();
    }

    @Override
    protected void shutDown() throws Exception {
      httpService.stopAndWait();
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
        .add("bindAddress", httpService.getBindAddress())
        .toString();
    }
  }
}
