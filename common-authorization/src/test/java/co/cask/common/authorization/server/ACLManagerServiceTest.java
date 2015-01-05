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
import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.CustomTypeManager;
import co.cask.common.authorization.DefaultAuthorizationContext;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.TestPermissions;
import co.cask.common.authorization.TestStreamId;
import co.cask.common.authorization.UserId;
import co.cask.common.authorization.client.ACLManagerClient;
import co.cask.common.authorization.guice.ACLManagerClientRuntimeModule;
import co.cask.common.authorization.guice.AuthorizationRuntimeModule;
import co.cask.common.authorization.guice.CGLibAuthorizationProxyRuntimeModule;
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
    ObjectId objectId = new TestStreamId("someStream");

    Set<ACLEntry> beforeAcls = aclManagerClient.getACLs(objectId, currentUser);
    Assert.assertEquals(0, beforeAcls.size());

    aclManagerClient.setACL(objectId, currentUser, TestPermissions.WRITE);

    Set<ACLEntry> afterAcls = aclManagerClient.getACLs(objectId, currentUser);
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
    ObjectId objectId = new TestStreamId("someStream");

    aclManagerClient.setACL(objectId, currentUser, TestPermissions.WRITE);

    Set<ACLEntry> beforeAcls = aclManagerClient.getACLs(objectId, currentUser);
    Assert.assertEquals(1, beforeAcls.size());

    aclManagerClient.deleteACL(objectId, currentUser, TestPermissions.WRITE);

    Set<ACLEntry> afterAcls = aclManagerClient.getACLs(objectId, currentUser);
    Assert.assertEquals(0, afterAcls.size());
  }

  private Injector createInjector(final AuthorizationContext context) {
    return Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(AuthorizationContext.class).toInstance(context);
          bind(CustomTypeManager.class).toInstance(new CustomTypeManager() {
            @Override
            public ObjectId fromObjectIdString(String objectId) {
              if (objectId.startsWith("stream:")) {
                return new TestStreamId(objectId.substring("stream:".length()));
              }

              throw new IllegalArgumentException("Unknown objectId: " + objectId);
            }

            @Override
            public SubjectId fromSubjectIdString(String subjectId) {
              if (subjectId.startsWith("user:")) {
                return new UserId(subjectId.substring("user:".length()));
              } else if (subjectId.startsWith("group:")) {
                return new UserId(subjectId.substring("group:".length()));
              }

              throw new IllegalArgumentException("Unknown subjectId: " + subjectId);
            }
          });
        }
      },
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AuthorizationRuntimeModule().getInMemoryModules(),
      new ACLManagerClientRuntimeModule().getInMemoryModules(),
      new CGLibAuthorizationProxyRuntimeModule()
    );
  }
}
