package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.UpdateLedger;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for the Ledgers service: create and update flows with validation and error handling. */
@DisplayName("Ledger Tests")
class LedgersTest {

  private static final BlnkLogger mockLogger = TestMocks.createMockLogger();

  private BlnkRequest thirdPartyRequest;

  @BeforeEach
  void beforeEach() {
    thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
  }

  @Test
  @DisplayName("Creates a ledger with valid data")
  void createsALedgerWithValidData() {
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledger =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    CreateLedger data =
        CreateLedger.create()
            .name("My Ledger")
            .metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledger.create(data);

    assertEquals(
        List.of(new CapturingRequest.Call("ledgers", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
    assertEquals(data.toJson().get("name").asText(), response.data().get("name").asText());
  }

  @Test
  @DisplayName("it should handle missing required fields")
  void itShouldHandleMissingRequiredFields() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledgers =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    // Simulates a caller omitting the required name field.
    CreateLedger data =
        CreateLedger.create().metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledgers.create(data);

    // Request won't get called since this fails validation.
    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status(), "Response is 400");
    assertNull(response.data());
  }

  @Test
  @DisplayName("it should handle thrown errors gracefully")
  void itShouldHandleThrownErrorsGracefully() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, "Network Error");
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledgerBalance =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    CreateLedger data =
        CreateLedger.create()
            .name("Test Ledger")
            .metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    // The call is recorded even though the request threw.
    assertEquals(
        List.of(new CapturingRequest.Call("ledgers", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(500, response.status(), "Response is 500");
    assertNull(response.data());
    assertEquals("Network Error", response.message());
  }

  @Test
  @DisplayName("update calls PUT /ledgers/{id}")
  void updateCallsPutLedgersId() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledgers =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    String ledgerId = "ldg_073f7ffe-9dfd-42ce-aa50-d1dca1788adc";
    UpdateLedger data = UpdateLedger.create().name("Updated Customer Savings Account");

    ApiResponse<JsonNode> response = ledgers.update(ledgerId, data);

    assertEquals(
        List.of(new CapturingRequest.Call("ledgers/" + ledgerId, data.toJson(), "PUT", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals(data.toJson().get("name").asText(), response.data().get("name").asText());
  }

  @Test
  @DisplayName("update rejects empty ledger id")
  void updateRejectsEmptyLedgerId() {
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledgers =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        ledgers.update("", UpdateLedger.create().name("Updated Name"));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("ledger id is required", response.message());
  }

  @Test
  @DisplayName("update rejects missing name")
  void updateRejectsMissingName() {
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledgers =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = ledgers.update("ldg_123", UpdateLedger.create());

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertNull(response.data());
  }

  @Test
  @DisplayName("update handles thrown errors gracefully")
  void updateHandlesThrownErrorsGracefully() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, "Network Error");
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Ledgers ledgers =
        new Ledgers(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    UpdateLedger data = UpdateLedger.create().name("Updated Name");

    ApiResponse<JsonNode> response = ledgers.update("ldg_123", data);

    assertEquals(
        List.of(new CapturingRequest.Call("ledgers/ldg_123", data.toJson(), "PUT", null)),
        capturedRequest.calls);
    assertEquals(500, response.status());
    assertEquals("Network Error", response.message());
  }
}
