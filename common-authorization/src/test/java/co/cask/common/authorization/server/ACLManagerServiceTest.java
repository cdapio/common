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

import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.DefaultAuthorizationContext;
import co.cask.common.authorization.NamespaceId;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.TestPermissions;
import co.cask.common.authorization.TestStreamId;
import co.cask.common.authorization.UserId;
import co.cask.common.authorization.client.ACLManagerClient;
import co.cask.common.authorization.guice.ACLManagerClientRuntimeModule;
import co.cask.common.authorization.guice.AuthorizationRuntimeModule;
import co.cask.common.authorization.guice.DiscoveryRuntimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

/**
 * Test for {@link ACLManagerService} and {@link ACLManagerClient}.
 */
public class ACLManagerServiceTest {

  private DefaultAuthorizationContext authorizationContext;
  private ACLManagerService aclManagerService;
  private ACLManagerClient aclManagerClient;

  @Before
  public void setUp() {
    authorizationContext = new DefaultAuthorizationContext();
    Injector injector = createInjector(authorizationContext);
    aclManagerService = injector.getInstance(ACLManagerService.class);
    aclManagerClient = injector.getInstance(ACLManagerClient.class);

    aclManagerService.startAndWait();
  }

  @After
  public void tearDown() {
    aclManagerService.stopAndWait();
  }

  @Test
  public void testSet() throws IOException {
    UserId currentUser = new UserId("bob");
    NamespaceId namespaceId = new NamespaceId("someNamespace");
    ObjectId objectId = new TestStreamId(namespaceId, "someStream");

    Set<ACLEntry> beforeAcls = aclManagerClient.getACLs(namespaceId, new ACLStore.Query(objectId, currentUser));
    Assert.assertEquals(0, beforeAcls.size());

    ACLEntry entry = new ACLEntry(objectId, currentUser, TestPermissions.WRITE);
    aclManagerClient.createACL(namespaceId, entry);

    Set<ACLEntry> afterAcls = aclManagerClient.getACLs(namespaceId, new ACLStore.Query(objectId, currentUser));
    Assert.assertEquals(1, afterAcls.size());
    ACLEntry afterAcl = afterAcls.iterator().next();
    Assert.assertEquals(currentUser, afterAcl.getSubject());
    Assert.assertEquals(objectId, afterAcl.getObject());
    Assert.assertEquals(TestPermissions.WRITE, afterAcl.getPermission());
  }

  @Test
  public void testDelete() throws IOException {
    UserId currentUser = new UserId("bob");
    authorizationContext.set(currentUser);
    NamespaceId namespaceId = new NamespaceId("someNamespace");
    ObjectId objectId = new TestStreamId(namespaceId, "someStream");

    ACLEntry entry = new ACLEntry(objectId, currentUser, TestPermissions.WRITE);
    aclManagerClient.createACL(namespaceId, entry);

    Set<ACLEntry> beforeAcls = aclManagerClient.getACLs(namespaceId, new ACLStore.Query(objectId, currentUser));
    Assert.assertEquals(1, beforeAcls.size());

    int numDeleted = aclManagerClient.deleteACLs(namespaceId, new ACLStore.Query(entry));
    Assert.assertEquals(1, numDeleted);

    Set<ACLEntry> afterAcls = aclManagerClient.getACLs(namespaceId, new ACLStore.Query(objectId, currentUser));
    Assert.assertEquals(0, afterAcls.size());
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
      new AuthorizationRuntimeModule().getInMemoryModules(),
      new ACLManagerClientRuntimeModule().getInMemoryModules()
    );
  }
}
