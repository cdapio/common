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
package co.cask.common.security.authorization;

import co.cask.http.NettyHttpService;
import com.google.common.base.Function;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * Provides a {@link co.cask.http.NettyHttpService.Builder} that parses the access token header
 * and sets the user ID in {@link co.cask.common.security.authorization.SecurityRequestContext}.
 */
public class SecurityNettyHttpServiceBuilder extends NettyHttpService.Builder {
  public SecurityNettyHttpServiceBuilder() {
    super();
    this.modifyChannelPipeline(new Function<ChannelPipeline, ChannelPipeline>() {
      @Override
      public ChannelPipeline apply(ChannelPipeline input) {
        input.addAfter("decoder", "authenticator", new AuthenticationChannelHandler());
        return input;
      }
    });
  }
}
