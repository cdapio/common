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
package co.cask.common.authorization.client;

import co.cask.common.authorization.IdentifiableObject;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.UnauthorizedException;

/**
 * No-op implementation of {@link AuthorizationClient}.
 */
public class NoopAuthorizationClient implements AuthorizationClient {

  public void authorize(Iterable<? extends ObjectId> objects, Iterable<? extends SubjectId> subjects,
                        Iterable<String> requiredPermissions) throws UnauthorizedException {
    // NO-OP
  }

  /**
   * Verifies that at least one of the subjects has permission to access at least one of the objects.
   * <p/>
   * throws UnauthorizedException if no ACL entries exist for any of the object-subject
   * pairs for the specified permission
   */
  @Override
  public void authorize(ObjectId object, Iterable<? extends SubjectId> subjects,
                        Iterable<String> requiredPermissions) throws UnauthorizedException {
    // NO-OP
  }

  @Override
  public void authorizeCurrentUser(
    ObjectId object, Iterable<String> requiredPermissions) throws UnauthorizedException{
    // NO-OP
  }

  /**
   * @return true if at least one of the subjects has permission to access at least one of the objects.
   */
  @Override
  public boolean isAuthorized(Iterable<? extends ObjectId> objects, Iterable<? extends SubjectId> subjects,
                              Iterable<String> requiredPermissions) {
    return true;
  }

  /**
   * @return true if at least one of the subjects has permission to access at least one of the objects.
   */
  @Override
  public boolean isAuthorized(ObjectId object, Iterable<? extends SubjectId> subjects,
                              Iterable<String> requiredPermissions) {
    return true;
  }

  /**
   * Filters out objects that none of the subjectIds are allowed to access,
   * leaving only the objects that at least one of the subjectIds are allowed to access.
   *
   * Generally used for listing objects that a user has access to.
   */
  @Override
  public <O extends ObjectId> Iterable<IdentifiableObject<O>> filter(
    Iterable<IdentifiableObject<O>> objects,
    final Iterable<? extends SubjectId> subjects,
    final Iterable<String> requiredPermissions) {
    return objects;
  }
}
