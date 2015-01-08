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
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;

import java.net.InetAddress;

/**
 * Tests for {@link ExternalAuthenticationServer}.
 */
public class ExternalAuthenticationServerTest extends ExternalAuthenticationServerTestBase {

  @BeforeClass
  public static void beforeClass() throws Exception {
    SecurityConfiguration cConf = SecurityConfiguration.create();
    cConf.set(Constants.SSL_ENABLED, "false");
    configuration = cConf;
    ldapListenerConfig = InMemoryListenerConfig.createLDAPConfig("LDAP", InetAddress.getByName("127.0.0.1"),
                                                                 ldapPort, null);
    setup();
  }

  @Override
  protected String getProtocol() {
    return "http";
  }

  @Override
  protected HttpClient getHTTPClient() throws Exception {
    return new DefaultHttpClient();
  }
}
