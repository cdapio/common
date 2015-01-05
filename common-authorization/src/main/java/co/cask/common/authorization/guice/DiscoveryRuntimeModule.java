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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.twill.discovery.DiscoveryService;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.apache.twill.discovery.InMemoryDiscoveryService;
import org.apache.twill.discovery.ZKDiscoveryService;
import org.apache.twill.zookeeper.ZKClient;

/**
 * Provides Guice bindings for DiscoveryService and DiscoveryServiceClient for different
 * runtime environments.
 */
public final class DiscoveryRuntimeModule {

  public Module getInMemoryModules() {
    return new InMemoryDiscoveryModule();
  }

  public Module getDistributedModules() {
    return new ZKDiscoveryModule();
  }

  private static final class InMemoryDiscoveryModule extends AbstractModule {

    // ensuring to be singleton across JVM
    private static final InMemoryDiscoveryService IN_MEMORY_DISCOVERY_SERVICE = new InMemoryDiscoveryService();

    @Override
    protected void configure() {
      InMemoryDiscoveryService discovery = IN_MEMORY_DISCOVERY_SERVICE;
      bind(DiscoveryService.class).toInstance(discovery);
      bind(DiscoveryServiceClient.class).toInstance(discovery);
    }
  }

  private static final class ZKDiscoveryModule extends PrivateModule {

    @Override
    protected void configure() {
      bind(DiscoveryService.class).to(ZKDiscoveryService.class);
      bind(DiscoveryServiceClient.class).to(ZKDiscoveryService.class);
      expose(DiscoveryService.class);
      expose(DiscoveryServiceClient.class);
    }

    @Provides
    @Singleton
    private ZKDiscoveryService providesDiscoveryService(ZKClient zkClient) {
      return new ZKDiscoveryService(zkClient);
    }
  }
}
