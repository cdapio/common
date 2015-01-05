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

/**
 *
 */
public class UserId implements SubjectId {

  public static final UserId ANON = new UserId(null) {
    @Override
    public String getId() {
      return "anonuser://";
    }

    @Override
    public int hashCode() {
      return Objects.hashCode("anonuser://");
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final UserId other = (UserId) obj;
      return Objects.equal("anonuser:", other.getId());
    }
  };

  private final String id;

  public UserId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return "user:" + id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final UserId other = (UserId) obj;
    return Objects.equal(this.id, other.id);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("id", id).toString();
  }
}
