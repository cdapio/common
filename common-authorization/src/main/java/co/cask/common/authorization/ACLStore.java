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

import java.util.Set;

/**
 * Provides methods for storing and querying {@link ACLEntry}s.
 */
public interface ACLStore {

  /**
   * Writes a single {@link ACLEntry}.
   *
   * @param entry the {@link ACLEntry} to write
   * @return true if entry did not previously exist
   */
  boolean write(ACLEntry entry);

  /**
   * Checks for the existence of an {@link ACLEntry}.
   *
   * @param entry the {@link ACLEntry} to check
   * @return true if the {@link ACLEntry} exists
   */
  boolean exists(ACLEntry entry);

  /**
   * Deletes an {@link ACLEntry} matching.
   *
   * @param entry the {@link ACLEntry} to delete
   * @return true if the {@link ACLEntry} previously existed
   */
  boolean delete(ACLEntry entry);

  /**
   * Fetches {@link ACLEntry}s matching the specified {@link Query}.
   *
   * @param query specifies the {@link ACLEntry}s to read
   * @return the {@link ACLEntry}s that have the {@code object}.
   */
  Set<ACLEntry> read(Query query);

  /**
   * Deletes {@link ACLEntry}s matching the specified {@link Query}.
   *
   * @param query specifies the {@link ACLEntry}s to delete
   * @return the number of {@link ACLEntry}s deleted
   */
  int delete(Query query);

  /**
   *
   */
  public class Query {

    private final ObjectId objectId;
    private final SubjectId subjectId;
    private final String permission;

    public Query(ObjectId objectId, SubjectId subjectId, String permission) {
      this.objectId = objectId;
      this.subjectId = subjectId;
      this.permission = permission;
    }

    public Query(ObjectId objectId, SubjectId subjectId) {
      this(objectId, subjectId, null);
    }

    public ObjectId getObjectId() {
      return objectId;
    }

    public SubjectId getSubjectId() {
      return subjectId;
    }

    public String getPermission() {
      return permission;
    }
  }
}
