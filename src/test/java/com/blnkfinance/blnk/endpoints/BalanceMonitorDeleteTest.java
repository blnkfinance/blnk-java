package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.BlnkTransport;
import com.blnkfinance.blnk.ServiceFactory;
import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for {@code BalanceMonitor.delete}: request shape, validation, and error forwarding. */
@DisplayName("BalanceMonitor.delete")
class BalanceMonitorDeleteTest {

  /** Delete-confirmation body shared across tests. */
  private static final ObjectNode deleteResponse = buildDeleteResponse();

  private static ObjectNode buildDeleteResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("message", "BalanceMonitor deleted successfully");
    return node;
  }

  @Test
  @DisplayName("delete DELETEs balance-monitors/{id}")
  void deleteDeletesBalanceMonitorsId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", deleteResponse);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    BalanceMonitor balanceMonitor =
        new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = balanceMonitor.delete("mon_test_123");

    assertEquals(
        List.of(new CapturingRequest.Call("balance-monitors/mon_test_123", null, "DELETE", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals("BalanceMonitor deleted successfully", response.data().get("message").asText());
  }

  @Test
  @DisplayName("delete URL-encodes monitor id")
  void deleteUrlEncodesMonitorId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", deleteResponse);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    BalanceMonitor balanceMonitor =
        new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    balanceMonitor.delete("mon_test/special");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balance-monitors/mon_test%2Fspecial", null, "DELETE", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("delete returns 400 for empty id")
  void deleteReturns400ForEmptyId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", deleteResponse);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    BalanceMonitor balanceMonitor =
        new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = balanceMonitor.delete("");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("monitor id is required", response.message());
  }

  @Test
  @DisplayName("delete forwards API errors")
  void deleteForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) ->
            new ApiResponse<>(404, "Balance monitor not found", null);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    BalanceMonitor balanceMonitor =
        new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = balanceMonitor.delete("mon_missing");

    assertEquals(
        List.of(new CapturingRequest.Call("balance-monitors/mon_missing", null, "DELETE", null)),
        capturedRequest.calls);
    assertEquals(404, response.status());
  }

  @Test
  @DisplayName("Delete succeeds on 200 OK with empty body")
  void deleteSucceedsOn200OkWithEmptyBody() {
    // Full-client test: transport resolves ok/200/"OK" with an empty text body;
    // json() throws (must not be reached — text() wins).
    BlnkTransport emptyBodyTransport = (url, request) -> TransportResponse.builder()
        .ok(true)
        .status(200)
        .statusText("OK")
        .json(() -> {
          throw new RuntimeException("Unexpected end of JSON input");
        })
        .text(() -> "")
        .build();

    Map<String, ServiceFactory> services = new LinkedHashMap<>();
    services.put("BalanceMonitor", BalanceMonitor::new);
    Blnk blnk =
        new Blnk(
            "test-key",
            TestMocks.createMockBlnkClientOptions(),
            services,
            HttpClientUtils.FORMAT_RESPONSE,
            emptyBodyTransport);

    ApiResponse<JsonNode> response = blnk.balanceMonitor().delete("mon_test_123");

    assertEquals(200, response.status());
    assertEquals("Success", response.message());
    assertNull(response.data());
  }
}
