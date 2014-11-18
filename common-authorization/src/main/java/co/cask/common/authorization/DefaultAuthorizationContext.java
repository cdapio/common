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

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Simple implementation of {@link AuthorizationContext} with mutable fields.
 */
public class DefaultAuthorizationContext implements AuthorizationContext {

  private SubjectId currentUser;
  private List<SubjectId> currentUsersGroups;

  public DefaultAuthorizationContext(SubjectId currentUser, List<SubjectId> currentUsersGroups) {
    this.currentUser = currentUser;
    this.currentUsersGroups = currentUsersGroups;
  }

  public DefaultAuthorizationContext(SubjectId currentUser) {
    this(currentUser, ImmutableList.<SubjectId>of());
  }

  @Override
  public SubjectId getCurrentUser() {
    return currentUser;
  }

  @Override
  public List<SubjectId> getCurrentUsersGroups() {
    return currentUsersGroups;
  }

  public void setCurrentUser(SubjectId currentUser) {
    this.currentUser = currentUser;
  }

  public void setCurrentUsersGroups(List<SubjectId> currentUsersGroups) {
    this.currentUsersGroups = currentUsersGroups;
  }
}
