package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.RunReconData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Reconciliation.run}: request shape and error forwarding. */
class ReconciliationRunTest {

  /** Returns a valid {@link RunReconData} payload shared across tests. */
  private static RunReconData validData() {
    return RunReconData.create()
        .uploadId("upload_test_123")
        .matchingRuleIds(List.of("rule_test_123"))
        .dryRun(true)
        .strategy("one_to_one")
        .groupingCriteria("amount");
  }

  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  private static Reconciliation reconciliation(CapturingRequest capturedRequest) {
    return new Reconciliation(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Reconciliation.run")
  class Run {

    @Test
    @DisplayName("run POSTs reconciliation/start")
    void runPostsReconciliationStart() {
      ObjectNode mockData = BlnkJson.objectNode();
      mockData.put("reconciliation_id", "recon_test_123");
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockData));
      RunReconData data = validData();

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).run(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call("reconciliation/start", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals("recon_test_123", response.data().get("reconciliation_id").asText());
      // The response body carries exactly one field.
      assertEquals(1, response.data().size());
    }

    @Test
    @DisplayName("run forwards API errors")
    void forwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(400, "matching_rule_ids is required", null));
      RunReconData data = validData();

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).run(data);

      // Proves run does NOT validate locally and forwards the server's 400.
      assertEquals(
          List.of(
              new CapturingRequest.Call("reconciliation/start", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(400, response.status());
    }
  }
}
