package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Identity.tokenizeField}: request shape, validation, and error forwarding. */
@DisplayName("Identity.tokenizeField")
class IdentityTokenizeFieldTest {

  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("message", "Field tokenized successfully");
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  @Test
  @DisplayName("tokenizeField POSTs identities/{id}/tokenize/{field}")
  void tokenizeFieldPostsIdentitiesIdTokenizeField() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.tokenizeField("idt_test_123", "FirstName");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenize/FirstName", null, "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals("Field tokenized successfully", response.data().get("message").asText());
  }

  @Test
  @DisplayName("tokenizeField uses PascalCase struct field name in path")
  void tokenizeFieldUsesPascalCaseStructFieldNameInPath() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    identity.tokenizeField("idt_test_123", "EmailAddress");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenize/EmailAddress", null, "POST", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("tokenizeField rejects empty identity id")
  void tokenizeFieldRejectsEmptyIdentityId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.tokenizeField("", "FirstName");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("identity id is required", response.message());
  }

  @Test
  @DisplayName("tokenizeField rejects empty field name")
  void tokenizeFieldRejectsEmptyFieldName() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.tokenizeField("idt_test_123", "");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("field name is required", response.message());
  }

  @Test
  @DisplayName("tokenizeField forwards API errors")
  void tokenizeFieldForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(409, "Field already tokenized", null));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.tokenizeField("idt_test_123", "FirstName");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenize/FirstName", null, "POST", null)),
        capturedRequest.calls);
    assertEquals(409, response.status());
  }
}
