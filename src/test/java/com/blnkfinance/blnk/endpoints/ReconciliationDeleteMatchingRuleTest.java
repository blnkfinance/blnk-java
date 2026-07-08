package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Reconciliation.deleteMatchingRule}: request shape, validation, and error forwarding. */
class ReconciliationDeleteMatchingRuleTest {

  /** Returns the delete-confirmation body shared across tests. */
  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("message", "Matching rule deleted successfully");
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  private static Reconciliation reconciliation(CapturingRequest capturedRequest) {
    return new Reconciliation(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Reconciliation.deleteMatchingRule")
  class DeleteMatchingRule {

    @Test
    @DisplayName("deleteMatchingRule DELETEs reconciliation/matching-rules/{id}")
    void deletesMatchingRule() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).deleteMatchingRule("rule_test_123");

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/matching-rules/rule_test_123", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals(
          "Matching rule deleted successfully", response.data().get("message").asText());
    }

    @Test
    @DisplayName("deleteMatchingRule rejects empty rule id")
    void rejectsEmptyRuleId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).deleteMatchingRule("");

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("matching rule id is required", response.message());
    }

    @Test
    @DisplayName("deleteMatchingRule forwards API errors")
    void forwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(404, "Matching rule not found", null));

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).deleteMatchingRule("rule_missing");

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/matching-rules/rule_missing", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
    }
  }
}
