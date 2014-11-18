/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.common.authorization;

import co.cask.common.authorization.client.AuthorizationClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * In-memory implementation of {@link AuthorizationClient}.
 */
public class InMemoryAuthorizationClient extends AuthorizationClient {

  private InMemoryACLStore aclStore = new InMemoryACLStore();

  @Override
  protected URL resolveURL(String path) throws MalformedURLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ACLEntry> getACLs(ObjectId objectId, SubjectId subjectId) {
    return aclStore.read(objectId, subjectId);
  }

  @Override
  public boolean setACL(ObjectId objectId, SubjectId subjectId, String permission) {
    return aclStore.write(new ACLEntry(objectId, subjectId, permission));
  }
}
