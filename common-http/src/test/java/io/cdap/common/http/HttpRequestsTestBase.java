/*
 * Copyright © 2014-2016 Cask Data, Inc.
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

package io.cdap.common.http;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.matcher.Matcher;
import io.cdap.http.AbstractHttpHandler;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.only;
import static org.junit.Assert.fail;

/**
 * Test base for {@link HttpRequests}.
 */
public abstract class HttpRequestsTestBase {

  protected abstract URI getBaseURI() throws URISyntaxException;

  protected abstract HttpRequestConfig getHttpRequestsConfig();

  protected abstract int getNumConnectionsOpened();

  protected abstract boolean returnResponseStream();

  @Test
  public void testHttpStatus() throws Exception {
    testGet("/fake/fake", only(404), only("Not Found"),
            only("Problem accessing: /fake/fake. Reason: Not Found"), any());
    testGet("/api/testOkWithResponse", only(200), any(), only("Great response"), any());

    int numConnectionsOpened = getNumConnectionsOpened();
    testGet("/api/testOkWithResponse201", only(201), any(), only("Great response 201"), any());
    testGet("/api/testOkWithResponse202", only(202), any(), only("Great response 202"), any());
    testGet("/api/testOkWithResponse203", only(203), any(), only("Great response 203"), any());
    testGet("/api/testOkWithResponse204", only(204), any(), only(""), any());
    testGet("/api/testOkWithResponse205", only(205), any(), only("Great response 205"), any());
    testGet("/api/testOkWithResponse206", only(206), any(), only("Great response 206"), any());
    // the preceding sequence of calls should reuse the same connection
    Assert.assertEquals(numConnectionsOpened, getNumConnectionsOpened());

    // Expected headers for a request
    Multimap<String, String> expectedHeaders = ArrayListMultimap.create();
    expectedHeaders.put("headerKey", "headerValue2");
    expectedHeaders.put("headerKey", "headerValue1");
    expectedHeaders.put(HttpHeaderNames.CONTENT_LENGTH.toString(), "0");
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

  private HttpRequest getRequest(HttpRequest.Builder builder, CompletableFuture<String> future) {
    if (returnResponseStream()) {
      // Set a small chunk size to verify that we're doing chunked reads correctly.
      int chunkSize = 5;
      StringBuilder sb = new StringBuilder();
      return builder.withContentConsumer(new HttpContentConsumer(chunkSize) {
        @Override
        public boolean onReceived(ByteBuffer buffer) {
          // create byte array with length = number of bytes written to the buffer
          byte[] bytes = new byte[buffer.remaining()];
          // read the bytes that were written to the buffer
          buffer.get(bytes);
          sb.append(new String(bytes, StandardCharsets.UTF_8));
          return true;
        }

        @Override
        public void onFinished() {
          future.complete(sb.toString());
        }
      }).build();
    } else {
      return builder.build();
    }
  }

  private void testPost(String path, Map<String, String> headers, String body,
                        Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                        Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    CompletableFuture<String> future = new CompletableFuture<>();
    HttpRequest request = getRequest(HttpRequest.post(url).addHeaders(headers).withBody(body), future);
    HttpRequestConfig requestConfig = getHttpRequestsConfig();
    HttpResponse response = HttpRequests.execute(request, requestConfig);
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders, future);
  }

  private void testPost(String path, Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                        Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    testPost(path, ImmutableMap.of(), "", expectedResponseCode,
             expectedMessage, expectedBody, expectedHeaders);
  }

  private void testPut(String path, Map<String, String> headers, String body,
                       Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                       Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    CompletableFuture<String> future = new CompletableFuture<>();
    HttpRequest request = getRequest(HttpRequest.put(url).addHeaders(headers).withBody(body), future);
    HttpRequestConfig requestConfig = getHttpRequestsConfig();
    HttpResponse response = HttpRequests.execute(request, requestConfig);
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders, future);
  }

  private void testGet(String path, Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                       Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    CompletableFuture<String> future = new CompletableFuture<>();
    HttpRequest request = getRequest(HttpRequest.get(url), future);
    HttpRequestConfig requestConfig = getHttpRequestsConfig();
    HttpResponse response = HttpRequests.execute(request, requestConfig);
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders, future);
  }

  private void testDelete(String path, Matcher<Object> expectedResponseCode, Matcher<Object> expectedMessage,
                          Matcher<Object> expectedBody, Matcher<Object> expectedHeaders) throws Exception {

    URL url = getBaseURI().resolve(path).toURL();
    CompletableFuture<String> future = new CompletableFuture<>();
    HttpRequest request = getRequest(HttpRequest.delete(url), future);
    HttpRequestConfig requestConfig = getHttpRequestsConfig();
    HttpResponse response = HttpRequests.execute(request, requestConfig);
    verifyResponse(response, expectedResponseCode, expectedMessage, expectedBody, expectedHeaders, future);
  }

  private void verifyResponse(HttpResponse response, Matcher<Object> expectedResponseCode,
                              Matcher<Object> expectedMessage, Matcher<Object> expectedBody,
                              Matcher<Object> expectedHeaders, CompletableFuture<String> future) {

    Assert.assertTrue("Response code - expected: " + expectedResponseCode.toString()
                        + " actual: " + response.getResponseCode(),
                      expectedResponseCode.matches(response.getResponseCode()));

    Assert.assertTrue("Response message - expected: " + expectedMessage.toString()
                        + " actual: " + response.getResponseMessage(),
                      expectedMessage.matches(response.getResponseMessage()));

    String actualResponseBody = "";
    if (!returnResponseStream()) {
      actualResponseBody = response.getResponseBodyAsString();
    } else {
      try {
        response.consumeContent();
        actualResponseBody = future.get();
      } catch (IOException | InterruptedException | ExecutionException e) {
          fail("Unexpected exception");
      }
    }
    Assert.assertTrue("Response body - expected: " + expectedBody.toString()
                        + " actual: " + actualResponseBody,
                      expectedBody.matches(actualResponseBody));

    Assert.assertTrue("Response headers - expected: " + expectedHeaders.toString()
                      + " actual: " + response.getHeaders(),
                      expectedHeaders.matches(response.getHeaders()));
  }

  @Path("/api")
  public static final class TestHandler extends AbstractHttpHandler {

    @GET
    @Path("/testHttpStatus")
    public void testHttpStatus(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @GET
    @Path("/testOkWithResponse")
    public void testOkWithResponse(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.OK, "Great response");
    }

    @GET
    @Path("/testOkWithResponse201")
    public void testOkWithResponse201(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.CREATED, "Great response 201");
    }

    @GET
    @Path("/testOkWithResponse202")
    public void testOkWithResponse202(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.ACCEPTED, "Great response 202");
    }

    @GET
    @Path("/testOkWithResponse203")
    public void testOkWithResponse203(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION, "Great response 203");
    }

    @GET
    @Path("/testOkWithResponse204")
    public void testOkWithResponse204(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.NO_CONTENT);
    }

    @GET
    @Path("/testOkWithResponse205")
    public void testOkWithResponse205(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.RESET_CONTENT, "Great response 205");
    }

    @GET
    @Path("/testOkWithResponse206")
    public void testOkWithResponse206(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.PARTIAL_CONTENT, "Great response 206");
    }

    @GET
    @Path("/testOkWithHeaders")
    public void testOkWithHeaders(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.OK, new DefaultHttpHeaders()
        .add("headerKey", "headerValue1").add("headerKey", "headerValue2"));
    }

    @GET
    @Path("/testBadRequest")
    public void testBadRequest(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
    }

    @GET
    @Path("/testBadRequestWithErrorMessage")
    public void testBadRequestWithErrorMessage(io.netty.handler.codec.http.HttpRequest request,
                                               HttpResponder responder) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, "Cool error message");
    }

    @GET
    @Path("/testConflict")
    public void testConflict(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.CONFLICT);
    }

    @GET
    @Path("/testConflictWithMessage")
    public void testConflictWithMessage(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.CONFLICT, "Conflictmes");
    }

    @POST
    @Path("/testHttpStatus")
    public void testHttpStatusPost(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @POST
    @Path("/testOkWithResponse")
    public void testOkWithResponsePost(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.OK, "Great response");
    }

    @POST
    @Path("/testOkWithResponse201")
    public void testOkWithResponse201Post(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.CREATED, "Great response 201");
    }

    @POST
    @Path("/testOkWithResponse202")
    public void testOkWithResponse202Post(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.ACCEPTED, "Great response 202");
    }

    @POST
    @Path("/testOkWithResponse203")
    public void testOkWithResponse203Post(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.NON_AUTHORITATIVE_INFORMATION, "Great response 203");
    }

    @POST
    @Path("/testOkWithResponse204")
    public void testOkWithResponse204Post(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.NO_CONTENT);
    }

    @POST
    @Path("/testOkWithResponse205")
    public void testOkWithResponse205Post(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.RESET_CONTENT, "Great response 205");
    }

    @POST
    @Path("/testOkWithResponse206")
    public void testOkWithResponse206Post(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.PARTIAL_CONTENT, "Great response 206");
    }

    @POST
    @Path("/testBadRequest")
    public void testBadRequestPost(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
    }

    @POST
    @Path("/testBadRequestWithErrorMessage")
    public void testBadRequestWithErrorMessagePost(io.netty.handler.codec.http.HttpRequest request,
                                                   HttpResponder responder) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, "Cool error message");
    }

    @POST
    @Path("/testConflict")
    public void testConflictPost(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.CONFLICT);
    }

    @POST
    @Path("/testConflictWithMessage")
    public void testConflictWithMessagePost(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendString(HttpResponseStatus.CONFLICT, "Conflictmes");
    }

    @POST
    @Path("/testPost")
    public void testPost(FullHttpRequest request,
                         HttpResponder responder) {
      responder.sendString(HttpResponseStatus.OK,
                           request.content().toString(Charsets.UTF_8) + request.headers().get("sdf"));
    }

    @POST
    @Path("/testPost409")
    public void testPost409(FullHttpRequest request,
                            HttpResponder responder) {
      responder.sendString(HttpResponseStatus.CONFLICT, request.content().toString(Charsets.UTF_8)
        + request.headers().get("sdf") + "409");
    }

    @PUT
    @Path("/testPut")
    public void testPut(FullHttpRequest request,
                        HttpResponder responder) {
      responder.sendString(HttpResponseStatus.OK, request.content().toString(Charsets.UTF_8)
        + request.headers().get("sdf"));
    }

    @PUT
    @Path("/testPut409")
    public void testPut409(FullHttpRequest request,
                           HttpResponder responder) {
      responder.sendString(HttpResponseStatus.CONFLICT, request.content().toString(Charsets.UTF_8)
        + request.headers().get("sdf") + "409");
    }

    @DELETE
    @Path("/testDelete")
    public void testDelete(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.OK);
    }

    @GET
    @Path("/testWrongMethod")
    public void testWrongMethod(io.netty.handler.codec.http.HttpRequest request, HttpResponder responder) {
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }

}
