package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.DetokenizeIdentityData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Identity.detokenize}: request shape, validation, and error forwarding. */
@DisplayName("Identity.detokenize")
class IdentityDetokenizeTest {

  private static DetokenizeIdentityData validData() {
    return DetokenizeIdentityData.create().fields(List.of("FirstName", "EmailAddress"));
  }

  private static ObjectNode mockResponse() {
    ObjectNode fields = BlnkJson.objectNode();
    fields.put("FirstName", "Jane");
    fields.put("EmailAddress", "jane@example.com");
    ObjectNode node = BlnkJson.objectNode();
    node.set("fields", fields);
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  @Test
  @DisplayName("detokenize POSTs identities/{id}/detokenize")
  void detokenizePostsIdentitiesIdDetokenize() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    DetokenizeIdentityData validData = validData();

    ApiResponse<JsonNode> response = identity.detokenize("idt_test_123", validData);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/detokenize", validData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals("Jane", response.data().get("fields").get("FirstName").asText());
    assertEquals(
        "jane@example.com", response.data().get("fields").get("EmailAddress").asText());
  }

  @Test
  @DisplayName("detokenize allows empty fields to detokenize all")
  void detokenizeAllowsEmptyFieldsToDetokenizeAll() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    DetokenizeIdentityData emptyFieldsData = DetokenizeIdentityData.create().fields(List.of());
    ObjectNode allFields = BlnkJson.objectNode();
    allFields.put("FirstName", "Jane");
    allFields.put("EmailAddress", "jane@example.com");
    allFields.put("PhoneNumber", "+1234567890");
    ObjectNode allFieldsResponse = BlnkJson.objectNode();
    allFieldsResponse.set("fields", allFields);
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", allFieldsResponse));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.detokenize("idt_test_123", emptyFieldsData);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/detokenize", emptyFieldsData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals(3, response.data().get("fields").size());
  }

  @Test
  @DisplayName("detokenize rejects empty identity id")
  void detokenizeRejectsEmptyIdentityId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.detokenize("", validData());

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("identity id is required", response.message());
  }

  @Test
  @DisplayName("detokenize rejects blank field names")
  void detokenizeRejectsBlankFieldNames() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        identity.detokenize(
            "idt_test_123", DetokenizeIdentityData.create().fields(List.of("FirstName", "")));

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("each field must be a non-empty string", response.message());
  }

  @Test
  @DisplayName("detokenize forwards API errors")
  void detokenizeForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(400, "Field is not tokenized", null));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    DetokenizeIdentityData data = DetokenizeIdentityData.create().fields(List.of("Street"));

    ApiResponse<JsonNode> response = identity.detokenize("idt_test_123", data);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/detokenize", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(400, response.status());
  }
}
