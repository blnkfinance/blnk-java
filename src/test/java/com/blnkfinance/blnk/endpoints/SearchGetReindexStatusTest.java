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

/** Unit tests for {@code Search.getReindexStatus}: request shape and error forwarding. */
@DisplayName("Search.getReindexStatus")
class SearchGetReindexStatusTest {

  @Test
  @DisplayName("getReindexStatus GETs search/reindex")
  void getReindexStatusGetsSearchReindex() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    ObjectNode progress = BlnkJson.objectNode();
    progress.put("status", "in_progress");
    progress.put("phase", "indexing_transactions");
    progress.put("total_records", 100);
    progress.put("processed_records", 50);
    progress.put("started_at", "2026-06-12T02:03:08.81867638Z");
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", progress);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = search.getReindexStatus();

    assertEquals(
        List.of(new CapturingRequest.Call("search/reindex", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals("in_progress", response.data().get("status").asText());
    assertEquals("indexing_transactions", response.data().get("phase").asText());
  }

  @Test
  @DisplayName("getReindexStatus forwards API errors")
  void getReindexStatusForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) ->
            new ApiResponse<>(404, "No reindex operation has been started", null);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = search.getReindexStatus();

    assertEquals(
        List.of(new CapturingRequest.Call("search/reindex", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(404, response.status());
  }
}
