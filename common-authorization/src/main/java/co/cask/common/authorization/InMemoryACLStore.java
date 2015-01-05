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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Set;

/**
 * In-memory implementation of {@link ACLStore}.
 */
public class InMemoryACLStore implements ACLStore {

  // (ObjectId, SubjectId) -> set of Permission
  private final SetMultimap<Key, String> store = LinkedHashMultimap.create();

  @Override
  public boolean write(ACLEntry entry) {
    Key key = new Key(entry.getObject(), entry.getSubject());
    return store.put(key, entry.getPermission());
  }

  @Override
  public boolean exists(ACLEntry entry) {
    Key key = new Key(entry.getObject(), entry.getSubject());
    return store.containsKey(key);
  }

  @Override
  public Set<ACLEntry> read(ObjectId objectId, SubjectId subjectId) {
    Key key = new Key(objectId, subjectId);
    Set<String> permissions = store.get(key);

    ImmutableSet.Builder<ACLEntry> builder = ImmutableSet.builder();
    for (String permission : permissions) {
      builder.add(new ACLEntry(objectId, subjectId, permission));
    }
    return builder.build();
  }

  @Override
  public boolean delete(ACLEntry entry) {
    Key key = new Key(entry.getObject(), entry.getSubject());
    Set<String> permissions = store.get(key);
    return permissions.remove(entry.getPermission());
  }

  /**
   * (ObjectId, SubjectId)
   */
  private static class Key {
    private final ObjectId objectId;
    private final SubjectId subjectId;

    private Key(ObjectId objectId, SubjectId subjectId) {
      this.objectId = objectId;
      this.subjectId = subjectId;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(objectId, subjectId);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final Key other = (Key) obj;
      return Objects.equal(this.objectId, other.objectId)
        && Objects.equal(this.subjectId, other.subjectId);
    }
  }
}
