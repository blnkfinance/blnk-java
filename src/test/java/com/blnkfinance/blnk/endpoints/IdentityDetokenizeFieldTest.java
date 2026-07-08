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

/** Unit tests for {@code Identity.detokenizeField}: request shape, validation, and error forwarding. */
@DisplayName("Identity.detokenizeField")
class IdentityDetokenizeFieldTest {

  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("field", "EmailAddress");
    node.put("value", "jane@example.com");
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  @Test
  @DisplayName("detokenizeField GETs identities/{id}/detokenize/{field}")
  void detokenizeFieldGetsIdentitiesIdDetokenizeField() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        identity.detokenizeField("idt_test_123", "EmailAddress");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/detokenize/EmailAddress", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals("EmailAddress", response.data().get("field").asText());
    assertEquals("jane@example.com", response.data().get("value").asText());
  }

  @Test
  @DisplayName("detokenizeField uses PascalCase struct field name in path")
  void detokenizeFieldUsesPascalCaseStructFieldNameInPath() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    ObjectNode firstNameResponse = BlnkJson.objectNode();
    firstNameResponse.put("field", "FirstName");
    firstNameResponse.put("value", "Jane");
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", firstNameResponse));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    identity.detokenizeField("idt_test_123", "FirstName");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/detokenize/FirstName", null, "GET", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("detokenizeField rejects empty identity id")
  void detokenizeFieldRejectsEmptyIdentityId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.detokenizeField("", "EmailAddress");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("identity id is required", response.message());
  }

  @Test
  @DisplayName("detokenizeField rejects empty field name")
  void detokenizeFieldRejectsEmptyFieldName() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.detokenizeField("idt_test_123", "");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("field name is required", response.message());
  }

  @Test
  @DisplayName("detokenizeField forwards API errors")
  void detokenizeFieldForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(400, "Field is not tokenized", null));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        identity.detokenizeField("idt_test_123", "PhoneNumber");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/detokenize/PhoneNumber", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(400, response.status());
  }
}
