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

package co.cask.common.security.guice;

import co.cask.common.io.Codec;
import co.cask.common.security.auth.DistributedKeyManager;
import co.cask.common.security.auth.KeyIdentifier;
import co.cask.common.security.auth.KeyManager;
import co.cask.common.security.config.SecurityConfiguration;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.apache.twill.zookeeper.ZKClientService;

/**
 * Configures dependency injection with all security class implementations required to run in a distributed
 * environment.
 */
public class DistributedSecurityModule extends SecurityModule {
  @Override
  protected void bindKeyManager(Binder binder) {
    binder.bind(KeyManager.class).toProvider(DistributedKeyManagerProvider.class).in(Scopes.SINGLETON);
  }

  private static final class DistributedKeyManagerProvider implements Provider<KeyManager> {
    private final SecurityConfiguration cConf;
    private final Codec<KeyIdentifier> keyCodec;
    private final ZKClientService zkClient;


    @Inject
    DistributedKeyManagerProvider(SecurityConfiguration cConf, Codec<KeyIdentifier> codec, ZKClientService zkClient) {
      this.cConf = cConf;
      this.keyCodec = codec;
      this.zkClient = zkClient;
    }

    @Override
    public KeyManager get() {
      return new DistributedKeyManager(cConf, keyCodec, zkClient);
    }
  }
}
