/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.common.authorization.client;

import co.cask.common.authorization.server.AuthorizationService;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import org.apache.twill.discovery.Discoverable;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.apache.twill.discovery.ServiceDiscovered;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * {@link AuthorizationClient} that uses {@link DiscoveryServiceClient} to obtain the base URL.
 */
public class DiscoveringAuthorizationClient extends AuthorizationClient {

  private final Supplier<InetSocketAddress> addressSupplier;

  @Inject
  public DiscoveringAuthorizationClient(final DiscoveryServiceClient discoveryServiceClient) {
    this.addressSupplier = new Supplier<InetSocketAddress>() {
      @Override
      public InetSocketAddress get() {
        ServiceDiscovered serviceDiscovered = discoveryServiceClient.discover(AuthorizationService.DISCOVERABLE_NAME);
        Preconditions.checkState(serviceDiscovered.iterator().hasNext());
        Discoverable discoverable = serviceDiscovered.iterator().next();
        return discoverable.getSocketAddress();
      }
    };
  }

  @Override
  protected URL resolveURL(String path) throws MalformedURLException {
    InetSocketAddress address = addressSupplier.get();
    return URI.create("http://" + address.getHostName() + ":" + address.getPort()).resolve(path).toURL();
  }
}
