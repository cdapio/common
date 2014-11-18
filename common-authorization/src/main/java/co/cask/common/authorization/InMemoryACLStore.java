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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory implementation of {@link ACLStore}.
 */
public class InMemoryACLStore implements ACLStore {

  // (ObjectId, SubjectId) -> Permission
  private final Map<Key, Set<String>> store = Maps.newHashMap();

  @Override
  public boolean write(ACLEntry entry) {
    Key key = new Key(entry.getObject(), entry.getSubject());
    Set<String> value = store.get(key);
    if (value == null) {
      value = Sets.newHashSet();
      store.put(key, value);
    }
    return value.add(entry.getPermission());
  }

  @Override
  public boolean exists(ACLEntry entry) {
    Key key = new Key(entry.getObject(), entry.getSubject());
    return store.containsKey(key);
  }

  @Override
  public List<ACLEntry> read(ObjectId objectId, SubjectId subjectId) {
    Key key = new Key(objectId, subjectId);

    ImmutableList.Builder<ACLEntry> builder = ImmutableList.builder();
    for (String permission : store.get(key)) {
      builder.add(new ACLEntry(objectId, subjectId, permission));
    }
    return builder.build();
  }

  private static class Key {
    private final ObjectId objectId;
    private final SubjectId subjectId;

    private Key(ObjectId objectId, SubjectId subjectId) {
      this.objectId = objectId;
      this.subjectId = subjectId;
    }

    public ObjectId getObjectId() {
      return objectId;
    }

    public SubjectId getSubjectId() {
      return subjectId;
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
