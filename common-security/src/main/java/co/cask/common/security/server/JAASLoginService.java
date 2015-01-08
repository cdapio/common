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
import org.eclipse.jetty.plus.jaas.callback.ObjectCallback;
import org.eclipse.jetty.plus.jaas.callback.RequestParameterCallback;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/* ---------------------------------------------------- */

/** JAASLoginService
 *
 * Creates a UserRealm suitable for use with JAAS
 */
public class JAASLoginService extends AbstractLifeCycle implements LoginService {
  private static final Logger LOG = Log.getLogger(JAASLoginService.class);

  public static String defaultRoleClassName = "org.eclipse.jetty.plus.jaas.JAASRole";
  public static String[] defaultRoleClassNames = {defaultRoleClassName};

  protected String[] roleClassNames = defaultRoleClassNames;
  protected String callbackHandlerClass;
  protected String realmName;
  protected String loginModuleName;
  protected JAASUserPrincipal defaultUser = new JAASUserPrincipal(null, null, null);
  protected IdentityService identityService;
  protected Configuration configuration;

    /* ---------------------------------------------------- */
  /**
   * Constructor.
   *
   */
  public JAASLoginService() {
  }


    /* ---------------------------------------------------- */
  /**
   * Constructor.
   *
   * @param name the name of the realm
   */
  public JAASLoginService(String name) {
    this();
    realmName = name;
    loginModuleName = name;
  }


    /* ---------------------------------------------------- */
  /**
   * Get the name of the realm.
   *
   * @return name or null if not set.
   */
  public String getName() {
    return realmName;
  }


    /* ---------------------------------------------------- */
  /**
   * Set the name of the realm
   *
   * @param name a <code>String</code> value
   */
  public void setName (String name) {
    realmName = name;
  }

    /* ------------------------------------------------------------ */
  /** Get the identityService.
   * @return the identityService
   */
  public IdentityService getIdentityService() {
    return identityService;
  }

    /* ------------------------------------------------------------ */
  /** Set the identityService.
   * @param identityService the identityService to set
   */
  public void setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
  }

    /* ------------------------------------------------------------ */
  /**
   * Set the name to use to index into the config
   * file of LoginModules.
   *
   * @param name a <code>String</code> value
   */
  public void setLoginModuleName (String name) {
    loginModuleName = name;
  }

  public void setSecurityConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /* ------------------------------------------------------------ */
  public void setCallbackHandlerClass (String classname) {
    callbackHandlerClass = classname;
  }

  /* ------------------------------------------------------------ */
  public void setRoleClassNames (String[] classnames) {
    ArrayList<String> tmp = new ArrayList<String>();

    if (classnames != null) {
      tmp.addAll(Arrays.asList(classnames));
    }

    if (!tmp.contains(defaultRoleClassName)) {
      tmp.add(defaultRoleClassName);
    }
    roleClassNames = tmp.toArray(new String[tmp.size()]);
  }

  /* ------------------------------------------------------------ */
  public String[] getRoleClassNames() {
    return roleClassNames;
  }

    /* ------------------------------------------------------------ */
  /**
   * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStart()
   */
  protected void doStart() throws Exception {
    if (identityService == null) {
      identityService = new DefaultIdentityService();
    }
    super.doStart();
  }

  /* ------------------------------------------------------------ */
  public UserIdentity login(final String username, final Object credentials) {
    try {
      CallbackHandler callbackHandler = null;


      if (callbackHandlerClass == null) {
        callbackHandler = new CallbackHandler() {
          public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
          {
            for (Callback callback: callbacks) {
              if (callback instanceof NameCallback) {
                ((NameCallback) callback).setName(username);
              } else if (callback instanceof PasswordCallback) {
                ((PasswordCallback) callback).setPassword((char[]) credentials.toString().toCharArray());
              } else if (callback instanceof ObjectCallback) {
                ((ObjectCallback) callback).setObject(credentials);
              } else if (callback instanceof RequestParameterCallback) {
                AbstractHttpConnection connection = AbstractHttpConnection.getCurrentConnection();
                Request request = (connection == null ? null : connection.getRequest());

                if (request != null) {
                  RequestParameterCallback rpc = (RequestParameterCallback) callback;
                  rpc.setParameterValues(Arrays.asList(request.getParameterValues(rpc.getParameterName())));
                }
              } else {
                throw new UnsupportedCallbackException(callback);
              }
            }
          }
        };
      } else {
        Class clazz = Loader.loadClass(getClass(), callbackHandlerClass);
        callbackHandler = (CallbackHandler) clazz.newInstance();
      }
      //set up the login context
      //TODO jaspi requires we provide the SecurityConfiguration parameter
      Subject subject = new Subject();

      LoginContext loginContext = new LoginContext(loginModuleName, subject, callbackHandler, configuration);

      loginContext.login();

      //login success
      JAASUserPrincipal userPrincipal = new JAASUserPrincipal(getUserName(callbackHandler), subject, loginContext);
      subject.getPrincipals().add(userPrincipal);

      return identityService.newUserIdentity(subject, userPrincipal, getGroups(subject));
    } catch (LoginException e) {
      LOG.debug(e);
    } catch (IOException e) {
      LOG.info(e.getMessage());
      LOG.debug(e);
    } catch (UnsupportedCallbackException e) {
      LOG.info(e.getMessage());
      LOG.debug(e);
    } catch (InstantiationException e) {
      LOG.info(e.getMessage());
      LOG.debug(e);
    } catch (IllegalAccessException e) {
      LOG.info(e.getMessage());
      LOG.debug(e);
    } catch (ClassNotFoundException e) {
      LOG.info(e.getMessage());
      LOG.debug(e);
    }
    return null;
  }

  /* ------------------------------------------------------------ */
  public boolean validate(UserIdentity user) {
    // TODO optionally check user is still valid
    return true;
  }

  /* ------------------------------------------------------------ */
  private String getUserName(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
    NameCallback nameCallback = new NameCallback("foo");
    callbackHandler.handle(new Callback[] {nameCallback});
    return nameCallback.getName();
  }

  /* ------------------------------------------------------------ */
  public void logout(UserIdentity user) {
    Set<JAASUserPrincipal> userPrincipals = user.getSubject().getPrincipals(JAASUserPrincipal.class);
    LoginContext loginContext = userPrincipals.iterator().next().getLoginContext();
    try {
      loginContext.logout();
    } catch (LoginException e) {
      LOG.warn(e);
    }
  }


  /* ------------------------------------------------------------ */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private String[] getGroups (Subject subject) {
    //get all the roles of the various types
    String[] roleClassNames = getRoleClassNames();
    Collection<String> groups = new LinkedHashSet<String>();
    try {
      for (String roleClassName : roleClassNames) {
        Class loadClass = Thread.currentThread().getContextClassLoader().loadClass(roleClassName);
        Set<Principal> rolesForType = subject.getPrincipals(loadClass);
        for (Principal principal : rolesForType) {
          groups.add(principal.getName());
        }
      }

      return groups.toArray(new String[groups.size()]);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

}
