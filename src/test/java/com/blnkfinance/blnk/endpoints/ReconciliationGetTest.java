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

/** Unit tests for {@code Reconciliation.get}: request shape, validation, and error forwarding. */
class ReconciliationGetTest {

  /** Returns a representative reconciliation response body shared across tests. */
  private static ObjectNode mockReconciliation() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("reconciliation_id", "recon_test_123");
    node.put("upload_id", "instant_abc");
    node.put("status", "completed");
    node.put("matched_transactions", 2);
    node.put("unmatched_transactions", 1);
    node.put("is_dry_run", true);
    node.put("started_at", "2026-06-12T04:23:48.196087Z");
    node.put("completed_at", "2026-06-12T04:23:49.196087Z");
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
  @DisplayName("Reconciliation.get")
  class Get {

    @Test
    @DisplayName("get calls reconciliation/{id}")
    void getCallsReconciliationId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockReconciliation()));

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).get("recon_test_123");

      assertEquals(
          List.of(
              new CapturingRequest.Call("reconciliation/recon_test_123", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals("recon_test_123", response.data().get("reconciliation_id").asText());
      assertEquals("completed", response.data().get("status").asText());
    }

    @Test
    @DisplayName("get rejects empty reconciliation id")
    void rejectsEmptyReconciliationId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockReconciliation()));

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).get("");

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("reconciliation id is required", response.message());
    }

    @Test
    @DisplayName("get forwards API errors")
    void forwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(404, "Reconciliation not found", null));

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).get("recon_missing");

      assertEquals(
          List.of(new CapturingRequest.Call("reconciliation/recon_missing", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
    }
  }
}
