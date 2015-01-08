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

package co.cask.common.security.server;

import co.cask.common.security.Constants;
import co.cask.common.security.config.SecurityConfiguration;
import com.google.inject.Inject;
import org.apache.geronimo.components.jaspi.impl.ServerAuthConfigImpl;
import org.apache.geronimo.components.jaspi.impl.ServerAuthContextImpl;
import org.apache.geronimo.components.jaspi.model.AuthModuleType;
import org.apache.geronimo.components.jaspi.model.ServerAuthConfigType;
import org.apache.geronimo.components.jaspi.model.ServerAuthContextType;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.jaspi.JaspiAuthenticator;
import org.eclipse.jetty.security.jaspi.JaspiAuthenticatorFactory;
import org.eclipse.jetty.security.jaspi.ServletCallbackHandler;
import org.eclipse.jetty.security.jaspi.modules.BasicAuthModule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 * An Authentication handler that supports JASPI plugins for External Authentication.
 */
public class JASPIAuthenticationHandler extends AbstractAuthenticationHandler {
  private JAASLoginService loginService;
  private IdentityService identityService;

  /**
   * Create a new Authentication handler to interface with JASPI plugins.
   * @param configuration
   * @throws Exception
   */
  @Inject
  public JASPIAuthenticationHandler(SecurityConfiguration configuration) throws Exception {
    super(configuration);
  }

  @Override
  protected LoginService getHandlerLoginService() {
    if (loginService == null) {
      loginService = new JAASLoginService();
      loginService.setLoginModuleName("JASPI");
      loginService.setSecurityConfiguration(getLoginModuleConfiguration());
      loginService.setIdentityService(getHandlerIdentityService());
    }
    return loginService;
  }

  @Override
  protected Authenticator getHandlerAuthenticator() {
    JaspiAuthenticatorFactory jaspiAuthenticatorFactory = new JaspiAuthenticatorFactory();
    jaspiAuthenticatorFactory.setLoginService(getHandlerLoginService());

    HashMap<String, ServerAuthContext> serverAuthContextMap = new HashMap<String, ServerAuthContext>();
    ServletCallbackHandler callbackHandler = new ServletCallbackHandler(getHandlerLoginService());
    ServerAuthModule authModule = new BasicAuthModule(callbackHandler, "JAASRealm");
    serverAuthContextMap.put("authContextID", new ServerAuthContextImpl(Collections.singletonList(authModule)));

    ServerAuthContextType serverAuthContextType = new ServerAuthContextType("HTTP", "server *",
                                                                            "authContextID",
                                                                            new AuthModuleType<ServerAuthModule>());
    ServerAuthConfigType serverAuthConfigType = new ServerAuthConfigType(serverAuthContextType, true);
    ServerAuthConfig serverAuthConfig = new ServerAuthConfigImpl(serverAuthConfigType, serverAuthContextMap);
    JaspiAuthenticator jaspiAuthenticator = new JaspiAuthenticator(serverAuthConfig, null, callbackHandler,
                                                                   new Subject(), true, getHandlerIdentityService());
    return jaspiAuthenticator;
  }

  @Override
  protected IdentityService getHandlerIdentityService() {
    if (identityService == null) {
      identityService = new DefaultIdentityService();
    }
    return identityService;
  }

  /**
   * Dynamically load the configuration properties set by the user for a JASPI plugin.
   * @return SecurityConfiguration
   */
  protected javax.security.auth.login.Configuration getLoginModuleConfiguration() {
    return new javax.security.auth.login.Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String s) {
        HashMap<String, String> map = new HashMap<String, String>();


        String configRegex = Constants.AUTH_HANDLER_CONFIG_BASE.replace(".", "\\.").concat(".");
        HashMap<String, String> configurables = (HashMap<String, String>) configuration.getValByRegex(configRegex);

        Iterator it = configurables.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry pairs = (Map.Entry) it.next();
          String key = pairs.getKey().toString();
          String value = pairs.getValue().toString();
          map.put(key.substring(key.lastIndexOf('.') + 1).trim(), value);
        }

        return new AppConfigurationEntry[] {
          new AppConfigurationEntry(configuration.get(Constants.LOGIN_MODULE_CLASS_NAME),
                                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, map)
        };
      }
    };
  }
}
