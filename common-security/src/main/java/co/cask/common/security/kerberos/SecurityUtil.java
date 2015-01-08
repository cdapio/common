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

package co.cask.common.security.kerberos;

import co.cask.common.security.Constants;
import co.cask.common.security.config.SecurityConfiguration;
import com.google.common.base.Preconditions;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.zookeeper.client.ZooKeeperSaslClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.security.auth.login.AppConfigurationEntry;

/**
 * Utility functions for Kerberos.
 */
public final class SecurityUtil {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityUtil.class);

  private SecurityUtil() { }

  /**
   * Enables Kerberos authentication based on configuration.
   *
   * @param conf configuration object.
   */
  public static void enableKerberosLogin(SecurityConfiguration conf) throws IOException {
    if (System.getProperty(Constants.External.JavaSecurity.ENV_AUTH_LOGIN_CONFIG) != null) {
      LOG.warn("Environment variable '{}' was already set to {}. Not generating JAAS configuration.",
               Constants.External.JavaSecurity.ENV_AUTH_LOGIN_CONFIG,
               System.getProperty(Constants.External.JavaSecurity.ENV_AUTH_LOGIN_CONFIG));
      return;
    }

    if (!isKerberosEnabled(conf)) {
      LOG.info("Kerberos login is not enabled. To enable Kerberos login, enable {} and configure {} and {}",
               Constants.KERBEROS_ENABLED, Constants.CFG_CDAP_MASTER_KRB_PRINCIPAL,
               Constants.CFG_CDAP_MASTER_KRB_KEYTAB_PATH);
      return;
    }

    Preconditions.checkArgument(conf.get(Constants.CFG_CDAP_MASTER_KRB_PRINCIPAL) != null,
                                "Kerberos authentication is enabled, but " +
                                Constants.CFG_CDAP_MASTER_KRB_PRINCIPAL + " is not configured");

    String principal = conf.get(Constants.CFG_CDAP_MASTER_KRB_PRINCIPAL);
    principal = SecurityUtil.expandPrincipal(principal);

    Preconditions.checkArgument(conf.get(Constants.CFG_CDAP_MASTER_KRB_KEYTAB_PATH) != null,
                                "Kerberos authentication is enabled, but " +
                                Constants.CFG_CDAP_MASTER_KRB_KEYTAB_PATH + " is not configured");

    File keyTabFile = new File(conf.get(Constants.CFG_CDAP_MASTER_KRB_KEYTAB_PATH));
    Preconditions.checkArgument(keyTabFile.exists(),
                                "Kerberos keytab file does not exist: " + keyTabFile.getAbsolutePath());
    Preconditions.checkArgument(keyTabFile.isFile(),
                                "Kerberos keytab file should be a file: " + keyTabFile.getAbsolutePath());
    Preconditions.checkArgument(keyTabFile.canRead(),
                                "Kerberos keytab file cannot be read: " + keyTabFile.getAbsolutePath());

    LOG.info("Using Kerberos principal {} and keytab {}", principal, keyTabFile.getAbsolutePath());

    System.setProperty(Constants.External.Zookeeper.ENV_AUTH_PROVIDER_1,
                       "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
    System.setProperty(Constants.External.Zookeeper.ENV_ALLOW_SASL_FAILED_CLIENTS, "true");
    System.setProperty(ZooKeeperSaslClient.LOGIN_CONTEXT_NAME_KEY, "Client");

    final Map<String, String> properties = new HashMap<String, String>();
    properties.put("doNotPrompt", "true");
    properties.put("useKeyTab", "true");
    properties.put("useTicketCache", "false");
    properties.put("doNotPrompt", "true");
    properties.put("principal", principal);
    properties.put("keyTab", keyTabFile.getAbsolutePath());

    final AppConfigurationEntry configurationEntry = new AppConfigurationEntry(
      KerberosUtil.getKrb5LoginModuleName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, properties);

    javax.security.auth.login.Configuration configuration = new javax.security.auth.login.Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String s) {
        return new AppConfigurationEntry[] { configurationEntry };
      }
    };

    // apply the configuration
    javax.security.auth.login.Configuration.setConfiguration(configuration);
  }

  /**
   * Expands _HOST in principal name with local hostname.
   *
   * @param principal Kerberos principal name
   * @return expanded principal name
   * @throws java.net.UnknownHostException if the local hostname could not be resolved into an address.
   */
  @Nullable
  public static String expandPrincipal(@Nullable String principal) throws UnknownHostException {
    if (principal == null) {
      return principal;
    }

    String localHostname = InetAddress.getLocalHost().getCanonicalHostName();
    return principal.replace("/_HOST@", "/" + localHostname + "@");
  }

  /**
   * @param cConf SecurityConfiguration object.
   * @return true, if Kerberos is enabled.
   */
  public static boolean isKerberosEnabled(SecurityConfiguration cConf) {
    return cConf.getBoolean(Constants.KERBEROS_ENABLED,
                            cConf.getBoolean(Constants.ENABLED));
  }
}
