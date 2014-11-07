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

package co.cask.common.security.runtime;

import co.cask.common.security.guice.SecurityModules;
import co.cask.common.security.server.ExternalAuthenticationServer;
import co.cask.common.util.conf.CConfiguration;
import co.cask.common.util.conf.Constants;
import co.cask.common.util.guice.ConfigModule;
import co.cask.common.util.guice.DiscoveryRuntimeModule;
import co.cask.common.util.guice.IOModule;
import co.cask.common.util.guice.ZKClientModule;
import co.cask.common.util.kerberos.SecurityUtil;
import co.cask.common.util.runtime.DaemonMain;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.twill.common.Services;
import org.apache.twill.zookeeper.ZKClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for authenticating clients accessing CDAP.  When a client authenticates successfully, it is issued
 * an access token containing a verifiable representation of the client's identity.  Other CDAP services
 * (such as the router) can independently verify client identities based on the token contents.
 */
public class AuthenticationServerMain extends DaemonMain {
  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServerMain.class);
  private ZKClientService zkClientService;
  private ExternalAuthenticationServer authServer;
  private CConfiguration configuration;

  @Override
  public void init(String[] args) {
    Injector injector = Guice.createInjector(new ConfigModule(),
                                             new IOModule(),
                                             new SecurityModules().getDistributedModules(),
                                             new DiscoveryRuntimeModule().getDistributedModules(),
                                             new ZKClientModule());
    configuration = injector.getInstance(CConfiguration.class);

    if (configuration.getBoolean(Constants.Security.CFG_SECURITY_ENABLED)) {
      this.zkClientService = injector.getInstance(ZKClientService.class);
      this.authServer = injector.getInstance(ExternalAuthenticationServer.class);
    }
  }

  @Override
  public void start() {
    if (authServer != null) {
      try {
        LOG.info("Starting AuthenticationServer.");

        // Enable Kerberos login
        SecurityUtil.enableKerberosLogin(configuration);

        Services.chainStart(zkClientService, authServer);
      } catch (Exception e) {
        LOG.error("Got exception while starting authenticaion server", e);
      }
    } else {
      String warning = "AuthenticationServer not started since co.cask.common.security is disabled." +
                       " To enable security, set \"security.enabled\" = \"true\" in cdap-site.xml" +
                       " and edit the appropriate configuration.";
      LOG.warn(warning);
    }
  }

  @Override
  public void stop() {
    if (authServer != null) {
      LOG.info("Stopping AuthenticationServer.");
      Futures.getUnchecked(Services.chainStop(authServer, zkClientService));
    }
  }

  @Override
  public void destroy() {
  }

  public static void main(String[] args) throws Exception {
    new AuthenticationServerMain().doMain(args);
  }
}
