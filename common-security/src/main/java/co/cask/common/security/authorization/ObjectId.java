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
 *
 */
public class ObjectId {

  public static final ObjectId GLOBAL = new ObjectId(null, "global", "");

  private ObjectId parent;
  private String type;
  private String id;

  public ObjectId(ObjectId parent, String type, String id) {
    this.parent = parent;
    this.type = type;
    this.id = id;
  }

  public ObjectId(String type, String id) {
    this.parent = ObjectId.GLOBAL;
    this.type = type;
    this.id = id;
  }

  public String getRep() {
    return type + ":" + id;
  }

  public void setParent(ObjectId parent) {
    this.parent = parent;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ObjectId getParent() {
    return parent;
  }

  public String getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(parent, type, id);
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || !(obj instanceof ObjectId)) {
      return false;
    }

    final ObjectId other = (ObjectId) obj;
    return Objects.equal(this.parent, other.parent) &&
      Objects.equal(this.type, other.type) &&
      Objects.equal(this.id, other.id);
  }
}
