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

import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.DefaultAuthorizationContext;
import co.cask.common.authorization.InMemoryACLStore;
import co.cask.common.authorization.InMemoryAuthorizationClient;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.RequiresPermissions;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.UnauthorizedException;
import co.cask.common.authorization.client.AuthorizationClient;
import co.cask.common.authorization.guice.DiscoveryRuntimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Base test for {@link AuthorizationProxyFactory} implementations.
 */
public abstract class AuthorizationProxyFactoryTest {

  private DefaultAuthorizationContext authorizationContext;
  private AuthorizationProxyFactory proxyFactory;
  private AuthorizationClient authorizationClient;

  protected abstract Class<? extends AuthorizationProxyFactory> getProxyFactoryClass();

  private enum TestObjectType {
    STREAM
  }

  @Before
  public void setUp() {
    authorizationContext = new DefaultAuthorizationContext(null);
    Injector injector = createInjector(authorizationContext);
    authorizationClient = injector.getInstance(AuthorizationClient.class);
    proxyFactory = injector.getInstance(AuthorizationProxyFactory.class);
  }

  @Test
  public void testAuthorized() throws Exception {
    SubjectId bobUser = SubjectId.ofUser("bob");
    ObjectId secretEntity = new ObjectId(TestObjectType.STREAM, "secretEntity");
    authorizationContext.setCurrentUser(bobUser);

    authorizationClient.setACL(secretEntity, bobUser, "ADMIN");
    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretEntity);
    testObject.doWithAdmin();
  }

  @Test
  public void testNoContext() throws Exception {
    SubjectId bobUser = SubjectId.ofUser("bob");
    ObjectId secretEntity = new ObjectId(TestObjectType.STREAM, "secretEntity");
    authorizationContext.setCurrentUser(null);

    authorizationClient.setACL(secretEntity, bobUser, "ADMIN");
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
    SubjectId bobUser = SubjectId.ofUser("bob");
    ObjectId secretEntity = new ObjectId(TestObjectType.STREAM, "secretEntity");
    authorizationContext.setCurrentUser(bobUser);

    authorizationClient.setACL(secretEntity, bobUser, "WRITE");
    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretEntity);
    try {
      testObject.doWithAdmin();
      Assert.fail("Expected UnauthorizedException");
    } catch (UnauthorizedException e) {
      // GOOD
    }
  }

  @Test
  public void testNoACL() throws Exception {
    SubjectId bobUser = SubjectId.ofUser("bob");
    ObjectId secretEntity = new ObjectId(TestObjectType.STREAM, "secretEntity");
    authorizationContext.setCurrentUser(bobUser);

    TestObject testObject = proxyFactory.getProxy(new TestObject(), secretEntity);
    try {
      testObject.doWithAdmin();
      Assert.fail("Expected UnauthorizedException");
    } catch (UnauthorizedException e) {
      // GOOD
    }
  }

  private Injector createInjector(final AuthorizationContext context) {
    final InMemoryAuthorizationClient inMemoryAuthClient = new InMemoryAuthorizationClient();
    return Guice.createInjector(
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(AuthorizationProxyFactory.class).to(getProxyFactoryClass());
          bind(ACLStore.class).to(InMemoryACLStore.class);
          bind(AuthorizationClient.class).toInstance(inMemoryAuthClient);
          bind(AuthorizationContext.class).toInstance(context);
        }
      });
  }

  /**
   * Contains a RequiresPermissions annotated method for testing.
   */
  public static class TestObject {
    @RequiresPermissions({ "ADMIN" })
    public void doWithAdmin() { }
  }

}
