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

package co.cask.common.authorization.proxy;

import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.DefaultAuthorizationContext;
import co.cask.common.authorization.GroupId;
import co.cask.common.authorization.LocalACLManagerClient;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.RequiresPermissions;
import co.cask.common.authorization.TestPermissions;
import co.cask.common.authorization.TestStreamId;
import co.cask.common.authorization.UnauthorizedException;
import co.cask.common.authorization.UserId;
import co.cask.common.authorization.client.ACLManagerClient;
import co.cask.common.authorization.guice.AuthorizationRuntimeModule;
import co.cask.common.authorization.guice.DiscoveryRuntimeModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Base test for {@link AuthorizationProxyFactory} implementations.
 */
public abstract class AuthorizationProxyFactoryTest {

  private DefaultAuthorizationContext authorizationContext;
  private AuthorizationProxyFactory proxyFactory;
  private ACLManagerClient aclManagerClient;

  protected abstract Class<? extends AuthorizationProxyFactory> getProxyFactoryClass();

  @Before
  public void setUp() {
    authorizationContext = new DefaultAuthorizationContext();
    Injector injector = createInjector(authorizationContext);
    aclManagerClient = injector.getInstance(ACLManagerClient.class);
    proxyFactory = injector.getInstance(AuthorizationProxyFactory.class);
  }

  @Test
  public void testUserAuthorized() throws Exception {
    UserId bobUser = new UserId("bob");
    ObjectId secretEntity = new TestStreamId("secretEntity");
    authorizationContext.set(bobUser);

    aclManagerClient.setACL(secretEntity, bobUser, TestPermissions.ADMIN);
    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretEntity);
    testObject.doWithAdmin();
  }

  @Test
  public void testGroupAuthorized() throws Exception {
    UserId bobUser = new UserId("bob");
    GroupId someGroup = new GroupId("someGroup");
    List<GroupId> bobGroups = ImmutableList.of(new GroupId("someOtherGroup"), someGroup);

    ObjectId secretEntity = new TestStreamId("secretObject");
    authorizationContext.set(bobUser, bobGroups);

    aclManagerClient.setACL(secretEntity, someGroup, TestPermissions.ADMIN);
    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretEntity);
    testObject.doWithAdmin();
  }

  @Test
  public void testWrongUser() throws Exception {
    UserId bobUser = new UserId("bob");
    ObjectId secretEntity = new TestStreamId("secretObject");
    authorizationContext.set(UserId.ANON);

    aclManagerClient.setACL(secretEntity, bobUser, TestPermissions.ADMIN);
    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretEntity);
    try {
      testObject.doWithAdmin();
      Assert.fail("Expected UnauthorizedException");
    } catch (UnauthorizedException e) {
      // GOOD
    }
  }

  @Test
  public void testWrongPermission() throws Exception {
    UserId bobUser = new UserId("bob");
    ObjectId secretObject = new TestStreamId("secretObject");
    authorizationContext.set(bobUser);

    aclManagerClient.setACL(secretObject, bobUser, TestPermissions.WRITE);
    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretObject);
    try {
      testObject.doWithAdmin();
      Assert.fail("Expected UnauthorizedException");
    } catch (UnauthorizedException e) {
      // GOOD
    }
  }

  @Test
  public void testNoACL() throws Exception {
    UserId bobUser = new UserId("bob");
    ObjectId secretObject = new TestStreamId("secretObject");
    authorizationContext.set(bobUser, ImmutableList.of(new GroupId("someGroup")));

    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretObject);
    try {
      testObject.doWithAdmin();
      Assert.fail("Expected UnauthorizedException");
    } catch (UnauthorizedException e) {
      // GOOD
    }
  }

  private Injector createInjector(final AuthorizationContext context) {
    return Guice.createInjector(
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AuthorizationRuntimeModule().getInMemoryModules(),
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(AuthorizationProxyFactory.class).to(getProxyFactoryClass());
          bind(ACLManagerClient.class).to(LocalACLManagerClient.class);
          bind(AuthorizationContext.class).toInstance(context);
        }
      });
  }

  /**
   * Contains a {@link RequiresPermissions} annotated method for testing.
   */
  public static class TestObject {
    @RequiresPermissions({ TestPermissions.ADMIN })
    public void doWithAdmin() { }
  }

}
