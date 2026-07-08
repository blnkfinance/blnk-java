package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.Criteria;
import com.blnkfinance.blnk.types.Matcher;
import com.blnkfinance.blnk.types.RunReconData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.blnkfinance.blnk.util.MultipartBody;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Unit tests for the Reconciliation service: matching rules, file uploads, and runs.
 * The {@code FILE_PATH} fixture resolves to the committed repo-root {@code file.csv};
 * surefire runs with the project base as the working directory.
 */
class ReconciliationTest {

  private static final String FILE_PATH = "file.csv";

  private static Reconciliation reconciliation(CapturingRequest capturedRequest) {
    return new Reconciliation(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Reconciliation")
  class ReconciliationGroup {

    @Test
    @DisplayName("Create Matching Rule")
    void createMatchingRule() {
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Matcher data =
          Matcher.create()
              .criteria(
                  List.of(
                      Criteria.create()
                          .field("amount")
                          .operator("equals")
                          .allowableDrift(0.01)))
              .description("Test Matching Rule")
              .name("Test Matching Rule");

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).createMatchingRule(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "reconciliation/matching-rules", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, response.status());
    }

    @Test
    @DisplayName("should upload file successfully when given a valid file path")
    void uploadValidFilePath() {
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).upload(FILE_PATH, "Stripe");

      // The upload must be sent as a multipart body, not plain JSON.
      assertInstanceOf(MultipartBody.class, capturedRequest.calls.get(0).data());
      assertEquals(201, response.status());
    }

    @Test
    @DisplayName("should handle error gracefully when given an invalid file path")
    void uploadInvalidFilePath() {
      String filePath = "./no_file.csv";
      // A missing local file is reported as a 404 response without any HTTP call
      // being made; the stub below would throw if it were ever invoked.
      BlnkRequest thirdPartyRequest =
          TestMocks.createMockBlnkRequest(
              false, "File does not exist at path: " + filePath, 404);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);

      ApiResponse<JsonNode> response =
          reconciliation(capturedRequest).upload(filePath, "Stripe");

      assertEquals(404, response.status());
    }

    @Test
    @DisplayName("should upload file successfully when given a valid Readstream")
    void uploadValidReadStream() throws IOException {
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);

      try (InputStream readStream = new FileInputStream(FILE_PATH)) {
        ApiResponse<JsonNode> response =
            reconciliation(capturedRequest).upload(readStream, "Stripe");

        assertInstanceOf(MultipartBody.class, capturedRequest.calls.get(0).data());
        assertEquals(201, response.status());
      }
    }
  }

  @Nested
  @DisplayName("Run Reconciliation")
  class RunReconciliation {

    private RunReconData data() {
      return RunReconData.create()
          .uploadId("0987654321")
          .matchingRuleIds(List.of("1233455555"))
          .dryRun(false)
          .strategy("one_to_many")
          .groupingCriteria("amount");
    }

    @Test
    @DisplayName("Start Reconciliation")
    void startReconciliation() {
      BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      RunReconData data = data();

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).run(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call("reconciliation/start", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("should handle thrown errors gracefully")
    void handlesThrownErrors() {
      BlnkRequest thirdPartyRequest =
          TestMocks.createMockBlnkRequest(false, "An error occurred", 500);
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);

      ApiResponse<JsonNode> response = reconciliation(capturedRequest).run(data());

      assertEquals(500, response.status());
    }
  }
}
