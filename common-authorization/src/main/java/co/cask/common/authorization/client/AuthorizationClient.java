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
package co.cask.common.authorization.client;

import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.UnauthorizedException;
import co.cask.common.http.HttpRequest;
import co.cask.common.http.HttpRequests;
import co.cask.common.http.HttpResponse;
import co.cask.common.http.ObjectResponse;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Provides ways to verify access and create/find ACL entries.
 */
public abstract class AuthorizationClient {

  private static final Gson GSON = new Gson();

  public void verifyAuthorized(ObjectId objectId, Iterable<SubjectId> subjectIds,
                               Iterable<String> requiredPermissions) throws IOException {

    for (SubjectId subjectId : subjectIds) {
      List<ACLEntry> acls = this.getACLs(objectId, subjectId);
      if (fulfillsRequiredPermissions(acls, requiredPermissions)) {
        return;
      }
    }

    throw new UnauthorizedException();
  }

  public void verifyAuthorized(ObjectId objectId, Iterable<SubjectId> subjectIds,
                               String requiredPermission) throws IOException {
    this.verifyAuthorized(objectId, subjectIds, ImmutableSet.of(requiredPermission));
  }

  protected abstract URL resolveURL(String path) throws MalformedURLException;

  public List<ACLEntry> getACLs(ObjectId objectId, SubjectId subjectId) throws IOException {
    HttpRequest request = HttpRequest.get(resolveURL(String.format(
      "/v1/acls/%s/%s/%s/%s", objectId.getType(), objectId.getId(), subjectId.getType(), subjectId.getId()))).build();
    HttpResponse response = HttpRequests.execute(request);
    return ObjectResponse.fromJsonBody(response, new TypeToken<List<ACLEntry>>() { }).getResponseObject();
  }

  public boolean setACL(ObjectId objectId, SubjectId subjectId, String permission) throws IOException {
    HttpRequest request = HttpRequest.post(resolveURL("/v1/acls"))
      .withBody(GSON.toJson(new ACLEntry(objectId, subjectId, permission)))
      .build();

    HttpResponse response = HttpRequests.execute(request);
    if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
      return true;
    } else if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
      return false;
    }

    throw new IOException("Unexpected response: " + response.getResponseCode() + ": " + response.getResponseMessage());
  }

  private boolean fulfillsRequiredPermissions(List<ACLEntry> aclEntries, Iterable<String> requiredPermissions) {
    Set<String> remainingRequiredPermission = Sets.newHashSet(requiredPermissions);
    for (ACLEntry aclEntry : aclEntries) {
      remainingRequiredPermission.remove(aclEntry.getPermission());
    }
    return remainingRequiredPermission.isEmpty();
  }
}
