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
package co.cask.common.authorization.server;

import co.cask.common.Bytes;
import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.NamespaceId;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Handler for getting and setting ACL entries.
 */
@Path("/v1")
public class ACLManagerHandler extends AbstractHttpHandler {

  private static final Gson GSON = new Gson();

  private final ACLStore aclStore;

  @Inject
  public ACLManagerHandler(ACLStore aclStore) {
    this.aclStore = aclStore;
  }

  @GET
  @Path("/acls/global")
  public void getGlobalACLs(HttpRequest request, HttpResponder responder,
                            @QueryParam("subject") String subjectString,
                            @QueryParam("permission") String permission) {

    // TODO: validate input
    SubjectId subject = string2SubjectId(subjectString);
    ObjectId objectId = ObjectId.GLOBAL;

    Set<ACLEntry> result = aclStore.read(new ACLStore.Query(objectId, subject, permission));
    String response = GSON.toJson(result);
    responder.sendString(HttpResponseStatus.OK, response);
  }

  @GET
  @Path("/acls/namespace/{namespace-id}")
  public void getACLs(HttpRequest request, HttpResponder responder,
                      @PathParam("namespace-id") String namespaceId,
                      @QueryParam("object") String objectString,
                      @QueryParam("subject") String subjectString,
                      @QueryParam("permission") String permission) {

    // TODO: validate input
    SubjectId subject = string2SubjectId(subjectString);
    NamespaceId namespace = new NamespaceId(namespaceId);
    ObjectId objectId = string2ObjectId(namespace, objectString);

    Set<ACLEntry> result = aclStore.read(new ACLStore.Query(objectId, subject, permission));
    String response = GSON.toJson(result);
    responder.sendString(HttpResponseStatus.OK, response);
  }

  @DELETE
  @Path("/acls/global")
  public void deleteGlobalACLs(HttpRequest request, HttpResponder responder,
                               @QueryParam("subject") String subjectString,
                               @QueryParam("permission") String permission) {

    // TODO: validate input
    SubjectId subject = string2SubjectId(subjectString);
    ObjectId objectId = ObjectId.GLOBAL;

    int result = aclStore.delete(new ACLStore.Query(objectId, subject, permission));
    responder.sendString(HttpResponseStatus.OK, GSON.toJson(new DeleteResponse(result)));
  }

  @DELETE
  @Path("/acls/namespace/{namespace-id}")
  public void deleteACLs(HttpRequest request, HttpResponder responder,
                         @PathParam("namespace-id") String namespaceId,
                         @QueryParam("object") String objectString,
                         @QueryParam("subject") String subjectString,
                         @QueryParam("permission") String permission) {

    // TODO: validate input
    SubjectId subject = string2SubjectId(subjectString);
    NamespaceId namespace = new NamespaceId(namespaceId);
    ObjectId objectId = string2ObjectId(namespace, objectString);

    int result = aclStore.delete(new ACLStore.Query(objectId, subject, permission));
    responder.sendString(HttpResponseStatus.OK, GSON.toJson(new DeleteResponse(result)));
  }

  @POST
  @Path("/acls/global")
  public void createGlobalACL(HttpRequest request, HttpResponder responder) {
    String body = Bytes.toString(request.getContent().toByteBuffer());
    ACLEntry aclEntry = GSON.fromJson(body, ACLEntry.class);
    aclEntry.setObject(ObjectId.GLOBAL);

    if (aclStore.write(aclEntry)) {
      responder.sendStatus(HttpResponseStatus.OK);
    } else {
      responder.sendStatus(HttpResponseStatus.NOT_MODIFIED);
    }
  }

  @POST
  @Path("/acls/namespace/{namespace-id}")
  public void createACL(HttpRequest request, HttpResponder responder, @PathParam("namespace-id") String namespaceId) {
    NamespaceId namespace = new NamespaceId(namespaceId);

    String body = Bytes.toString(request.getContent().toByteBuffer());
    ACLEntry aclEntry = GSON.fromJson(body, ACLEntry.class);
    aclEntry.getObject().setParent(namespace);

    if (aclStore.write(aclEntry)) {
      responder.sendStatus(HttpResponseStatus.OK);
    } else {
      responder.sendStatus(HttpResponseStatus.NOT_MODIFIED);
    }
  }

  private SubjectId string2SubjectId(String subjectIdString) {
    if (subjectIdString == null) {
      return null;
    }

    String[] split = subjectIdString.split(":");
    if (split.length == 2) {
      return new SubjectId(split[0], split[1]);
    }

    throw new IllegalArgumentException("Invalid subjectId format: " + subjectIdString);
  }

  private ObjectId string2ObjectId(ObjectId parent, String objectIdString) {
    if (objectIdString == null) {
      return null;
    }

    if (ObjectId.GLOBAL.getType().equals(objectIdString)) {
      return ObjectId.GLOBAL;
    }

    String[] split = objectIdString.split(":");
    if (split.length == 1) {
      return new ObjectId(parent, split[0], "");
    } else if (split.length == 2) {
      return new ObjectId(parent, split[0], split[1]);
    }

    throw new IllegalArgumentException("Invalid objectId format: " + objectIdString);
  }

  /**
   *
   */
  public static final class DeleteResponse {
    private final int deleted;

    public DeleteResponse(int deleted) {
      this.deleted = deleted;
    }

    public int getDeleted() {
      return deleted;
    }
  }
}
