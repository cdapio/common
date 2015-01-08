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
import co.cask.common.security.Constants;
import co.cask.common.security.authentication.FileBasedKeyManager;
import co.cask.common.security.authentication.KeyIdentifier;
import co.cask.common.security.authentication.KeyManager;
import co.cask.common.security.config.SecurityConfiguration;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.junit.rules.TemporaryFolder;

/**
 * Guice module for testing FileBasedKeyManagers. Modifies functionality to write keys to a temporary folder.
 */
public class FileBasedSecurityTestModule extends SecurityModule {
  private static TemporaryFolder temporaryFolder;

  public FileBasedSecurityTestModule(TemporaryFolder temporaryFolder) {
    this.temporaryFolder = temporaryFolder;
  }

  @Override
  protected void bindKeyManager(Binder binder) {
    binder.bind(KeyManager.class).toProvider(FileBasedKeyManagerTestProvider.class);
  }

  private static final class FileBasedKeyManagerTestProvider implements Provider<KeyManager> {
    private SecurityConfiguration cConf;
    private Codec<KeyIdentifier> keyIdentifierCodec;

    @Inject
    FileBasedKeyManagerTestProvider(SecurityConfiguration cConf, Codec<KeyIdentifier> keyIdentifierCodec) {
      this.cConf = cConf;
      this.keyIdentifierCodec = keyIdentifierCodec;
    }

    @Override
    public KeyManager get() {
      // Set up the configuration to write the keyfile to a temporary folder.
      cConf.set(Constants.CFG_FILE_BASED_KEYFILE_PATH,
                temporaryFolder.getRoot().getAbsolutePath().concat("/keyfile"));

      return new FileBasedKeyManager(cConf, keyIdentifierCodec);
    }
  };
}
