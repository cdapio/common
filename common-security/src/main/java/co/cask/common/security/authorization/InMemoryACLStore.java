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
package co.cask.common.security.authorization;

import com.google.common.collect.Sets;

import java.util.Iterator;
import java.util.Set;

/**
 * In-memory implementation of {@link ACLStore}.
 */
public class InMemoryACLStore implements ACLStore {

  private Set<ACLEntry> store = Sets.newHashSet();

  @Override
  public boolean write(ACLEntry entry) {
    return store.add(entry);
  }

  @Override
  public boolean exists(ACLEntry entry) {
    return store.contains(entry);
  }

  @Override
  public boolean delete(ACLEntry entry) {
    return store.remove(entry);
  }

  @Override
  public Set<ACLEntry> read(Query query) {
    Set<ACLEntry> result = Sets.newHashSet();

    for (ACLEntry aclEntry : store) {
      if (query.getObjectId() != null && !query.getObjectId().equals(aclEntry.getObject())) {
        break;
      }

      if (query.getSubjectId() != null && !query.getSubjectId().equals(aclEntry.getSubject())) {
        break;
      }

      if (query.getPermission() != null && !query.getPermission().equals(aclEntry.getPermission())) {
        break;
      }

      result.add(aclEntry);
    }

    return result;
  }

  @Override
  public int delete(Query query) {
    int numDeleted = 0;

    Iterator<ACLEntry> iterator = store.iterator();
    while (iterator.hasNext()) {
      ACLEntry aclEntry = iterator.next();

      if (query.getObjectId() != null && !query.getObjectId().equals(aclEntry.getObject())) {
        break;
      }

      if (query.getSubjectId() != null && !query.getSubjectId().equals(aclEntry.getSubject())) {
        break;
      }

      if (query.getPermission() != null && !query.getPermission().equals(aclEntry.getPermission())) {
        break;
      }

      iterator.remove();
      numDeleted++;
    }

    return numDeleted;
  }
}
