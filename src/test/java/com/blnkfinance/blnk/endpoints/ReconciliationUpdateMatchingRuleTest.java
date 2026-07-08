package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.Criteria;
import com.blnkfinance.blnk.types.Matcher;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@code Reconciliation.updateMatchingRule}: request shape, validation, and error forwarding. */
class ReconciliationUpdateMatchingRuleTest {

  /**
   * Returns a valid {@link Matcher} payload shared across tests. The second
   * criterion has no allowable_drift, so that key must be absent from the wire JSON.
   */
  private static Matcher validData() {
    return Matcher.create()
        .name("Updated matcher")
        .description("Amount with 2% drift matcher")
        .criteria(
            List.of(
                Criteria.create().field("amount").operator("equals").allowableDrift(0.02),
                Criteria.create().field("currency").operator("equals")));
  }

  /** Returns the valid payload echoed back with a rule id and server timestamps. */
  private static ObjectNode mockResponse() {
    ObjectNode node = validData().toJson();
    node.put("rule_id", "rule_test_123");
    node.put("created_at", "2026-06-12T04:31:55.613241Z");
    node.put("updated_at", "2026-06-12T04:31:55.715443261Z");
    return node;
  }

  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  private static Reconciliation reconciliation(CapturingRequest capturedRequest) {
    return new Reconciliation(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Reconciliation.updateMatchingRule")
  class UpdateMatchingRule {

    @Test
    @DisplayName("updateMatchingRule PUTs reconciliation/matching-rules/{id}")
    void putsMatchingRule() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));
      Matcher data = validData();

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).updateMatchingRule("rule_test_123", data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/matching-rules/rule_test_123", data.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals("rule_test_123", response.data().get("rule_id").asText());
      assertEquals("Updated matcher", response.data().get("name").asText());
    }

    @Test
    @DisplayName("updateMatchingRule rejects empty rule id")
    void rejectsEmptyRuleId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).updateMatchingRule("", validData());

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("matching rule id is required", response.message());
    }

    @Test
    @DisplayName("updateMatchingRule returns 400 for invalid payload")
    void returns400ForInvalidPayload() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));
      Matcher invalid =
          validData()
              .criteria(List.of(Criteria.create().field("invalid").operator("equals")));

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).updateMatchingRule("rule_test_123", invalid);

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      // The message must reference the offending criteria, whatever its exact wording.
      String message = response.message().toLowerCase(Locale.ROOT);
      assertTrue(message.contains("criterion") || message.contains("criteria"));
    }

    @Test
    @DisplayName("updateMatchingRule forwards API errors")
    void forwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(404, "Matching rule not found", null));
      Matcher data = validData();

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).updateMatchingRule("rule_missing", data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/matching-rules/rule_missing", data.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
    }
  }
}
