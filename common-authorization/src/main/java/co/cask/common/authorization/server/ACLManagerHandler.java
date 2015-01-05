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

import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.CustomTypeManager;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.client.InterfaceAdapter;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Handler for getting and setting ACL entries.
 */
@Path("/v1")
public class ACLManagerHandler extends AbstractHttpHandler {

  private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(ObjectId.class, new InterfaceAdapter<ObjectId>())
    .registerTypeAdapter(SubjectId.class, new InterfaceAdapter<SubjectId>())
    .create();

  private final ACLStore aclStore;
  private final CustomTypeManager typeManager;

  @Inject
  public ACLManagerHandler(ACLStore aclStore, CustomTypeManager typeManager) {
    this.aclStore = aclStore;
    this.typeManager = typeManager;
  }

  @GET
  @Path("/acls/{object-id}/{subject-id}")
  public void getACLs(HttpRequest request, HttpResponder responder,
                      @PathParam("object-id") String objectId,
                      @PathParam("subject-id") String subjectId) {

    ObjectId object = typeManager.fromObjectIdString(objectId);
    SubjectId subject = typeManager.fromSubjectIdString(subjectId);

    Set<ACLEntry> result = aclStore.read(object, subject);
    String response = GSON.toJson(result);
    responder.sendString(HttpResponseStatus.OK, response);
  }

  @POST
  @Path("/acls/{object-id}/{subject-id}/{permission}")
  public void setACL(HttpRequest request, HttpResponder responder,
                     @PathParam("object-id") String objectId,
                     @PathParam("subject-id") String subjectId,
                     @PathParam("permission") String permission) {

    ObjectId object = typeManager.fromObjectIdString(objectId);
    SubjectId subject = typeManager.fromSubjectIdString(subjectId);

    ACLEntry aclEntry = new ACLEntry(object, subject, permission);
    if (aclStore.write(aclEntry)) {
      responder.sendStatus(HttpResponseStatus.OK);
    } else {
      responder.sendStatus(HttpResponseStatus.NOT_MODIFIED);
    }
  }

  @DELETE
  @Path("/acls/{object-id}/{subject-id}/{permission}")
  public void deleteACL(HttpRequest request, HttpResponder responder,
                        @PathParam("object-id") String objectId,
                        @PathParam("subject-id") String subjectId,
                        @PathParam("permission") String permission) {

    ObjectId object = typeManager.fromObjectIdString(objectId);
    SubjectId subject = typeManager.fromSubjectIdString(subjectId);

    ACLEntry aclEntry = new ACLEntry(object, subject, permission);
    if (aclStore.delete(aclEntry)) {
      responder.sendStatus(HttpResponseStatus.OK);
    } else {
      responder.sendStatus(HttpResponseStatus.NOT_MODIFIED);
    }
  }
}
