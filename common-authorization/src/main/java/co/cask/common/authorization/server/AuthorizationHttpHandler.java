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
package co.cask.common.authorization.server;

import co.cask.common.authorization.ACLEntry;
import co.cask.common.authorization.ACLStore;
import co.cask.common.authorization.ObjectId;
import co.cask.common.authorization.SubjectId;
import co.cask.common.authorization.SubjectType;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Handler for getting and setting ACL entries.
 */
@Path("/v1")
public class AuthorizationHttpHandler extends AbstractHttpHandler {

  private static final Gson GSON = new Gson();

  private final ACLStore aclStore;

  @Inject
  public AuthorizationHttpHandler(ACLStore aclStore) {
    this.aclStore = aclStore;
  }

  @GET
  @Path("/acls/{object-type}/{object-id}/{subject-type}/{subject-id}")
  public void getACLs(HttpRequest request, HttpResponder responder,
                      @PathParam("object-type") String objectType, @PathParam("object-id") String objectId,
                      @PathParam("subject-type") String subjectType, @PathParam("subject-id") String subjectId) {

    ObjectId object = new ObjectId(objectType, objectId);
    SubjectId subject = new SubjectId(SubjectType.valueOf(subjectType), subjectId);

    List<ACLEntry> result = aclStore.read(object, subject);
    responder.sendString(HttpResponseStatus.OK, GSON.toJson(result));
  }

  @POST
  @Path("/acls")
  public void setACL(HttpRequest request, HttpResponder responder) {
    ACLEntry aclEntry = GSON.fromJson(request.getContent().toString(Charsets.UTF_8), ACLEntry.class);
    if (aclStore.write(aclEntry)) {
      responder.sendStatus(HttpResponseStatus.OK);
    } else {
      responder.sendStatus(HttpResponseStatus.NOT_MODIFIED);
    }
  }

}
