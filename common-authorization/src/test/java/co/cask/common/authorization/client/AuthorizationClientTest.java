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

package co.cask.common.authorization.client;

import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.DefaultAuthorizationContext;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.TestPermissions;
import co.cask.common.authorization.TestStreamId;
import co.cask.common.authorization.UserId;
import co.cask.common.authorization.guice.AuthorizationRuntimeModule;
import co.cask.common.authorization.guice.CGLibAuthorizationProxyRuntimeModule;
import co.cask.common.authorization.guice.DiscoveryRuntimeModule;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Test for {@link co.cask.common.authorization.server.ACLManagerService}.
 */
public class AuthorizationClientTest {

  private DefaultAuthorizationContext authorizationContext;
  private AuthorizationClient authorizationClient;
  private ACLStore aclStore;

  @Before
  public void setUp() {
    authorizationContext = new DefaultAuthorizationContext();
    Injector injector = createInjector(authorizationContext);
    authorizationClient = injector.getInstance(AuthorizationClient.class);
    aclStore = injector.getInstance(ACLStore.class);
  }

  @Test
  public void testAuthorized() throws IOException {
    UserId currentUser = new UserId("bob");
    ObjectId objectId = new TestStreamId("someStream");

    aclStore.write(new ACLEntry(objectId, currentUser, TestPermissions.WRITE));
    authorizationClient.authorize(objectId, ImmutableSet.<SubjectId>of(currentUser),
                                  ImmutableSet.of(TestPermissions.WRITE));
  }

  @Test
  public void testCurrentUserAuthorized() throws IOException {
    UserId currentUser = new UserId("bob");
    authorizationContext.set(currentUser);
    ObjectId objectId = new TestStreamId("someStream");

    aclStore.write(new ACLEntry(objectId, currentUser, TestPermissions.WRITE));
    authorizationClient.authorizeCurrentUser(objectId, ImmutableSet.of(TestPermissions.WRITE));
  }

  private Injector createInjector(final AuthorizationContext context) {
    return Guice.createInjector(
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AuthorizationRuntimeModule().getInMemoryModules(),
      new CGLibAuthorizationProxyRuntimeModule(),
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(AuthorizationContext.class).toInstance(context);
        }
      }
    );
  }
}
