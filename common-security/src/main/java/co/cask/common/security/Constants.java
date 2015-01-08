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
package co.cask.common.security;

/**
 *
 */
public class Constants {

  /** Path to the Kerberos keytab file used by CDAP */
  public static final String CFG_CDAP_MASTER_KRB_KEYTAB_PATH = "cdap.master.kerberos.keytab";
  /** Kerberos principal used by CDAP */
  public static final String CFG_CDAP_MASTER_KRB_PRINCIPAL = "cdap.master.kerberos.principal";

  /**
   *
   */
  public static final class Service {
    public static final String ACL = "acl";
    public static final String APP_FABRIC_HTTP = "appfabric";
    public static final String TRANSACTION = "transaction";
    public static final String METRICS = "metrics";
    public static final String LOGSAVER = "log.saver";
    public static final String GATEWAY = "gateway";
    public static final String STREAMS = "streams";
    public static final String MASTER_SERVICES = "master.services";
    public static final String METRICS_PROCESSOR = "metrics.processor";
    public static final String DATASET_MANAGER = "dataset.service";
    public static final String DATASET_EXECUTOR = "dataset.executor";
    public static final String EXTERNAL_AUTHENTICATION = "external.authentication";
    public static final String EXPLORE_HTTP_USER_SERVICE = "explore.service";
    public static final String SERVICE_INSTANCE_TABLE_NAME = "cdap.services.instances";
  }

  /**
   * Monitor constants.
   */
  public static final class Monitor {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_NOTOK = "NOTOK";
    public static final String DISCOVERY_TIMEOUT_SECONDS = "monitor.handler.service.discovery.timeout.seconds";
  }

  /**
   * Constants for endpoints
   */
  public static final class EndPoints {
    /**
     * Status endpoint
     */
    public static final String STATUS = "/status";
  }

  /** Enables security. */
  public static final String ENABLED = "security.enabled";
  /** Enables authorization. */
  public static final String AUTHORIZATION_ENABLED = "security.authorization.enabled";
  /** Enables Kerberos authentication. */
  public static final String KERBEROS_ENABLED = "kerberos.auth.enabled";
  /** Algorithm used to generate the digest for access tokens. */
  public static final String TOKEN_DIGEST_ALGO = "security.token.digest.algorithm";
  /** Key length for secret key used by token digest algorithm. */
  public static final String TOKEN_DIGEST_KEY_LENGTH = "security.token.digest.keylength";
  /** Time duration in milliseconds after which an active secret key should be retired. */
  public static final String TOKEN_DIGEST_KEY_EXPIRATION = "security.token.digest.key.expiration.ms";
  /** Parent znode used for secret key distribution in ZooKeeper. */
  public static final String DIST_KEY_PARENT_ZNODE = "security.token.distributed.parent.znode";
  /** TODO: Config param is deprecated, AUTH_SERVER_BIND_ADDRESS should be used instead. */
  public static final String AUTH_SERVER_ADDRESS = "security.auth.server.address";
  /** Address the Authentication Server should bind to. */
  public static final String AUTH_SERVER_BIND_ADDRESS = "security.auth.server.bind.address";
  /** SecurityConfiguration for External Authentication Server. */
  public static final String AUTH_SERVER_BIND_PORT = "security.auth.server.bind.port";
  /** Maximum number of handler threads for the Authentication Server embedded Jetty instance. */
  public static final String MAX_THREADS = "security.server.maxthreads";
  /** Access token expiration time in milliseconds. */
  public static final String TOKEN_EXPIRATION = "security.server.token.expiration.ms";
  /** Long lasting Access token expiration time in milliseconds. */
  public static final String EXTENDED_TOKEN_EXPIRATION = "security.server.extended.token.expiration.ms";
  public static final String CFG_FILE_BASED_KEYFILE_PATH = "security.data.keyfile.path";
  /** SecurityConfiguration for security realm. */
  public static final String CFG_REALM = "security.realm";
  /** Authentication Handler class name */
  public static final String AUTH_HANDLER_CLASS = "security.authentication.handlerClassName";
  /** Prefix for all configurable properties of an Authentication handler. */
  public static final String AUTH_HANDLER_CONFIG_BASE = "security.authentication.handler.";
  /** Authentication Login Module class name */
  public static final String LOGIN_MODULE_CLASS_NAME = "security.authentication.loginmodule.className";
  /** Realm file for Basic Authentication */
  public static final String BASIC_REALM_FILE = "security.authentication.basic.realmfile";
  /** Enables SSL */
  public static final String SSL_ENABLED = "ssl.enabled";

  /**
   * Headers for security.
   */
  public static final class Headers {
    /** Internal user ID header passed from Router to downstream services */
    public static final String USER_ID = "CDAP-UserId";
  }

  /**
   * Security configuration for Router.
   */
  public static final class Router {
    /** SSL keystore location */
    public static final String SSL_KEYSTORE_PATH = "router.ssl.keystore.path";
    /** SSL keystore type */
    public static final String SSL_KEYSTORE_TYPE = "router.ssl.keystore.type";
    /** SSL keystore key password */
    public static final String SSL_KEYPASSWORD = "router.ssl.keystore.keypassword";
    /** SSL keystore password */
    public static final String SSL_KEYSTORE_PASSWORD = "router.ssl.keystore.password";

    /** Default SSL keystore type */
    public static final String DEFAULT_SSL_KEYSTORE_TYPE = "JKS";
  }

  /**
   * Security configuration for ExternalAuthenticationServer.
   */
  public static final class AuthenticationServer {
    /** SSL port */
    public static final String SSL_PORT = "security.auth.server.ssl.bind.port";
    /** SSL keystore location */
    public static final String SSL_KEYSTORE_PATH = "security.auth.server.ssl.keystore.path";
    /** SSL keystore type */
    public static final String SSL_KEYSTORE_TYPE = "security.auth.server.ssl.keystore.type";
    /** SSL keystore key password */
    public static final String SSL_KEYPASSWORD = "security.auth.server.ssl.keystore.keypassword";
    /** SSL keystore password */
    public static final String SSL_KEYSTORE_PASSWORD = "security.auth.server.ssl.keystore.password";

    /** Default SSL keystore type */
    public static final String DEFAULT_SSL_KEYSTORE_TYPE = "JKS";
  }

  /**
   * Constants related to external systems.
   */
  public static final class External {
    /**
     * Constants used by Java security.
     */
    public static final class JavaSecurity {
      public static final String ENV_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";
    }

    /**
     * Constants used by Zookeeper.
     */
    public static final class Zookeeper {
      public static final String ENV_AUTH_PROVIDER_1 = "zookeeper.authProvider.1";
      public static final String ENV_ALLOW_SASL_FAILED_CLIENTS = "zookeeper.allowSaslFailedClients";
    }
  }

  /**
   * Zookeeper SecurityConfiguration.
   */
  public static final class Zookeeper {
    public static final String QUORUM = "zookeeper.quorum";
    public static final String CFG_SESSION_TIMEOUT_MILLIS = "zookeeper.session.timeout.millis";
    public static final int DEFAULT_SESSION_TIMEOUT_MILLIS = 40000;
  }
}
