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

import co.cask.common.util.conf.CConfiguration;
import co.cask.common.util.conf.Constants;
import com.google.common.base.Throwables;
import com.google.inject.Inject;

import java.util.HashMap;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * An Authentication handler that authenticates against a LDAP server instance for External Authentication.
 */
public class LDAPAuthenticationHandler extends JAASAuthenticationHandler {
  private static final String[] mandatoryConfigurables = new String[] { "debug", "hostname", "port", "userBaseDn",
                                                                                "userRdnAttribute", "userObjectClass" };
  private static final String[] optionalConfigurables = new String[] { "bindDn", "bindPassword", "userIdAttribute",
                                                                      "userPasswordAttribute", "roleBaseDn",
                                                                      "roleNameAttribute", "roleMemberAttribute",
                                                                      "roleObjectClass" };

  /**
   * Create a new Authentication handler to use LDAP for external authentication.
   */
  @Inject
  public LDAPAuthenticationHandler(CConfiguration configuration) throws Exception {
    super(configuration);
  }

  /**
   * Create a configuration from properties. Allows optional configurables.
   */
  @Override
  protected Configuration getLoginModuleConfiguration() {
    return new Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String s) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("contextFactory", "com.sun.jndi.ldap.LdapCtxFactory");
        map.put("authenticationMethod", "simple");
        map.put("forceBindingLogin", "true");

        for (String configurable : mandatoryConfigurables) {
          String key = Constants.Security.AUTH_HANDLER_CONFIG_BASE.concat(configurable);
          String value = configuration.get(key);
          if (value == null) {
            String errorMessage = String.format("Mandatory configuration %s is not set.", key);
            throw Throwables.propagate(new RuntimeException(errorMessage));
          }
          map.put(configurable, value);
        }

        for (String configurable: optionalConfigurables) {
          String value = configuration.get(Constants.Security.AUTH_HANDLER_CONFIG_BASE.concat(configurable));
          if (value != null) {
            map.put(configurable, value);
          }
        }
        return new AppConfigurationEntry[] {
          new AppConfigurationEntry(configuration.get(Constants.Security.LOGIN_MODULE_CLASS_NAME),
                                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, map)
        };
      }
    };
  }
}
