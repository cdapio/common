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
   * Writes an {@link ACLEntry}.
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
   * Fetches a set of {@link ACLEntry}s by {@code object} and {@code subject}.
   *
   * @param objectId the relevant object
   * @param subjectId the relevant subject
   * @return the {@link ACLEntry}s that have the {@code object} and {@code subject}.
   */
  Set<ACLEntry> read(ObjectId objectId, SubjectId subjectId);

  /**
   * Deletes an {@link ACLEntry}.
   *
   * @param entry the {@link ACLEntry} to delete
   * @return true if the entry did not previously exist
   */
  boolean delete(ACLEntry entry);
}
