package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.FilterCondition;
import com.blnkfinance.blnk.types.FilterParams;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Search.filter}: collection routing and filter validation. */
@DisplayName("Search.filter")
class SearchFilterTest {

  @Test
  @DisplayName("filter forwards transactions collection")
  void filterForwardsTransactionsCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    FilterParams params =
        FilterParams.create()
            .filters(
                List.of(
                    FilterCondition.create().field("status").operator("eq").value("APPLIED")))
            .logicalOperator("and")
            .sortBy("created_at")
            .sortOrder("desc")
            .includeCount(true)
            .limit(20)
            .offset(0);

    ApiResponse<JsonNode> response = search.filter(params, "transactions");

    assertEquals(
        List.of(
            new CapturingRequest.Call("transactions/filter", params.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("filter forwards ledgers collection")
  void filterForwardsLedgersCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    FilterParams params =
        FilterParams.create()
            .filters(
                List.of(
                    FilterCondition.create().field("name").operator("like").value("%General%")));

    ApiResponse<JsonNode> response = search.filter(params, "ledgers");

    assertEquals(
        List.of(new CapturingRequest.Call("ledgers/filter", params.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("filter rejects invalid collection")
  void filterRejectsInvalidCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.filter(
            FilterParams.create()
                .filters(
                    List.of(
                        FilterCondition.create()
                            .field("status")
                            .operator("eq")
                            .value("APPLIED"))),
            "accounts");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
  }

  @Test
  @DisplayName("filter rejects missing filter value")
  void filterRejectsMissingFilterValue() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.filter(
            FilterParams.create()
                .filters(List.of(FilterCondition.create().field("status").operator("eq"))),
            "transactions");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("filters[0].value is required for operator \"eq\"", response.message());
  }

  @Test
  @DisplayName("filter rejects invalid limit")
  void filterRejectsInvalidLimit() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.filter(
            FilterParams.create()
                .filters(
                    List.of(
                        FilterCondition.create()
                            .field("status")
                            .operator("eq")
                            .value("APPLIED")))
                .limit(500),
            "transactions");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals(
        "limit must be an integer between 1 and 100 if provided", response.message());
  }
}
