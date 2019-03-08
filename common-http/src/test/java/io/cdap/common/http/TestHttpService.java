/*
 * Copyright © 2016 Cask Data, Inc.
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

import com.google.common.base.Objects;
import com.google.common.util.concurrent.AbstractIdleService;
import io.cdap.http.ChannelPipelineModifier;
import io.cdap.http.NettyHttpService;
import io.cdap.http.SSLConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import org.junit.Assert;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for testing {@link HttpRequests}.
 */
public final class TestHttpService extends AbstractIdleService {

  private final NettyHttpService httpService;
  private final AtomicInteger numConnectionsOpened = new AtomicInteger(0);

  public TestHttpService(boolean sslEnabled) throws URISyntaxException {
    NettyHttpService.Builder builder = NettyHttpService.builder("test");
    builder
      .setHost("localhost")
      .setHttpHandlers(new HttpRequestsTestBase.TestHandler())
      .setChannelPipelineModifier(new ChannelPipelineModifier() {
        @Override
        public void modify(ChannelPipeline channelPipeline) {
          channelPipeline.addLast("connection-counter", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRegistered(ChannelHandlerContext ctx) {
              numConnectionsOpened.incrementAndGet();
              ctx.fireChannelRegistered();
            }
          });
        }
      })
      .setWorkerThreadPoolSize(10)
      .setExecThreadPoolSize(10)
      .setConnectionBacklog(20000);

    if (sslEnabled) {
      URL keystore = getClass().getClassLoader().getResource("cert.jks");
      Assert.assertNotNull(keystore);
      builder.enableSSL(SSLConfig.builder(new File(keystore.toURI()), "secret").build());
    }

    this.httpService = builder.build();
  }

  public InetSocketAddress getBindAddress() {
    return httpService.getBindAddress();
  }

  @Override
  protected void startUp() throws Exception {
    httpService.start();
  }

  @Override
  protected void shutDown() throws Exception {
    httpService.stop();
  }

  public int getNumConnectionsOpened() {
    return numConnectionsOpened.get();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("bindAddress", httpService.getBindAddress())
      .toString();
  }
}
