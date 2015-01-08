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

package co.cask.common.security.auth;

import co.cask.common.ImmutablePair;
import co.cask.common.io.Codec;
import co.cask.common.security.Constants;
import co.cask.common.security.config.SecurityConfiguration;
import co.cask.common.security.guice.ConfigModule;
import co.cask.common.security.guice.DiscoveryRuntimeModule;
import co.cask.common.security.guice.IOModule;
import co.cask.common.security.guice.SecurityModules;
import co.cask.common.security.guice.ZKClientModule;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster;
import org.apache.twill.zookeeper.ZKClientService;
import org.apache.zookeeper.ZooDefs;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 * Tests covering the {@link DistributedKeyManager} implementation.
 */
public class DistributedKeyManagerTest extends TestTokenManager {
  private static final Logger LOG = LoggerFactory.getLogger(DistributedKeyManagerTest.class);
  private static MiniZooKeeperCluster zkCluster;
  private static Injector injector1;
  private static Injector injector2;

  @BeforeClass
  public static void setup() throws Exception {
    HBaseTestingUtility testUtil = new HBaseTestingUtility();
    zkCluster = testUtil.startMiniZKCluster();
    String zkConnectString = testUtil.getConfiguration().get(HConstants.ZOOKEEPER_QUORUM) + ":"
      + zkCluster.getClientPort();
    LOG.info("Running ZK cluster at " + zkConnectString);
    SecurityConfiguration cConf1 = SecurityConfiguration.create();
    cConf1.set(Constants.Zookeeper.QUORUM, zkConnectString);

    SecurityConfiguration cConf2 = SecurityConfiguration.create();
    cConf2.set(Constants.Zookeeper.QUORUM, zkConnectString);
    injector1 = Guice.createInjector(new ConfigModule(cConf1), new IOModule(),
                                     new SecurityModules().getDistributedModules(), new ZKClientModule(),
                                     new DiscoveryRuntimeModule().getDistributedModules());
    injector2 = Guice.createInjector(new ConfigModule(cConf2), new IOModule(),
                                     new SecurityModules().getDistributedModules(), new ZKClientModule(),
                                     new DiscoveryRuntimeModule().getDistributedModules());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    zkCluster.shutdown();
  }

  @Test
  public void testKeyDistribution() throws Exception {
    DistributedKeyManager manager1 = getKeyManager(injector1, true);
    DistributedKeyManager manager2 = getKeyManager(injector2, false);
    TimeUnit.MILLISECONDS.sleep(1000);

    TestingTokenManager tokenManager1 =
      new TestingTokenManager(manager1, injector1.getInstance(AccessTokenIdentifierCodec.class));
    TestingTokenManager tokenManager2 =
      new TestingTokenManager(manager2, injector2.getInstance(AccessTokenIdentifierCodec.class));
    tokenManager1.startAndWait();
    tokenManager2.startAndWait();

    long now = System.currentTimeMillis();
    AccessTokenIdentifier ident1 = new AccessTokenIdentifier("testuser", Lists.newArrayList("users", "admins"),
                                                             now, now + 60 * 60 * 1000);
    AccessToken token1 = tokenManager1.signIdentifier(ident1);
    // make sure the second token manager has the secret key required to validate the signature
    tokenManager2.waitForKey(tokenManager1.getCurrentKey().getKeyId(), 2000, TimeUnit.MILLISECONDS);
    tokenManager2.validateSecret(token1);

    tokenManager2.waitForCurrentKey(2000, TimeUnit.MILLISECONDS);
    AccessToken token2 = tokenManager2.signIdentifier(ident1);
    tokenManager1.validateSecret(token2);
    assertEquals(token1.getIdentifier().getUsername(), token2.getIdentifier().getUsername());
    assertEquals(token1.getIdentifier().getGroups(), token2.getIdentifier().getGroups());
    assertEquals(token1, token2);

    tokenManager1.stopAndWait();
    tokenManager2.stopAndWait();
  }

  @Test
  public void testGetACLs() throws Exception {
    SecurityConfiguration kerbConf = SecurityConfiguration.create();
    kerbConf.set(Constants.KERBEROS_ENABLED, "true");
    kerbConf.set(Constants.CFG_CDAP_MASTER_KRB_PRINCIPAL, "prinicpal@REALM.NET");
    kerbConf.set(Constants.CFG_CDAP_MASTER_KRB_KEYTAB_PATH, "/path/to/keytab");
    Assert.assertEquals(ZooDefs.Ids.CREATOR_ALL_ACL, DistributedKeyManager.getACLs(kerbConf));

    SecurityConfiguration noKerbConf = SecurityConfiguration.create();
    noKerbConf.unset(Constants.CFG_CDAP_MASTER_KRB_PRINCIPAL);
    Assert.assertEquals(ZooDefs.Ids.OPEN_ACL_UNSAFE, DistributedKeyManager.getACLs(noKerbConf));
  }

  @Override
  protected ImmutablePair<TokenManager, Codec<AccessToken>> getTokenManagerAndCodec() {
    try {
      DistributedKeyManager keyManager = getKeyManager(injector1, true);
      TokenManager tokenManager = new TokenManager(keyManager, injector1.getInstance(AccessTokenIdentifierCodec.class));
      tokenManager.startAndWait();
      return new ImmutablePair<TokenManager, Codec<AccessToken>>(tokenManager,
                                                                 injector1.getInstance(AccessTokenCodec.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private DistributedKeyManager getKeyManager(Injector injector, boolean expectLeader) throws Exception {
    ZKClientService zk = injector.getInstance(ZKClientService.class);
    zk.startAndWait();
    WaitableDistributedKeyManager keyManager =
      new WaitableDistributedKeyManager(injector.getInstance(SecurityConfiguration.class),
          injector.getInstance(Key.get(new TypeLiteral<Codec<KeyIdentifier>>() { })),
          zk);

    keyManager.startAndWait();
    if (expectLeader) {
      keyManager.waitForLeader(5000, TimeUnit.MILLISECONDS);
    }
    return keyManager;
  }

  private static class WaitableDistributedKeyManager extends DistributedKeyManager {
    public WaitableDistributedKeyManager(SecurityConfiguration conf, Codec<KeyIdentifier> codec, ZKClientService zk) {
      super(conf, codec, zk, Lists.newArrayList(ZooDefs.Ids.OPEN_ACL_UNSAFE));
    }

    public void waitForLeader(long duration, TimeUnit unit) throws InterruptedException, TimeoutException {
      Stopwatch timer = new Stopwatch().start();
      do {
        if (!leader.get()) {
          unit.sleep(duration / 10);
        }
      } while (!leader.get() && timer.elapsedTime(unit) < duration);
      if (!leader.get()) {
        throw new TimeoutException("Timed out waiting to become leader");
      }
    }

    public KeyIdentifier getCurrentKey() {
      return currentKey;
    }

    public boolean hasKey(int keyId) {
      return super.hasKey(keyId);
    }
  }

  private static class TestingTokenManager extends TokenManager {
    private TestingTokenManager(KeyManager keyManager, Codec<AccessTokenIdentifier> identifierCodec) {
      super(keyManager, identifierCodec);
    }

    public KeyIdentifier getCurrentKey() {
      if (keyManager instanceof WaitableDistributedKeyManager) {
        return ((WaitableDistributedKeyManager) keyManager).getCurrentKey();
      }
      return null;
    }

    public void waitForKey(int keyId, long duration, TimeUnit unit) throws InterruptedException, TimeoutException {
      if (keyManager instanceof WaitableDistributedKeyManager) {
        WaitableDistributedKeyManager waitKeyManager = (WaitableDistributedKeyManager) keyManager;
        Stopwatch timer = new Stopwatch().start();
        boolean hasKey = false;
        do {
          hasKey = waitKeyManager.hasKey(keyId);
          if (!hasKey) {
            unit.sleep(duration / 10);
          }
        } while (!hasKey && timer.elapsedTime(unit) < duration);
        if (!hasKey) {
          throw new TimeoutException("Timed out waiting for key " + keyId);
        }
      }
    }

    public void waitForCurrentKey(long duration, TimeUnit unit) throws InterruptedException, TimeoutException {
      if (keyManager instanceof WaitableDistributedKeyManager) {
        WaitableDistributedKeyManager waitKeyManager = (WaitableDistributedKeyManager) keyManager;
        Stopwatch timer = new Stopwatch().start();
        boolean hasKey = false;
        do {
          hasKey = waitKeyManager.getCurrentKey() != null;
          if (!hasKey) {
            unit.sleep(duration / 10);
          }
        } while (!hasKey && timer.elapsedTime(unit) < duration);
        if (!hasKey) {
          throw new TimeoutException("Timed out waiting for current key to be set");
        }
      }
    }
  }
}
