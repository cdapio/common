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
import co.cask.common.authorization.RequiresPermissions;
import co.cask.common.authorization.client.AuthorizationClient;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;


/**
 * Provides proxied objects via cglib proxies.
 */
public class CGLibAuthorizationProxyFactory extends AuthorizationProxyFactory {

  @Inject
  public CGLibAuthorizationProxyFactory(AuthorizationClient authorizationClient, AuthorizationContext context) {
    super(authorizationClient, context);
  }

  @Override
  @SuppressWarnings("unchecked cast")
  public <T> T getProxy(final T object, final ObjectId objectId) {
    return (T) Enhancer.create(object.getClass(),
                               new MethodInterceptor() {
      @Override
      public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        RequiresPermissions requiresPermissions = getAnnotation(method.getDeclaredAnnotations());
        if (requiresPermissions != null) {
          Set<String> requiredPermissions = ImmutableSet.copyOf(requiresPermissions.value());
          verifyAuthorized(objectId, requiredPermissions);
        }
        return method.invoke(object, args);
      }
    });
  }

  private RequiresPermissions getAnnotation(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation instanceof RequiresPermissions) {
        return (RequiresPermissions) annotation;
      }
    }
    return null;
  }
}
