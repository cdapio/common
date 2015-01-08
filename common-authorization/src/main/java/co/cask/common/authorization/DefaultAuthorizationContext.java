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
package co.cask.common.authorization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Simple implementation of {@link AuthorizationContext} with mutable fields.
 */
public class DefaultAuthorizationContext implements AuthorizationContext {

  private UserId currentUser;
  private List<GroupId> currentUsersGroups;

  public DefaultAuthorizationContext(UserId currentUser, List<GroupId> currentUsersGroups) {
    Preconditions.checkArgument(currentUser != null);
    Preconditions.checkArgument(currentUsersGroups != null);
    this.currentUser = currentUser;
    this.currentUsersGroups = currentUsersGroups;
  }

  public DefaultAuthorizationContext(UserId currentUser) {
    this(currentUser, ImmutableList.<GroupId>of());
  }

  public DefaultAuthorizationContext() {
    this(UserId.ANON, ImmutableList.<GroupId>of());
  }

  @Override
  public UserId getCurrentUser() {
    return currentUser;
  }

  @Override
  public List<GroupId> getCurrentUsersGroups() {
    return currentUsersGroups;
  }

  public void set(UserId currentUser, List<GroupId> currentUsersGroups) {
    Preconditions.checkArgument(currentUser != null);
    Preconditions.checkArgument(currentUsersGroups != null);
    this.currentUser = currentUser;
    this.currentUsersGroups = currentUsersGroups;
  }

  public void set(UserId currentUser) {
    Preconditions.checkArgument(currentUser != null);
    this.currentUser = currentUser;
    this.currentUsersGroups = ImmutableList.of();
  }
}
