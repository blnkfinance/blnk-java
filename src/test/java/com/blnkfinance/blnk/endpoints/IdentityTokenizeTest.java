package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.TokenizeIdentityData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Identity.tokenize}: request shape, validation, and error forwarding. */
@DisplayName("Identity.tokenize")
class IdentityTokenizeTest {

  private static TokenizeIdentityData validData() {
    return TokenizeIdentityData.create().fields(List.of("FirstName", "EmailAddress"));
  }

  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("message", "Fields tokenized successfully");
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  @Test
  @DisplayName("tokenize uses PascalCase struct field names")
  void tokenizeUsesPascalCaseStructFieldNames() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    TokenizeIdentityData pascalCaseData =
        TokenizeIdentityData.create()
            .fields(List.of("FirstName", "LastName", "EmailAddress", "PhoneNumber"));
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.tokenize("idt_test_123", pascalCaseData);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenize", pascalCaseData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("tokenize POSTs identities/{id}/tokenize")
  void tokenizePostsIdentitiesIdTokenize() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    TokenizeIdentityData validData = validData();

    ApiResponse<JsonNode> response = identity.tokenize("idt_test_123", validData);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenize", validData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals("Fields tokenized successfully", response.data().get("message").asText());
  }

  @Test
  @DisplayName("tokenize rejects empty identity id")
  void tokenizeRejectsEmptyIdentityId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.tokenize("", validData());

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("identity id is required", response.message());
  }

  @Test
  @DisplayName("tokenize rejects empty fields")
  void tokenizeRejectsEmptyFields() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        identity.tokenize("idt_test_123", TokenizeIdentityData.create().fields(List.of()));

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("at least one field must be specified", response.message());
  }

  @Test
  @DisplayName("tokenize forwards API errors")
  void tokenizeForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(404, "Identity not found", null));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    TokenizeIdentityData validData = validData();

    ApiResponse<JsonNode> response = identity.tokenize("idt_missing", validData);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_missing/tokenize", validData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(404, response.status());
  }
}
