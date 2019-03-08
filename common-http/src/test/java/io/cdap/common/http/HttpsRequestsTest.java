/*
 * Copyright © 2014-2016 Cask Data, Inc.
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

import org.junit.After;
import org.junit.Before;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test for {@link HttpRequests} against HTTPS.
 */
public class HttpsRequestsTest extends HttpRequestsTestBase {

  private static TestHttpService httpsService;

  @Before
  public void setUp() throws Exception {
    httpsService = new TestHttpService(true);
    httpsService.startAndWait();
  }

  @After
  public void tearDown() {
    httpsService.stopAndWait();
  }

  @Override
  protected URI getBaseURI() throws URISyntaxException {
    InetSocketAddress bindAddress = httpsService.getBindAddress();
    return new URI("https://" + bindAddress.getHostName() + ":" + bindAddress.getPort());
  }

  @Override
  protected HttpRequestConfig getHttpRequestsConfig() {
    return new HttpRequestConfig(0, 0, false);
  }

  @Override
  protected int getNumConnectionsOpened() {
    return httpsService.getNumConnectionsOpened();
  }
}
