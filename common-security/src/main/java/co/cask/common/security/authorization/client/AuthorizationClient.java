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
package co.cask.common.security.authorization.client;

import co.cask.common.security.authorization.IdentifiableObject;
import co.cask.common.security.authorization.ObjectId;
import co.cask.common.security.authorization.SubjectId;
import co.cask.common.security.authorization.UnauthorizedException;

/**
 * Provides ways to verify access to protected objects.
 */
public interface AuthorizationClient {

  /**
   * Verifies that at least one of the subjects has access to at least one of the objects
   * for all of the required permissions.
   *
   * @param objects objects to consider
   * @param subjects subjects to consider
   * @param requiredPermissions permissions that are required
   * @throws UnauthorizedException if none of the subjects have access to the object
   *                               for all of the required permissions
   */
  public void authorize(Iterable<? extends ObjectId> objects, Iterable<? extends SubjectId> subjects,
                        Iterable<String> requiredPermissions) throws UnauthorizedException;

  /**
   * Verifies that at least one of the subjects has access to the object for all of the required permissions.
   *
   * @param object object to consider
   * @param subjects subjects to consider
   * @param requiredPermissions permissions that are required
   * @throws UnauthorizedException if none of the subjects have access to the object
   *                               for all of the required permissions
   */
  public void authorize(ObjectId object, Iterable<? extends SubjectId> subjects,
                        Iterable<String> requiredPermissions) throws UnauthorizedException;

  /**
   * Verifies that the current user or its groups has access to the object for all of the required permissions.
   *
   * @param object object to consider
   * @param requiredPermissions permissions that are required
   * @throws UnauthorizedException if none of the current user or its groups have access to the object
   *                               for all of the required permissions
   */
  public void authorizeCurrentUser(
    ObjectId object, Iterable<String> requiredPermissions) throws UnauthorizedException;

  /**
   * Indicates whether at least one of the subjects is allowed access to at least one of the objects
   * for all of the required permissions.
   *
   * @param objects objects to consider
   * @param subjects subjects to consider
   * @param requiredPermissions permissions that are required
   * @return true if at least one of the subjects have access to at least one of the objects
   *         for all of the required permissions
   */
  public boolean isAuthorized(Iterable<? extends ObjectId> objects, Iterable<? extends SubjectId> subjects,
                              Iterable<String> requiredPermissions);

  /**
   * Indicates whether at least one of the subjects is allowed access to the object for all of the required permissions.
   *
   * @param object object to consider
   * @param subjects subjects to consider
   * @param requiredPermissions permissions that are required
   * @return true if at least one of the subjects have access to the object for all of the required permissions
   */
  public boolean isAuthorized(ObjectId object, Iterable<? extends SubjectId> subjects,
                              Iterable<String> requiredPermissions);

  /**
   * Filters out objects that none of the subjectIds are allowed to access,
   * leaving only the objects that at least one of the subjectIds are allowed to access.
   *
   * Generally used for listing objects that a user has access to.
   */
  public <O extends ObjectId> Iterable<IdentifiableObject<O>> filter(
    Iterable<IdentifiableObject<O>> objects,
    final Iterable<? extends SubjectId> subjects,
    final Iterable<String> requiredPermissions);
}
