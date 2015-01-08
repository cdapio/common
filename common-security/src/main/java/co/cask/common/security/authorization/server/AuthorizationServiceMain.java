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
package co.cask.common.security.authorization.server;

import co.cask.common.security.authorization.guice.AuthorizationRuntimeModule;
import co.cask.common.security.authorization.guice.DiscoveryRuntimeModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.twill.common.Services;

import java.util.concurrent.ExecutionException;

/**
 * Runs {@link ACLManagerService} in main.
 */
public class AuthorizationServiceMain {
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    Injector injector = Guice.createInjector(
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AuthorizationRuntimeModule().getInMemoryModules()
    );

    ACLManagerService service = injector.getInstance(ACLManagerService.class);
    service.startAndWait();

    ListenableFuture<Service.State> completionFuture = Services.getCompletionFuture(service);
    completionFuture.get();
  }
}
