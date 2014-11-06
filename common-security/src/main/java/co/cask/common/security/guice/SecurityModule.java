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

package co.cask.common.security.guice;

import co.cask.common.io.Codec;
import co.cask.common.security.auth.AccessToken;
import co.cask.common.security.auth.AccessTokenCodec;
import co.cask.common.security.auth.AccessTokenIdentifier;
import co.cask.common.security.auth.AccessTokenIdentifierCodec;
import co.cask.common.security.auth.AccessTokenTransformer;
import co.cask.common.security.auth.AccessTokenValidator;
import co.cask.common.security.auth.KeyIdentifier;
import co.cask.common.security.auth.KeyIdentifierCodec;
import co.cask.common.security.auth.TokenManager;
import co.cask.common.security.auth.TokenValidator;
import co.cask.common.security.server.AuditLogHandler;
import co.cask.common.security.server.ExternalAuthenticationServer;
import co.cask.common.security.server.GrantAccessToken;
import co.cask.common.util.conf.CConfiguration;
import co.cask.common.util.conf.Constants;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.eclipse.jetty.server.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Guice bindings for co.cask.common.security related classes.  This extends {@code PrivateModule} in order to limit
 * which classes are exposed.
 */
public abstract class SecurityModule extends PrivateModule {

  private static final Logger EXTERNAL_AUTH_AUDIT_LOG = LoggerFactory.getLogger("external-auth-access");

  @Override
  protected final void configure() {
    bind(new TypeLiteral<Codec<AccessToken>>() { }).to(AccessTokenCodec.class).in(Scopes.SINGLETON);
    bind(new TypeLiteral<Codec<AccessTokenIdentifier>>() { }).to(AccessTokenIdentifierCodec.class).in(Scopes.SINGLETON);
    bind(new TypeLiteral<Codec<KeyIdentifier>>() { }).to(KeyIdentifierCodec.class).in(Scopes.SINGLETON);

    bindKeyManager(binder());
    bind(TokenManager.class).in(Scopes.SINGLETON);

    bind(ExternalAuthenticationServer.class).in(Scopes.SINGLETON);

    MapBinder<String, Object> handlerBinder =
      MapBinder.newMapBinder(binder(), String.class, Object.class, Names.named("co.cask.common.security.handlers.map"));

    handlerBinder.addBinding(ExternalAuthenticationServer.HandlerType.AUTHENTICATION_HANDLER)
                 .toProvider(AuthenticationHandlerProvider.class);
    handlerBinder.addBinding(ExternalAuthenticationServer.HandlerType.GRANT_TOKEN_HANDLER)
                 .to(GrantAccessToken.class);

    bind(AuditLogHandler.class)
      .annotatedWith(Names.named(ExternalAuthenticationServer.NAMED_EXTERNAL_AUTH))
      .toInstance(new AuditLogHandler(EXTERNAL_AUTH_AUDIT_LOG));

    bind(new TypeLiteral<Map<String, Object>>() { })
      .annotatedWith(Names.named("co.cask.common.security.handlers"))
      .toProvider(AuthenticationHandlerMapProvider.class)
      .in(Scopes.SINGLETON);

    bind(TokenValidator.class).to(AccessTokenValidator.class);
    bind(AccessTokenTransformer.class).in(Scopes.SINGLETON);
    expose(AccessTokenTransformer.class);
    expose(TokenValidator.class);
    expose(TokenManager.class);
    expose(ExternalAuthenticationServer.class);
    expose(new TypeLiteral<Codec<KeyIdentifier>>() { });
  }

  @Provides
  private Class<? extends Handler> provideHandlerClass(CConfiguration configuration) throws ClassNotFoundException {
    return configuration.getClass(Constants.Security.AUTH_HANDLER_CLASS, null, Handler.class);
  }

  private static final class AuthenticationHandlerProvider implements Provider<Handler> {

    private final Injector injector;
    private final Class<? extends Handler> handlerClass;

    @Inject
    private AuthenticationHandlerProvider(Injector injector, Class<? extends Handler> handlerClass) {
      this.injector = injector;
      this.handlerClass = handlerClass;
    }

    @Override
    public Handler get() {
      return injector.getInstance(handlerClass);
    }
  }

  private static final class AuthenticationHandlerMapProvider implements Provider<Map<String, Object>> {
    private final Map<String, Object> handlerMap;

    @Inject
    public AuthenticationHandlerMapProvider(@Named("co.cask.common.security.handlers.map")
                                                Map<String, Object> handlers) {
      handlerMap = new HashMap<String, Object>(handlers);
    }

    @Override
    public Map<String, Object> get() {
      return handlerMap;
    }
  }

  protected abstract void bindKeyManager(Binder binder);
}
