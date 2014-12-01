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
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.client.AuthorizationClient;

import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;

/**
 * Provides proxies that require authorization for calls to methods annotated with
 * {@link co.cask.common.authorization.RequiresPermissions}.
 */
public abstract class AuthorizationProxyFactory {

  protected final AuthorizationClient authorizationClient;
  private final AuthorizationContext context;

  @Inject
  public AuthorizationProxyFactory(AuthorizationClient authorizationClient, AuthorizationContext context) {
    this.authorizationClient = authorizationClient;
    this.context = context;
  }

  /**
   * Returns a proxied instance of {@code object} that controls
   * access on calls to methods annotated with
   * {@link co.cask.common.authorization.RequiresPermissions}.
   *
   * @param instance instance to be proxied
   * @param objectId the ID of the object that the instance represents
   * @param <T> type of the instance
   * @return the proxied instance
   */
  public abstract <T> T getProxy(T instance, ObjectId objectId);

  protected void verifyAuthorized(ObjectId objectId, Set<String> requiredPermissions) throws IOException {
    authorizationClient.verifyAuthorized(objectId, context.getCurrentUser(), requiredPermissions);
  }
}
