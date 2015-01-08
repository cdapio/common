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
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;

/**
 * Handler for basic authentication of users.
 */
public class BasicAuthenticationHandler extends AbstractAuthenticationHandler {
  private IdentityService identityService;

  @Inject
  public BasicAuthenticationHandler(SecurityConfiguration configuration) throws Exception {
    super(configuration);
  }

  @Override
  protected LoginService getHandlerLoginService() {
    String realmFile = configuration.get(Constants.BASIC_REALM_FILE);
    HashLoginService loginService = new HashLoginService();
    loginService.setConfig(realmFile);
    loginService.setIdentityService(getHandlerIdentityService());
    return loginService;
  }

  @Override
  protected Authenticator getHandlerAuthenticator() {
    return new BasicAuthenticator();
  }

  @Override
  protected IdentityService getHandlerIdentityService() {
    if (identityService == null) {
      identityService = new DefaultIdentityService();
    }
    return identityService;
  }

  @Override
  protected javax.security.auth.login.Configuration getLoginModuleConfiguration() {
    return null;
  }
}
