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
package co.cask.common.authorization.guice;

import co.cask.common.authorization.client.ACLManagerClient;
import co.cask.common.authorization.client.DiscoveringBaseURISupplier;
import com.google.common.base.Supplier;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import java.net.URI;

/**
 * Provides Guice bindings for {@link co.cask.common.authorization.client.ACLManagerClient}.
 */
public final class ACLManagerClientRuntimeModule {

  public Module getInMemoryModules() {
    return new InMemoryAuthorizationModule();
  }

  public Module getDistributedModules() {
    return new InMemoryAuthorizationModule();
  }

  private static final class InMemoryAuthorizationModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(new TypeLiteral<Supplier<URI>>() { })
        .annotatedWith(Names.named(ACLManagerClient.NAME))
        .to(DiscoveringBaseURISupplier.class);
    }
  }
}
