package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Identity.getTokenizedFields}: request shape, validation, and error forwarding. */
@DisplayName("Identity.getTokenizedFields")
class IdentityGetTokenizedFieldsTest {

  private static ArrayNode tokenizedFields() {
    return BlnkJson.arrayNode()
        .add("FirstName")
        .add("LastName")
        .add("EmailAddress")
        .add("PhoneNumber");
  }

  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.set("tokenized_fields", tokenizedFields());
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  @Test
  @DisplayName("getTokenizedFields GETs identities/{id}/tokenized-fields")
  void getTokenizedFieldsGetsIdentitiesIdTokenizedFields() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.getTokenizedFields("idt_test_123");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenized-fields", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals(tokenizedFields(), response.data().get("tokenized_fields"));
  }

  @Test
  @DisplayName("getTokenizedFields returns empty list for fresh identity")
  void getTokenizedFieldsReturnsEmptyListForFreshIdentity() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    ObjectNode emptyResponse = BlnkJson.objectNode();
    emptyResponse.set("tokenized_fields", BlnkJson.arrayNode());
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", emptyResponse));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.getTokenizedFields("idt_test_123");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_test_123/tokenized-fields", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals(BlnkJson.arrayNode(), response.data().get("tokenized_fields"));
  }

  @Test
  @DisplayName("getTokenizedFields rejects empty identity id")
  void getTokenizedFieldsRejectsEmptyIdentityId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(200, "Success", mockResponse()));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.getTokenizedFields("");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("identity id is required", response.message());
  }

  @Test
  @DisplayName("getTokenizedFields forwards API errors")
  void getTokenizedFieldsForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    CapturingRequest capturedRequest =
        CapturingRequest.of(respond(404, "Identity not found", null));
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = identity.getTokenizedFields("idt_missing");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities/idt_missing/tokenized-fields", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(404, response.status());
  }
}
