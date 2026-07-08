package com.blnkfinance.blnk;

import com.blnkfinance.blnk.endpoints.Reconciliation;
import com.blnkfinance.blnk.testsupport.CapturingTransport;
import com.blnkfinance.blnk.testsupport.TransportMocks;
import com.blnkfinance.blnk.testsupport.StreamTestUtils;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkApiErrorDetail;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.blnkfinance.blnk.util.MultipartBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the core {@link Blnk} client: construction and option
 * defaults, authentication headers, multipart uploads, timeouts, retry
 * behavior, and error handling.
 */
class BaseBlnkClientTest {

  // A single options instance is intentionally shared across all tests.
  private static final BlnkClientOptions options = TestMocks.createMockBlnkClientOptions();
  private static final BlnkTransport thirdPartyRequest = TransportMocks.successTransport();
  private static final Map<String, ServiceFactory> mockServices = TestMocks.createMockServices();
  private static final String apiKey = "123455";

  private Blnk blnk;

  @BeforeEach
  void beforeEach() {
    blnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE,
            thirdPartyRequest);
  }

  private static ObjectNode obj(String... kv) {
    ObjectNode node = BlnkJson.objectNode();
    for (int i = 0; i < kv.length; i += 2) {
      node.put(kv[i], kv[i + 1]);
    }
    return node;
  }

  @Test
  @DisplayName("Init Blnk Client")
  void initBlnkClient() {
    assertEquals(apiKey, blnk.apiKey);
  }

  @Test
  @DisplayName("should throw error when baseUrl is missing in options")
  void throwsWhenBaseUrlMissing() {
    BlnkClientOptions invalidOptions = BlnkClientOptions.builder().build();
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
        new Blnk(apiKey, invalidOptions, mockServices, HttpClientUtils.FORMAT_RESPONSE,
            thirdPartyRequest));
    assertEquals("baseUrl is required for self-hosted Blnk SDK.", thrown.getMessage());
  }

  @Test
  @DisplayName("Omits X-Blnk-Key for local unauthenticated mode")
  void omitsApiKeyHeader() {
    CapturingTransport capturedTransport = CapturingTransport.of(TransportMocks.successTransport());
    Blnk localBlnk =
        new Blnk("", options, mockServices, HttpClientUtils.FORMAT_RESPONSE, capturedTransport);

    localBlnk.request("health", obj(), "GET", null);

    Map<String, String> headers = capturedTransport.calls.get(0).request().headers();
    assertFalse(headers.containsKey("X-Blnk-Key"));
  }

  @Test
  @DisplayName("Sends X-Blnk-Key when api key is set")
  void sendsApiKeyHeader() {
    CapturingTransport capturedTransport = CapturingTransport.of(TransportMocks.successTransport());
    Blnk authedBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, capturedTransport);

    authedBlnk.request("health", obj(), "GET", null);

    Map<String, String> headers = capturedTransport.calls.get(0).request().headers();
    assertEquals(apiKey, headers.get("X-Blnk-Key"));
  }

  @Test
  @DisplayName("Does not expose public getApiKey getter")
  void noGetApiKey() {
    for (Method method : Blnk.class.getMethods()) {
      assertFalse(method.getName().equals("getApiKey"),
          "Blnk must not expose a public getApiKey getter (removed in v1.3.0)");
    }
  }

  @Test
  @DisplayName("Constructor should set apiKey, options, logger, services, and formatResponse correctly")
  void constructorSetsFields() {
    assertEquals(apiKey, blnk.apiKey, "apiKey is set correctly");
    assertEquals(5000, blnk.options.timeout(), "timeout is set correctly");
    assertSame(options.logger(), blnk.logger, "logger is set correctly");
    assertSame(mockServices, blnk.services, "services are set correctly");
    assertSame(HttpClientUtils.FORMAT_RESPONSE, blnk.formatResponse,
        "formatResponse is set correctly");
  }

  private static CapturingTransport created201Transport() {
    return CapturingTransport.of((url, request) -> TransportResponse.builder()
        .ok(true)
        .status(201)
        .statusText("Created")
        .json(() -> obj("upload_id", "upl_test"))
        .build());
  }

  @Test
  @DisplayName("request converts multipart form data for the transport")
  void requestConvertsMultipartBody() {
    MultipartBody formData = MultipartBody.create();
    formData.append("source", "stripe");
    formData.append("file", "a,b,c".getBytes(StandardCharsets.UTF_8), "test.csv");

    CapturingTransport capturedTransport = created201Transport();
    Blnk formBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, capturedTransport);

    formBlnk.request("reconciliation/upload", formData, "POST", null);

    TransportRequest init = capturedTransport.calls.get(0).request();
    assertTrue(init.multipart(), "body is a multipart payload");
    assertNotNull(init.body());
    String payload = StreamTestUtils.readBody(init.body());
    assertTrue(payload.contains("a,b,c"));
    Map<String, String> headers = init.headers();
    assertTrue(headers.get("content-type").contains("multipart/form-data"));
    assertFalse(headers.get("content-type").contains("application/json"));
  }

  @Test
  @DisplayName("request converts stream-backed multipart form data for the transport")
  void requestConvertsStreamBackedMultipartBody() throws Exception {
    Path dir = Files.createTempDirectory("blnk-upload-");
    Path filePath = dir.resolve("upload.csv");
    Files.writeString(filePath, "amount,ref\n100,abc");

    MultipartBody formData = MultipartBody.create();
    formData.append("source", "stripe");
    formData.appendFile("file", filePath);

    CapturingTransport capturedTransport = created201Transport();
    Blnk formBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, capturedTransport);

    formBlnk.request("reconciliation/upload", formData, "POST", null);

    TransportRequest init = capturedTransport.calls.get(0).request();
    assertTrue(init.multipart());
    String payload = StreamTestUtils.readBody(init.body());
    assertTrue(payload.contains("amount,ref"));
    assertTrue(payload.contains("100,abc"));
  }

  @Test
  @DisplayName("reconciliation.upload sends stream multipart body through the HTTP transport")
  void reconciliationUploadSendsMultipart() throws Exception {
    Path dir = Files.createTempDirectory("blnk-recon-upload-");
    Path filePath = dir.resolve("upload.csv");
    Files.writeString(filePath, "amount,ref\n200,xyz");

    CapturingTransport capturedTransport = created201Transport();
    Map<String, ServiceFactory> services = new LinkedHashMap<>(mockServices);
    services.put("Reconciliation", Reconciliation::new);
    Blnk uploadBlnk =
        new Blnk(apiKey, options, services, HttpClientUtils.FORMAT_RESPONSE, capturedTransport);

    ApiResponse<JsonNode> response =
        uploadBlnk.reconciliation().upload(filePath.toString(), "stripe");

    assertEquals(201, response.status());
    TransportRequest init = capturedTransport.calls.get(0).request();
    assertTrue(init.multipart());
    String payload = StreamTestUtils.readBody(init.body());
    assertTrue(payload.contains("stripe"));
    assertTrue(payload.contains("200,xyz"));
  }

  @Test
  @DisplayName("Returns success for 204 No Content without parsing JSON")
  void handles204() {
    BlnkTransport noContentTransport = (url, request) -> TransportResponse.builder()
        .ok(true)
        .status(204)
        .statusText("No Content")
        .json(() -> {
          throw new RuntimeException("Unexpected end of JSON input");
        })
        .text(() -> "")
        .build();

    Blnk noContentBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, noContentTransport);

    ApiResponse<JsonNode> response =
        noContentBlnk.request("api-keys/api_key_test_123", null, "DELETE", null);

    assertEquals(204, response.status());
    assertEquals("Success", response.message());
    assertNull(response.data());
  }

  @Test
  @DisplayName("Returns success for 200 OK with empty DELETE body")
  void handlesEmpty200() {
    BlnkTransport emptyBodyTransport = (url, request) -> TransportResponse.builder()
        .ok(true)
        .status(200)
        .statusText("OK")
        .json(() -> {
          throw new RuntimeException("Unexpected end of JSON input");
        })
        .text(() -> "")
        .build();

    Blnk emptyBodyBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, emptyBodyTransport);

    ApiResponse<JsonNode> response =
        emptyBodyBlnk.request("identities/idt_test_123", null, "DELETE", null);

    assertEquals(200, response.status());
    assertEquals("Success", response.message());
    assertNull(response.data());
  }

  @Test
  @DisplayName("Attaches error_detail.code on 409 Conflict")
  void attaches409ErrorDetail() {
    String body = "{\"error\":\"duplicate transaction reference\",\"error_detail\":"
        + "{\"code\":\"TXN_DUPLICATE_REFERENCE\",\"message\":\"duplicate transaction reference\"}}";
    BlnkTransport conflictTransport = (url, request) -> TransportResponse.builder()
        .ok(false)
        .status(409)
        .statusText("Conflict")
        .json(() -> BlnkJson.parse(body))
        .text(() -> body)
        .build();

    Blnk conflictBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, conflictTransport);

    ApiResponse<JsonNode> result =
        conflictBlnk.request("transactions", obj("reference", "dup_ref"), "POST", null);

    assertEquals(409, result.status());
    assertEquals(
        new BlnkApiErrorDetail("TXN_DUPLICATE_REFERENCE", "duplicate transaction reference"),
        result.error());
  }

  @Test
  @DisplayName("Attaches error_detail.code on 423 Locked")
  void attaches423ErrorDetail() {
    String body = "{\"error\":\"resource locked\",\"error_detail\":"
        + "{\"code\":\"GEN_LOCKED\",\"message\":\"resource locked\"}}";
    BlnkTransport lockedTransport = (url, request) -> TransportResponse.builder()
        .ok(false)
        .status(423)
        .statusText("Locked")
        .json(() -> BlnkJson.parse(body))
        .text(() -> body)
        .build();

    Blnk lockedBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, lockedTransport);

    ApiResponse<JsonNode> result = lockedBlnk.request("balances/bln_test", obj(), "PUT", null);

    assertEquals(423, result.status());
    assertEquals(new BlnkApiErrorDetail("GEN_LOCKED", "resource locked"), result.error());
  }

  @Test
  @DisplayName("request carries the configured timeout on each transport call")
  void requestCarriesTimeout() {
    // The configured timeout is carried on each individual transport call.
    CapturingTransport capturedTransport = CapturingTransport.of((url, request) ->
        TransportResponse.builder()
            .ok(true)
            .status(200)
            .statusText("OK")
            .json(() -> obj("message", "Success"))
            .build());
    Blnk signalBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, capturedTransport);

    signalBlnk.request("/test", obj("foo", "bar"), "POST", null);

    TransportRequest capturedInit = capturedTransport.calls.get(0).request();
    assertEquals(5000, capturedInit.timeoutMs());
  }

  @Test
  @DisplayName("request returns 408 when the transport aborts immediately")
  void immediateAbortReturns408() {
    BlnkTransport abortingTransport = (url, request) -> {
      throw new BlnkAbortException("The operation was aborted.");
    };

    Blnk abortBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, abortingTransport);

    ApiResponse<JsonNode> result = abortBlnk.request("/slow", obj("foo", "bar"), "POST", null);

    assertEquals(408, result.status());
    assertTrue(result.message().contains("timed out"));
    assertEquals("Request timed out after 5000ms", result.message());
  }

  @Test
  @DisplayName("request returns 408 when the transport aborts (timeout)")
  void timeoutAbortReturns408() {
    BlnkTransport abortingTransport = (url, request) -> {
      try {
        Thread.sleep(60_000); // never resolves within the timeout
      } catch (InterruptedException e) {
        throw new BlnkAbortException("The operation was aborted.", e);
      }
      return null;
    };

    Blnk timeoutBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").timeout(10)
            .logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        abortingTransport);

    ApiResponse<JsonNode> result = timeoutBlnk.request("/slow", obj("foo", "bar"), "POST", null);

    assertEquals(408, result.status());
    assertTrue(result.message().contains("timed out"));
    assertEquals("Request timed out after 10ms", result.message());
  }

  @Test
  @DisplayName("request method should make successful POST request and return formatted response")
  void successfulPostRequest() {
    ApiResponse<JsonNode> result = blnk.request("/test-endpoint", obj("foo", "bar"), "POST", null);

    assertEquals(
        new ApiResponse<>(200, "Success", BlnkJson.parse("{\"message\":\"Success\"}")),
        result,
        "Returns the expected ApiResponse");
  }

  @Test
  @DisplayName("request method should make failed request and return formatted response")
  void failedRequest() {
    Blnk badBlnkRequest = new Blnk(
        apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, TransportMocks.failTransport());
    ApiResponse<JsonNode> result =
        badBlnkRequest.request("/test-endpoint", obj("foo", "bar"), "POST", null);

    // The message is taken from the response statusText ("Failed"); data is
    // the parsed body; no structured error is attached because the body
    // carries no error_detail.
    assertEquals(
        new ApiResponse<>(500, "Failed", BlnkJson.parse("{\"message\":\"Failed\"}")),
        result,
        "Returns the expected ApiResponse");
  }

  @Test
  @DisplayName("defaults client timeout and retry options")
  void defaultsClientOptions() {
    Blnk defaultBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        thirdPartyRequest);
    assertEquals(10000, defaultBlnk.options.timeout());
    assertEquals(1, defaultBlnk.options.retryCount());
    assertEquals(2000, defaultBlnk.options.retryDelayMs());
  }

  @Test
  @DisplayName("request attaches structured error from error_detail")
  void attachesStructuredError() {
    // Mock has NO text body → exercises the json() fallback path.
    BlnkTransport errorTransport = (url, request) -> TransportResponse.builder()
        .ok(false)
        .status(404)
        .statusText("Not Found")
        .json(() -> BlnkJson.parse(
            "{\"error\":\"ledger not found\",\"error_detail\":"
                + "{\"code\":\"LGR_NOT_FOUND\",\"message\":\"ledger not found\"}}"))
        .build();
    Blnk errorBlnk =
        new Blnk(apiKey, options, mockServices, HttpClientUtils.FORMAT_RESPONSE, errorTransport);

    ApiResponse<JsonNode> result = errorBlnk.request("ledgers/missing", obj(), "GET", null);

    assertEquals(404, result.status());
    assertEquals(new BlnkApiErrorDetail("LGR_NOT_FOUND", "ledger not found"), result.error());
  }

  @Test
  @DisplayName("request retries retryable 5xx GET responses")
  void retriesRetryable5xxGet() {
    AtomicInteger calls = new AtomicInteger();
    BlnkTransport retryTransport = (url, request) -> {
      if (calls.incrementAndGet() == 1) {
        return TransportResponse.builder()
            .ok(false)
            .status(503)
            .statusText("Service Unavailable")
            .json(() -> obj("error", "temporarily unavailable"))
            .build();
      }
      return TransportResponse.builder()
          .ok(true)
          .status(200)
          .statusText("OK")
          .json(() -> obj("message", "Success"))
          .build();
    };

    Blnk retryBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").timeout(5000)
            .retryCount(3).retryDelayMs(1).logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        retryTransport);

    ApiResponse<JsonNode> result = retryBlnk.request("/retry-me", obj(), "GET", null);

    assertEquals(2, calls.get());
    assertEquals(200, result.status());
  }

  @Test
  @DisplayName("request does not retry mutating POST on 5xx")
  void doesNotRetryPostOn5xx() {
    AtomicInteger calls = new AtomicInteger();
    BlnkTransport postTransport = (url, request) -> {
      calls.incrementAndGet();
      return TransportResponse.builder()
          .ok(false)
          .status(503)
          .statusText("Service Unavailable")
          .json(() -> BlnkJson.parse(
              "{\"error\":\"temporarily unavailable\",\"error_detail\":"
                  + "{\"code\":\"GEN_INTERNAL\",\"message\":\"temporarily unavailable\"}}"))
          .build();
    };

    Blnk postBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").timeout(5000)
            .retryCount(3).retryDelayMs(1).logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        postTransport);

    ObjectNode data = BlnkJson.objectNode();
    data.put("amount", 100);
    ApiResponse<JsonNode> result = postBlnk.request("transactions", data, "POST", null);

    assertEquals(1, calls.get());
    assertEquals(503, result.status());
    assertEquals("GEN_INTERNAL", result.error().code());
  }

  @Test
  @DisplayName("request does not retry timeouts even when retryCount > 1")
  void doesNotRetryTimeouts() {
    AtomicInteger calls = new AtomicInteger();
    BlnkTransport timeoutTransport = (url, request) -> {
      calls.incrementAndGet();
      try {
        Thread.sleep(60_000);
      } catch (InterruptedException e) {
        throw new BlnkAbortException("The operation was aborted.", e);
      }
      return null;
    };

    Blnk timeoutBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").timeout(10)
            .retryCount(3).retryDelayMs(1).logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        timeoutTransport);

    ApiResponse<JsonNode> result = timeoutBlnk.request("/slow", obj(), "GET", null);

    assertEquals(1, calls.get());
    assertEquals(408, result.status());
  }

  @Test
  @DisplayName("request returns structured error after GET retries are exhausted")
  void structuredErrorAfterRetriesExhausted() {
    AtomicInteger calls = new AtomicInteger();
    BlnkTransport failingTransport = (url, request) -> {
      calls.incrementAndGet();
      return TransportResponse.builder()
          .ok(false)
          .status(503)
          .statusText("Service Unavailable")
          .json(() -> BlnkJson.parse(
              "{\"error\":\"still unavailable\",\"error_detail\":"
                  + "{\"code\":\"GEN_INTERNAL\",\"message\":\"still unavailable\"}}"))
          .build();
    };

    Blnk exhaustedBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").timeout(5000)
            .retryCount(2).retryDelayMs(1).logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        failingTransport);

    ApiResponse<JsonNode> result = exhaustedBlnk.request("/unstable", obj(), "GET", null);

    assertEquals(2, calls.get());
    assertEquals(503, result.status());
    assertEquals(new BlnkApiErrorDetail("GEN_INTERNAL", "still unavailable"), result.error());
  }

  @Test
  @DisplayName("retryCount below 1 is normalized to 1")
  void retryCountBelowOneNormalized() {
    Blnk normalizedBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").retryCount(0).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        thirdPartyRequest);

    assertEquals(1, normalizedBlnk.options.retryCount());
  }

  @Test
  @DisplayName("non-finite retry options are normalized on the client")
  void nonFiniteRetryOptionsNormalized() {
    Blnk normalizedBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder()
            .baseUrl("http://mock-api.com")
            .retryCount(Double.NaN)
            .retryDelayMs(Double.POSITIVE_INFINITY)
            .build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        thirdPartyRequest);

    assertEquals(1, normalizedBlnk.options.retryCount());
    assertEquals(2000, normalizedBlnk.options.retryDelayMs());
  }

  @Test
  @DisplayName("request does not retry 4xx responses")
  void doesNotRetry4xx() {
    AtomicInteger calls = new AtomicInteger();
    BlnkTransport clientErrorTransport = (url, request) -> {
      calls.incrementAndGet();
      return TransportResponse.builder()
          .ok(false)
          .status(400)
          .statusText("Bad Request")
          .json(() -> BlnkJson.parse(
              "{\"error\":\"bad request\",\"error_detail\":"
                  + "{\"code\":\"GEN_BAD_REQUEST\",\"message\":\"bad request\"}}"))
          .build();
    };

    Blnk noRetryBlnk = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("http://mock-api.com").timeout(5000)
            .retryCount(3).retryDelayMs(1).logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        clientErrorTransport);

    ApiResponse<JsonNode> result = noRetryBlnk.request("/bad", obj("foo", "bar"), "POST", null);

    assertEquals(1, calls.get());
    assertEquals(400, result.status());
    assertEquals("GEN_BAD_REQUEST", result.error().code());
  }

  @Test
  @DisplayName("should append \"/\" to base url if it is not set")
  void appendsSlashToBaseUrl() {
    Blnk blnkWithoutBaseUrl = new Blnk(
        apiKey,
        BlnkClientOptions.builder().baseUrl("base").timeout(5000).logger(options.logger()).build(),
        mockServices,
        HttpClientUtils.FORMAT_RESPONSE,
        thirdPartyRequest);
    assertEquals("base/", blnkWithoutBaseUrl.options.baseUrl());
  }
}
