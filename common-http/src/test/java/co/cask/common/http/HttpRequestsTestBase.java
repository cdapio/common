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

package co.cask.common.http;

import co.cask.http.AbstractHttpHandler;
import co.cask.http.BodyConsumer;
import co.cask.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.inject.matcher.Matcher;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.only;

/**
 * Test base for {@link HttpRequests}.
 */
public abstract class HttpRequestsTestBase {

  @ClassRule
  public static final TemporaryFolder TMP_FOLDER = new TemporaryFolder();

  protected abstract URI getBaseURI() throws URISyntaxException;

  protected abstract HttpRequestConfig getHttpRequestsConfig();

  @Test
  public void testHttpStatus() throws Exception {
    testGet("/fake/fake", only(404), only("Not Found"),
            only("Problem accessing: /fake/fake. Reason: Not Found"), any());
    testGet("/api/testOkWithResponse", only(200), any(), only("Great response"), any());
    testGet("/api/testOkWithResponse201", only(201), any(), only("Great response 201"), any());
    testGet("/api/testOkWithResponse202", only(202), any(), only("Great response 202"), any());
    testGet("/api/testOkWithResponse203", only(203), any(), only("Great response 203"), any());
    testGet("/api/testOkWithResponse204", only(204), any(), only(""), any());
    testGet("/api/testOkWithResponse205", only(205), any(), only("Great response 205"), any());
    testGet("/api/testOkWithResponse206", only(206), any(), only("Great response 206"), any());

    // Expected headers for a request
    Multimap<String, String> expectedHeaders = ArrayListMultimap.create();
    expectedHeaders.put("headerKey", "headerValue2");
    expectedHeaders.put("headerKey", "headerValue1");
    expectedHeaders.put("Connection", "keep-alive");
    expectedHeaders.put("Content-Length", "0");
    testGet("/api/testOkWithHeaders", only(200), only("OK"), only(""), only(expectedHeaders));

    testGet("/api/testHttpStatus", only(200), only("OK"), only(""), any());
    testGet("/api/testBadRequest", only(400), only("Bad Request"), only(""), any());
    testGet("/api/testBadRequestWithErrorMessage", only(400), only("Bad Request"), only("Cool error message"), any());
    testGet("/api/testConflict", only(409), only("Conflict"), only(""), any());
    testGet("/api/testConflictWithMessage", only(409), only("Conflict"), only("Conflictmes"), any());

    testPost("/fake/fake", only(404), only("Not Found"),
             only("Problem accessing: /fake/fake. Reason: Not Found"), any());
    testPost("/api/testOkWithResponse", only(200), any(), only("Great response"), any());
    testPost("/api/testOkWithResponse201", only(201), any(), only("Great response 201"), any());
    testPost("/api/testOkWithResponse202", only(202), any(), only("Great response 202"), any());
    testPost("/api/testOkWithResponse203", only(203), any(), only("Great response 203"), any());
    testPost("/api/testOkWithResponse204", only(204), any(), only(""), any());
    testPost("/api/testOkWithResponse205", only(205), any(), only("Great response 205"), any());
    testPost("/api/testOkWithResponse206", only(206), any(), only("Great response 206"), any());
    testPost("/api/testHttpStatus", only(200), only("OK"), only(""), any());
    testPost("/api/testBadRequest", only(400), only("Bad Request"), only(""), any());
    testPost("/api/testBadRequestWithErrorMessage", only(400), only("Bad Request"), only("Cool error message"), any());
    testPost("/api/testConflict", only(409), only("Conflict"), only(""), any());
    testPost("/api/testConflictWithMessage", only(409), only("Conflict"), only("Conflictmes"), any());
    testPost("/api/testPost", ImmutableMap.of("sdf", "123zz"), "somebody", only(200),
             any(), only("somebody123zz"), any());
    testPost("/api/testPost409", ImmutableMap.of("sdf", "123zz"), "somebody", only(409), any(),
             only("somebody123zz409"), any());

    testPut("/api/testPut", ImmutableMap.of("sdf", "123zz"), "somebody", only(200),
            any(), only("somebody123zz"), any());
    testPut("/api/testPut409", ImmutableMap.of("sdf", "123zz"), "somebody", only(409),
            any(), only("somebody123zz409"), any());

    testDelete("/api/testDelete", only(200), any(), any(), any());
  }

  private void testPost(String path, Map<String, String> headers, String body,
                        Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                        Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    // Test string body
    URL url = getBaseURI().resolve(path).toURL();
    HttpRequest request = HttpRequest.post(url).addHeaders(headers).withBody(body).build();
    HttpResponse response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders);

    // Test with ByteBuffer body.
    request = HttpRequest.post(url).addHeaders(headers).withBody(Charsets.UTF_8.encode(body)).build();
    response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders);

    // Test with file body
    File file = TMP_FOLDER.newFile();
    Files.write(body, file, Charsets.UTF_8);
    request = HttpRequest.post(url).addHeaders(headers).withBody(file).build();
    response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders);
  }

  private void testPost(String path, Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                        Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    testPost(path, ImmutableMap.<String, String>of(), "", expectedResponseCode,
             expectedMessage, expectedBody, expectedHeaders);
  }

  private void testPut(String path, Map<String, String> headers, String body,
                       Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                       Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    HttpRequest request = HttpRequest.put(url).addHeaders(headers).withBody(body).build();
    HttpResponse response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders);
  }

  private void testGet(String path, Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                       Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    HttpRequest request = HttpRequest.get(url).build();
    HttpResponse response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders);
  }

  private void testDelete(String path, Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                          Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    HttpRequest request = HttpRequest.delete(url).build();
    HttpResponse response = HttpRequests.execute(request, getHttpRequestsConfig());
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders);
  }

  protected void verifyResponse(HttpResponse response, Matcher<Object> expectedResponseCode,
                              Matcher<Object> expectedMessage, Matcher<Object> expectedBody,
                              Matcher<Object> expectedHeaders) {

    Assert.assertTrue("Response code - expected: " + expectedResponseCode.toString()
                        + " actual: " + response.getResponseCode(),
                      expectedResponseCode.matches(response.getResponseCode()));

    Assert.assertTrue("Response message - expected: " + expectedMessage.toString()
                        + " actual: " + response.getResponseMessage(),
                      expectedMessage.matches(response.getResponseMessage()));

    String actualResponseBody = new String(response.getResponseBody());
    Assert.assertTrue("Response body - expected: " + expectedBody.toString()
                        + " actual: " + actualResponseBody,
                      expectedBody.matches(actualResponseBody));

    Assert.assertTrue("Response headers - expected: " + expectedHeaders.toString()
                      + " actual: " + response.getHeaders(),
                      expectedHeaders.matches(response.getHeaders()));
  }

  @Path("/api")
  public static final class TestHandler extends AbstractHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TestHandler.class);

    @GET
    @Path("/testHttpStatus")
    public void testHttpStatus(org.jboss.netty.handler.codec.http.HttpRequest request,
                               HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @GET
    @Path("/testOkWithResponse")
    public void testOkWithResponse(org.jboss.netty.handler.codec.http.HttpRequest request,
                                   HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.OK, "Great response");
    }

    @GET
    @Path("/testOkWithResponse201")
    public void testOkWithResponse201(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.CREATED, "Great response 201");
    }

    @GET
    @Path("/testOkWithResponse202")
    public void testOkWithResponse202(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.ACCEPTED, "Great response 202");
    }

    @GET
    @Path("/testOkWithResponse203")
    public void testOkWithResponse203(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION, "Great response 203");
    }

    @GET
    @Path("/testOkWithResponse204")
    public void testOkWithResponse204(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.NO_CONTENT);
    }

    @GET
    @Path("/testOkWithResponse205")
    public void testOkWithResponse205(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.RESET_CONTENT, "Great response 205");
    }

    @GET
    @Path("/testOkWithResponse206")
    public void testOkWithResponse206(org.jboss.netty.handler.codec.http.HttpRequest request,
                                      HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.PARTIAL_CONTENT, "Great response 206");
    }

    @GET
    @Path("/testOkWithHeaders")
    public void testOkWithHeaders(org.jboss.netty.handler.codec.http.HttpRequest request,
                                  HttpResponder responder) throws Exception {
      Multimap<String, String> headers = ImmutableListMultimap.of("headerKey", "headerValue1",
                                                                  "headerKey", "headerValue2");
      responder.sendStatus(HttpResponseStatus.OK, headers);
    }

    @GET
    @Path("/testBadRequest")
    public void testBadRequest(org.jboss.netty.handler.codec.http.HttpRequest request,
                               HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
    }

    @GET
    @Path("/testBadRequestWithErrorMessage")
    public void testBadRequestWithErrorMessage(org.jboss.netty.handler.codec.http.HttpRequest request,
                                               HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, "Cool error message");
    }

    @GET
    @Path("/testConflict")
    public void testConflict(org.jboss.netty.handler.codec.http.HttpRequest request,
                             HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.CONFLICT);
    }

    @GET
    @Path("/testConflictWithMessage")
    public void testConflictWithMessage(org.jboss.netty.handler.codec.http.HttpRequest request,
                                        HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.CONFLICT, "Conflictmes");
    }

    @POST
    @Path("/testHttpStatus")
    public void testHttpStatusPost(org.jboss.netty.handler.codec.http.HttpRequest request,
                                   HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @POST
    @Path("/testOkWithResponse")
    public void testOkWithResponsePost(org.jboss.netty.handler.codec.http.HttpRequest request,
                                       HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.OK, "Great response");
    }

    @POST
    @Path("/testOkWithResponse201")
    public void testOkWithResponse201Post(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.CREATED, "Great response 201");
    }

    @POST
    @Path("/testOkWithResponse202")
    public void testOkWithResponse202Post(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.ACCEPTED, "Great response 202");
    }

    @POST
    @Path("/testOkWithResponse203")
    public void testOkWithResponse203Post(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION, "Great response 203");
    }

    @POST
    @Path("/testOkWithResponse204")
    public void testOkWithResponse204Post(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.NO_CONTENT);
    }

    @POST
    @Path("/testOkWithResponse205")
    public void testOkWithResponse205Post(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.RESET_CONTENT, "Great response 205");
    }

    @POST
    @Path("/testOkWithResponse206")
    public void testOkWithResponse206Post(org.jboss.netty.handler.codec.http.HttpRequest request,
                                          HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.PARTIAL_CONTENT, "Great response 206");
    }

    @POST
    @Path("/testBadRequest")
    public void testBadRequestPost(org.jboss.netty.handler.codec.http.HttpRequest request,
                                   HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
    }

    @POST
    @Path("/testBadRequestWithErrorMessage")
    public void testBadRequestWithErrorMessagePost(org.jboss.netty.handler.codec.http.HttpRequest request,
                                                   HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, "Cool error message");
    }

    @POST
    @Path("/testConflict")
    public void testConflictPost(org.jboss.netty.handler.codec.http.HttpRequest request,
                                 HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.CONFLICT);
    }

    @POST
    @Path("/testConflictWithMessage")
    public void testConflictWithMessagePost(org.jboss.netty.handler.codec.http.HttpRequest request,
                                            HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.CONFLICT, "Conflictmes");
    }

    @POST
    @Path("/testPost")
    public void testPost(org.jboss.netty.handler.codec.http.HttpRequest request,
                         HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.OK,
                           request.getContent().toString(Charsets.UTF_8) + request.getHeader("sdf"));
    }

    @POST
    @Path("/testPost409")
    public void testPost409(org.jboss.netty.handler.codec.http.HttpRequest request,
                            HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.CONFLICT, request.getContent().toString(Charsets.UTF_8)
        + request.getHeader("sdf") + "409");
    }

    @PUT
    @Path("/testPut")
    public void testPut(org.jboss.netty.handler.codec.http.HttpRequest request,
                        HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.OK, request.getContent().toString(Charsets.UTF_8)
        + request.getHeader("sdf"));
    }

    @PUT
    @Path("/testPut409")
    public void testPut409(org.jboss.netty.handler.codec.http.HttpRequest request,
                           HttpResponder responder) throws Exception {
      responder.sendString(HttpResponseStatus.CONFLICT, request.getContent().toString(Charsets.UTF_8)
        + request.getHeader("sdf") + "409");
    }

    @DELETE
    @Path("/testDelete")
    public void testDelete(org.jboss.netty.handler.codec.http.HttpRequest request,
                           HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @GET
    @Path("/testWrongMethod")
    public void testWrongMethod(org.jboss.netty.handler.codec.http.HttpRequest request,
                                HttpResponder responder) throws Exception {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @POST
    @Path("/testChunk")
    public BodyConsumer testChunk(org.jboss.netty.handler.codec.http.HttpRequest request, HttpResponder responder,
                                  @HeaderParam("X-Request-Accept") boolean accept) throws Exception {
      if (!accept) {
        responder.sendStatus(HttpResponseStatus.BAD_REQUEST,
                             ImmutableMultimap.of(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE));
        return null;
      }

      return new BodyConsumer() {

        private ChannelBuffer buffer = ChannelBuffers.EMPTY_BUFFER;

        @Override
        public void chunk(ChannelBuffer request, HttpResponder responder) {
          buffer = ChannelBuffers.wrappedBuffer(buffer, ChannelBuffers.copiedBuffer(request));
        }

        @Override
        public void finished(HttpResponder responder) {
          responder.sendBytes(HttpResponseStatus.OK, buffer.toByteBuffer(), null);
        }

        @Override
        public void handleError(Throwable cause) {
          LOG.error("Exception", cause);
        }
      };
    }
  }

}
