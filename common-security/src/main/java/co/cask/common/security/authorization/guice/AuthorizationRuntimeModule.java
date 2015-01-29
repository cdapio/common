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
package co.cask.common.security.authorization.guice;

import co.cask.common.security.authorization.ACLStore;
import co.cask.common.security.authorization.InMemoryACLStore;
import co.cask.common.security.authorization.client.AuthorizationClient;
import co.cask.common.security.authorization.client.DefaultAuthorizationClient;
import co.cask.common.security.authorization.client.NoopAuthorizationClient;
import com.google.inject.AbstractModule;
import com.google.inject.Module;

/**
 * Provides Guice bindings for {@link co.cask.common.security.authorization.server.ACLManagerService}.
 */
public final class AuthorizationRuntimeModule {

  public Module getInMemoryModules() {
    return new InMemoryAuthorizationModule();
  }

  private static final class InMemoryAuthorizationModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(AuthorizationClient.class).to(DefaultAuthorizationClient.class);
      bind(ACLStore.class).toInstance(new InMemoryACLStore());
    }
  }

  private static final class NoopAuthorizationModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(AuthorizationClient.class).to(NoopAuthorizationClient.class);
    }
  }
}
