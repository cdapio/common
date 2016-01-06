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

package co.cask.common.http;

import co.cask.http.NettyHttpService;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.junit.Assert;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

/**
 * Service for testing {@link HttpRequests}.
 */
public final class TestHttpService extends AbstractIdleService {

  private final NettyHttpService httpService;
  private final AtomicInteger numConnectionsOpened = new AtomicInteger(0);

  public TestHttpService(boolean sslEnabled) throws URISyntaxException {
    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder
      .setHost("localhost")
      .addHttpHandlers(Sets.newHashSet(new HttpRequestsTestBase.TestHandler()))
      .modifyChannelPipeline(new Function<ChannelPipeline, ChannelPipeline>() {
        @Nullable
        @Override
        public ChannelPipeline apply(ChannelPipeline input) {
          input.addLast("connection-counter", new SimpleChannelHandler() {
            @Override
            public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
              numConnectionsOpened.incrementAndGet();
              super.channelOpen(ctx, e);
            }
          });
          return input;
        }
      })
      .setWorkerThreadPoolSize(10)
      .setExecThreadPoolSize(10)
      .setConnectionBacklog(20000);

    if (sslEnabled) {
      URL keystore = getClass().getClassLoader().getResource("cert.jks");
      Assert.assertNotNull(keystore);
      builder.enableSSL(new File(keystore.toURI()), "secret", "secret");
    }

    this.httpService = builder.build();
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
