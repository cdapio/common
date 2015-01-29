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

import com.google.common.base.Objects;

/**
 * An Access Control List (ACL) entry, which allows the {@link #subject}
 * access to {@link #object} for a {@link #permission}.
 *
 * <p>
 * For example, an ACL entry
 * of {@code subject=bob, object=/tmp/sdf, permission=WRITE} might allow
 * the user {@code bob} to {@code WRITE} to the {@code /tmp/sdf} file.
 * The actual meaning depends on the system that uses this library.
 * </p>
 */
public class ACLEntry {

  private ObjectId object;
  private SubjectId subject;
  private String permission;

  public ACLEntry(ObjectId object, SubjectId subject, String permission) {
    this.object = object;
    this.subject = subject;
    this.permission = permission;
  }

  public ObjectId getObject() {
    return object;
  }

  public SubjectId getSubject() {
    return subject;
  }

  public String getPermission() {
    return permission;
  }

  public void setObject(ObjectId object) {
    this.object = object;
  }

  public void setSubject(SubjectId subject) {
    this.subject = subject;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(object, subject, permission);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ACLEntry other = (ACLEntry) obj;
    return Objects.equal(this.object, other.object) &&
      Objects.equal(this.subject, other.subject) &&
      Objects.equal(this.permission, other.permission);
  }
}
