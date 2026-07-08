package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.MonitorCondition;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for the BalanceMonitor service: create, get, list, and update flows. */
class BalanceMonitorsTest {

  @Nested
  @DisplayName("POST BalanceMonitor")
  class PostBalanceMonitor {

    @Test
    @DisplayName("Create Balance Monitor")
    void createBalanceMonitor() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
      String id = "1234567890";
      MonitorData data =
          MonitorData.create()
              .balanceId(id)
              .condition(
                  MonitorCondition.create()
                      .field("debit_balance")
                      .operator("<")
                      .value(500)
                      .precision(100));

      ApiResponse<JsonNode> response = balanceMonitor.create(data);

      assertEquals(201, response.status());
      assertEquals(
          data.toJson().get("balance_id").asText(), response.data().get("balance_id").asText());
      assertEquals(
          List.of(new CapturingRequest.Call("balance-monitors", data.toJson(), "POST", null)),
          capturedRequest.calls);
    }

    @Test
    @DisplayName("It should handle missing fields")
    void itShouldHandleMissingFields() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
      // Missing balance_id — fails validation before the request is made.
      MonitorData data =
          MonitorData.create()
              .condition(
                  MonitorCondition.create()
                      .field("debit_balance")
                      .operator("<")
                      .value(500)
                      .precision(100));

      ApiResponse<JsonNode> response = balanceMonitor.create(data);

      assertEquals(400, response.status());
      assertNull(response.data());
      assertEquals(List.of(), capturedRequest.calls);
    }

    @Test
    @DisplayName("It should handle invalid fields")
    void itShouldHandleInvalidFields() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
      String id = "1234567890";
      // condition.value is a string — fails the condition check.
      MonitorData data =
          MonitorData.create()
              .balanceId(id)
              .condition(
                  MonitorCondition.create()
                      .field("debit_balance")
                      .operator("<")
                      .value("500")
                      .precision(100));

      ApiResponse<JsonNode> response = balanceMonitor.create(data);

      assertEquals(400, response.status());
      assertNull(response.data());
      assertEquals(List.of(), capturedRequest.calls);
    }
  }

  @Nested
  @DisplayName("GET BalanceMonitor")
  class GetBalanceMonitorParent {

    @Test
    @DisplayName("Get Balance Monitor")
    void getBalanceMonitor() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
      String id = "1234567890";

      ApiResponse<JsonNode> response = balanceMonitor.get(id);

      assertEquals(200, response.status());
      assertEquals(
          List.of(new CapturingRequest.Call("balance-monitors/" + id, null, "GET", null)),
          capturedRequest.calls);
    }

    @Test
    @DisplayName("List Balance Monitors")
    void listBalanceMonitors() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

      ApiResponse<JsonNode> response = balanceMonitor.list();

      assertEquals(200, response.status());
      assertEquals(
          List.of(new CapturingRequest.Call("balance-monitors", null, "GET", null)),
          capturedRequest.calls);
    }
  }

  @Nested
  @DisplayName("PUT BalanceMonitor")
  class PutBalanceMonitor {

    @Test
    @DisplayName("It should update a balance monitor")
    void itShouldUpdateABalanceMonitor() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
      String id = "12345678";
      MonitorData data =
          MonitorData.create()
              .balanceId(id)
              .condition(
                  MonitorCondition.create()
                      .field("debit_balance")
                      .operator("<")
                      .value(500)
                      .precision(100));

      ApiResponse<JsonNode> response = balanceMonitor.update(id, data);

      assertEquals(200, response.status());
      assertEquals(
          data.toJson().get("balance_id").asText(), response.data().get("balance_id").asText());
      assertEquals(
          List.of(
              new CapturingRequest.Call("balance-monitors/" + id, data.toJson(), "PUT", null)),
          capturedRequest.calls);
    }

    @Test
    @DisplayName("It should handle missing fields")
    void itShouldHandleMissingFields() {
      BlnkLogger mockLogger = TestMocks.createMockLogger();
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      BalanceMonitor balanceMonitor =
          new BalanceMonitor(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
      String id = "1234567890";
      // Missing balance_id — fails validation before the request is made.
      MonitorData data =
          MonitorData.create()
              .condition(
                  MonitorCondition.create()
                      .field("debit_balance")
                      .operator("<")
                      .value(500)
                      .precision(100));

      ApiResponse<JsonNode> response = balanceMonitor.update(id, data);

      assertEquals(400, response.status());
      assertNull(response.data());
      assertEquals(List.of(), capturedRequest.calls);
    }
  }
}
