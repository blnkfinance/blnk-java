package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the BlnkSystem service: the health endpoint and its error handling. */
@DisplayName("GET system health")
class SystemTest {

  private static final BlnkLogger mockLogger = TestMocks.createMockLogger();

  private BlnkRequest thirdPartyRequest;

  @BeforeEach
  void beforeEach() {
    thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
  }

  @Test
  @DisplayName("health calls GET /health")
  void healthCallsGetHealth() {
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    BlnkSystem system =
        new BlnkSystem(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = system.health();

    assertEquals(
        List.of(new CapturingRequest.Call("health", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("health handles thrown errors")
  void healthHandlesThrownErrors() {
    BlnkRequest errorRequest = TestMocks.createMockBlnkRequest(false, "Network error occurred");
    CapturingRequest capturedRequest = CapturingRequest.of(errorRequest);
    BlnkSystem system =
        new BlnkSystem(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = system.health();

    // The call IS recorded before the mock throws.
    assertEquals(
        List.of(new CapturingRequest.Call("health", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(500, response.status());
    assertEquals("Network error occurred", response.message());
  }
}
