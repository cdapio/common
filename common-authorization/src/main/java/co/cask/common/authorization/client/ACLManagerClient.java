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
import co.cask.common.authorization.AuthorizationContext;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.http.HttpRequest;
import co.cask.common.http.HttpRequests;
import co.cask.common.http.HttpResponse;
import co.cask.common.http.ObjectResponse;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Provides ways to verify, create, and list ACL entries.
 */
public class ACLManagerClient {

  public static final String CONF_CACHE_MAX_SIZE = "cask.common.authorization.client.cache.max-size";
  public static final String CONF_CACHE_EXPIRATION = "cask.common.authorization.client.cache.expiration";

  public static final String NAME = "AuthorizationClient";

  private static final Logger LOG = LoggerFactory.getLogger(ACLManagerClient.class);
  private static final long DEFAULT_CACHE_EXPIRATION = 5;
  private static final long DEFAULT_CACHE_MAX_SIZE = 10000;

  private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(ObjectId.class, new InterfaceAdapter<ObjectId>())
    .registerTypeAdapter(SubjectId.class, new InterfaceAdapter<SubjectId>())
    .create();

  private final AuthorizationContext context;

  /**
   * (ObjectId, SubjectId) -> modifiable set of ACL entries
   */
  private final LoadingCache<Key, Set<ACLEntry>> aclCache;
  private final Supplier<URI> baseURISupplier;

  @Inject
  public ACLManagerClient(AuthorizationContext context, @Named(NAME) Supplier<URI> baseURISupplier) {
    this.context = context;
    this.baseURISupplier = baseURISupplier;
    this.aclCache = CacheBuilder.newBuilder()
      .maximumSize(0)
      .build(new CacheLoader<Key, Set<ACLEntry>>() {
        @Override
        public Set<ACLEntry> load(Key key) throws Exception {
          ObjectId objectId = key.getObjectId();
          SubjectId subjectId = key.getSubjectId();

          String path = String.format("/v1/acls/%s/%s", objectId.getId(), subjectId.getId());
          HttpRequest request = HttpRequest.get(resolveURL(path)).build();
          HttpResponse response = HttpRequests.execute(request);
          List<ACLEntry> aclEntries = ObjectResponse.fromJsonBody(
            response, new TypeToken<List<ACLEntry>>() { }, GSON).getResponseObject();
          return Sets.newHashSet(aclEntries);
        }
      });
  }

  /**
   * @param objectId the object that is being accessed
   * @param subjectId the subject that is accessing the objectId
   * @return the list of the {@link ACLEntry}s that are relevant to an object and a subject
   * @throws IOException if an error occurred when contacting the authorization service
   */
  public Set<ACLEntry> getACLs(ObjectId objectId, SubjectId subjectId) throws IOException {
    try {
      return aclCache.get(new Key(objectId, subjectId));
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), IOException.class);
      throw Throwables.propagate(e.getCause());
    }
  }

  /**
   * Sets an {@link ACLEntry} for an object, subject, and a permission. This allows the subject to
   * access the object for the specified permission.
   *
   * <p>
   * For example, if object is "secretFile", subject is "Bob", and permission is "WRITE", then "Bob"
   * would be allowed to write to the "secretFile", assuming that what is doing the writing is protecting
   * the "secretFile" via a call to one of the {@code verifyAuthorized()} or {@code isAuthorized()} calls.
   * </p>
   *
   * @param object the object that is being accessed
   * @param subject the subject that is accessing the object
   * @param permission the permission to allow the subject to operate on the object for
   * @return true if the {@link ACLEntry} did not previously exist
   * @throws IOException if an error occurred when contacting the authorization service
   */
  public boolean setACL(ObjectId object, SubjectId subject, String permission) throws IOException {
    String path = String.format("/v1/acls/%s/%s/%s", object.getId(), subject.getId(), permission);
    HttpRequest request = HttpRequest.post(resolveURL(path))
      // TODO: proper serialization of objectId, subjectId, permission
      .withBody(GSON.toJson(new ACLEntry(object, subject, permission)))
      .build();
    HttpResponse response = HttpRequests.execute(request);

    if (response.getResponseCode() != HttpURLConnection.HTTP_OK &&
      response.getResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
      throw new IOException("Unexpected response: " + response.getResponseCode() +
                              ": " + response.getResponseMessage());
    }

    return response.getResponseCode() == HttpURLConnection.HTTP_OK;
  }

  /**
   * Deletes an {@link ACLEntry} for an object, subject, and a permission. This disallows the subject to
   * access the object for the specified permission.
   *
   * <p>
   * For example, if object is "secretFile", subject is "Bob", and permission is "WRITE", then "Bob"
   * would be no longer allowed to write to the "secretFile", assuming that what is doing the writing is protecting
   * the "secretFile" via a call to one of the {@code verifyAuthorized()} or {@code isAuthorized()} calls.
   * </p>
   *
   * @param objectId the object that is being accessed
   * @param subjectId the subject that is accessing the objectId
   * @param permission the permission to disallow the subject to operate on the object for
   * @return true if the {@link ACLEntry} previously existed
   * @throws IOException if an error occurred when contacting the authorization service
   */
  public boolean deleteACL(ObjectId objectId, SubjectId subjectId,
                           String permission) throws IOException {
    String path = String.format("/v1/acls/%s/%s/%s", objectId.getId(), subjectId.getId(), permission);
    HttpRequest request = HttpRequest.delete(resolveURL(path)).build();
    HttpResponse response = HttpRequests.execute(request);

    if (response.getResponseCode() != HttpURLConnection.HTTP_OK &&
      response.getResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
      throw new IOException("Unexpected response: " + response.getResponseCode() +
                              ": " + response.getResponseMessage());
    }

    return response.getResponseCode() == HttpURLConnection.HTTP_OK;
  }

  protected URL resolveURL(String path) throws MalformedURLException {
    return baseURISupplier.get().resolve(path).toURL();
  }

  /**
   * Key for {@link #aclCache}: (ObjectId, SubjectId)
   */
  private static final class Key {
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
      return Objects.equal(this.objectId, other.objectId) && Objects.equal(this.subjectId, other.subjectId);
    }
  }
}
