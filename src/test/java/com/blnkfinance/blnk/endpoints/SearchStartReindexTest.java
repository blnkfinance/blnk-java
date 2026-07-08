package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.StartReindexRequest;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Search.startReindex}: request body construction and validation. */
@DisplayName("Search.startReindex")
class SearchStartReindexTest {

  @Test
  @DisplayName("startReindex posts to search/reindex with empty body")
  void startReindexPostsEmptyBody() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 202);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = search.startReindex();

    assertEquals(
        List.of(
            new CapturingRequest.Call("search/reindex", BlnkJson.objectNode(), "POST", null)),
        capturedRequest.calls);
    assertEquals(202, response.status());
  }

  @Test
  @DisplayName("startReindex forwards batch_size")
  void startReindexForwardsBatchSize() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 202);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.startReindex(StartReindexRequest.create().batchSize(500));

    ObjectNode expectedBody = BlnkJson.objectNode();
    expectedBody.put("batch_size", 500L); // BlnkJson integral numbers are long nodes
    assertEquals(
        List.of(new CapturingRequest.Call("search/reindex", expectedBody, "POST", null)),
        capturedRequest.calls);
    assertEquals(202, response.status());
  }

  @Test
  @DisplayName("startReindex rejects invalid batch_size")
  void startReindexRejectsInvalidBatchSize() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.startReindex(StartReindexRequest.create().batchSize(0));

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("batch_size must be a positive integer if provided", response.message());
  }
}
