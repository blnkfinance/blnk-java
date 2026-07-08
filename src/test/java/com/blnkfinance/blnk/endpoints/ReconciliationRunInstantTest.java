package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.ExternalTransaction;
import com.blnkfinance.blnk.types.RunInstantReconData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@code Reconciliation.runInstant}: request shape, validation, and error forwarding. */
class ReconciliationRunInstantTest {

  /** Returns a valid {@link RunInstantReconData} payload shared across tests. */
  private static RunInstantReconData validData() {
    return RunInstantReconData.create()
        .externalTransactions(
            List.of(
                ExternalTransaction.create()
                    .id("txn_1")
                    .amount(5.49)
                    .reference("INV-2023-002")
                    .currency("GBP")
                    .description("Card payment")
                    .date("2024-11-15T14:25:30Z")
                    .source("bank-api")))
        .strategy("one_to_one")
        .dryRun(true)
        .matchingRuleIds(List.of("rule_abc123"));
  }

  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  private static Reconciliation reconciliation(CapturingRequest capturedRequest) {
    return new Reconciliation(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Reconciliation.runInstant")
  class RunInstant {

    @Test
    @DisplayName("runInstant POSTs reconciliation/start-instant")
    void runInstantPostsStartInstant() {
      ObjectNode mockData = BlnkJson.objectNode();
      mockData.put("reconciliation_id", "rec_test_123");
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockData));
      RunInstantReconData data = validData();

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).runInstant(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/start-instant", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals("rec_test_123", response.data().get("reconciliation_id").asText());
    }

    @Test
    @DisplayName("runInstant returns 400 for invalid payload")
    void returns400ForInvalidPayload() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.objectNode()));

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest)
              .runInstant(validData().externalTransactions(List.of()));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertTrue(response.message().contains("external_transactions"));
    }

    @Test
    @DisplayName("runInstant forwards API errors")
    void forwardsApiErrors() {
      // The stub returns a 500 response rather than throwing.
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(500, "Failed to start instant reconciliation", null));
      RunInstantReconData data = validData();

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).runInstant(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/start-instant", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(500, response.status());
    }
  }
}
