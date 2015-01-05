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

import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.IdentifiableObject;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.UnauthorizedException;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Set;
import javax.annotation.Nullable;

/**
 * Default implementation of {@link AuthorizationClient} which uses {@link ACLStore}.
 */
public class DefaultAuthorizationClient implements AuthorizationClient {

  private final ACLStore aclStore;
  private final AuthorizationContext context;

  /**
   * @param aclStore used to set and get {@link ACLEntry}s
   * @param context provides current user and its groups
   */
  @Inject
  public DefaultAuthorizationClient(ACLStore aclStore, AuthorizationContext context) {
    this.aclStore = aclStore;
    this.context = context;
  }

  @Override
  public void authorize(Iterable<? extends ObjectId> objects, Iterable<? extends SubjectId> subjects,
                        Iterable<String> requiredPermissions) throws UnauthorizedException {
    if (!isAuthorized(objects, subjects, requiredPermissions)) {
      throw new UnauthorizedException();
    }
  }

  @Override
  public void authorize(ObjectId object, Iterable<? extends SubjectId> subjects,
                        Iterable<String> requiredPermissions) throws UnauthorizedException {
    if (!isAuthorized(object, subjects, requiredPermissions)) {
      throw new UnauthorizedException();
    }
  }

  @Override
  public void authorizeCurrentUser(ObjectId object,
                                   Iterable<String> requiredPermissions) throws UnauthorizedException {

    Iterable<? extends SubjectId> subjects = Iterables.concat(context.getCurrentUsersGroups(),
                                                              ImmutableSet.of(context.getCurrentUser()));
    if (!isAuthorized(object, subjects, requiredPermissions)) {
      throw new UnauthorizedException();
    }
  }

  @Override
  public boolean isAuthorized(Iterable<? extends ObjectId> objects, Iterable<? extends SubjectId> subjects,
                              Iterable<String> requiredPermissions) {

    Set<String> remainingRequiredPermission = Sets.newHashSet(requiredPermissions);
    for (ObjectId object : objects) {
      for (SubjectId subject : subjects) {
        for (ACLEntry aclEntry : aclStore.read(object, subject)) {
          remainingRequiredPermission.remove(aclEntry.getPermission());
        }
      }
    }
    return remainingRequiredPermission.isEmpty();
  }

  @Override
  public boolean isAuthorized(ObjectId object, Iterable<? extends SubjectId> subjects,
                              Iterable<String> requiredPermissions) {

    Set<String> remainingRequiredPermission = Sets.newHashSet(requiredPermissions);
    for (SubjectId subject : subjects) {
      for (ACLEntry aclEntry : aclStore.read(object, subject)) {
        remainingRequiredPermission.remove(aclEntry.getPermission());
      }
    }
    return remainingRequiredPermission.isEmpty();
  }

  @Override
  public <O extends ObjectId> Iterable<IdentifiableObject<O>> filter(
    Iterable<IdentifiableObject<O>> objects,
    final Iterable<? extends SubjectId> subjects,
    final Iterable<String> requiredPermissions) {
    return Iterables.filter(objects, new Predicate<IdentifiableObject<O>>() {
      @Override
      public boolean apply(@Nullable IdentifiableObject<O> input) {
        return input != null && isAuthorized(input.getId(), subjects, requiredPermissions);
      }
    });
  }
}
