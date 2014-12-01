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

package co.cask.common.authorization.server;

import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.DefaultAuthorizationContext;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.client.AuthorizationClient;
import co.cask.common.authorization.guice.AuthorizationRuntimeModule;
import co.cask.common.authorization.guice.DiscoveryRuntimeModule;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Test for {@link AuthorizationService}.
 */
public class AuthorizationServiceTest {

  private DefaultAuthorizationContext authorizationContext;
  private AuthorizationService authorizationService;
  private AuthorizationClient authorizationClient;

  @Before
  public void setUp() {
    authorizationContext = new DefaultAuthorizationContext(null);
    Injector injector = createInjector(authorizationContext);
    authorizationService = injector.getInstance(AuthorizationService.class);
    authorizationClient = injector.getInstance(AuthorizationClient.class);

    authorizationService.startAndWait();
  }

  @After
  public void tearDown() {
    authorizationService.stopAndWait();
  }

  @Test
  public void testAuthorized() throws IOException {
    SubjectId currentUser = SubjectId.ofUser("bob");
    authorizationContext.setCurrentUser(currentUser);

    ObjectId objectId = new ObjectId("STREAM", "someStream");
    authorizationClient.setACL(objectId, currentUser, "WRITE");

    authorizationClient.verifyAuthorized(objectId, currentUser, ImmutableSet.of("WRITE"));
  }

  private Injector createInjector(final AuthorizationContext context) {
    return Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(AuthorizationContext.class).toInstance(context);
        }
      },
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AuthorizationRuntimeModule().getInMemoryModules()
    );
  }
}
