package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.MultiSearchParams;
import com.blnkfinance.blnk.types.SearchParams;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link Search#multiSearch}: body shape, validation, and error handling. */
@DisplayName("Search.multiSearch")
class SearchMultiSearchTest {

  private static final BlnkLogger mockLogger = TestMocks.createMockLogger();

  private static Search service(CapturingRequest capturedRequest) {
    return new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
  }

  @Test
  @DisplayName("posts a searches array with collection beside each entry's params")
  void postsSearchesArray() {
    CapturingRequest capturedRequest =
        CapturingRequest.of(TestMocks.createMockBlnkRequest(true, null, 200));
    MultiSearchParams params =
        MultiSearchParams.create()
            .add("transactions", SearchParams.create().q("ref_001").queryBy("reference"))
            .add("balances", SearchParams.create().q("*").filterBy("currency:USD").perPage(50));

    ApiResponse<JsonNode> response = service(capturedRequest).multiSearch(params);

    assertEquals(
        List.of(new CapturingRequest.Call("multi-search", params.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(
        BlnkJson.parse(
            "{\"searches\":["
                + "{\"collection\":\"transactions\",\"q\":\"ref_001\",\"query_by\":\"reference\"},"
                + "{\"collection\":\"balances\",\"q\":\"*\",\"filter_by\":\"currency:USD\",\"per_page\":50}"
                + "]}").toString(),
        params.toJson().toString());
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("rejects an empty searches list without calling the API")
  void rejectsEmptySearches() {
    CapturingRequest capturedRequest =
        CapturingRequest.of(TestMocks.createMockBlnkRequest(true, null, 200));

    ApiResponse<JsonNode> response =
        service(capturedRequest).multiSearch(MultiSearchParams.create());

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("searches must be a non-empty array", response.message());
  }

  @Test
  @DisplayName("rejects an unknown collection and names the entry")
  void rejectsUnknownCollection() {
    CapturingRequest capturedRequest =
        CapturingRequest.of(TestMocks.createMockBlnkRequest(true, null, 200));
    MultiSearchParams params =
        MultiSearchParams.create()
            .add("ledgers", SearchParams.create().q("savings"))
            .add("accounts", SearchParams.create().q("x"));

    ApiResponse<JsonNode> response = service(capturedRequest).multiSearch(params);

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals(
        "searches[1].collection must be ledgers, transactions, balances, or identities",
        response.message());
  }

  @Test
  @DisplayName("rejects a missing q and names the entry")
  void rejectsMissingQ() {
    CapturingRequest capturedRequest =
        CapturingRequest.of(TestMocks.createMockBlnkRequest(true, null, 200));

    ApiResponse<JsonNode> response =
        service(capturedRequest)
            .multiSearch(MultiSearchParams.create().add("ledgers", SearchParams.create()));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("searches[0]: Field \"q\" must be filled", response.message());
  }

  @Test
  @DisplayName("rejects null params")
  void rejectsNullParams() {
    CapturingRequest capturedRequest =
        CapturingRequest.of(TestMocks.createMockBlnkRequest(true, null, 200));

    ApiResponse<JsonNode> response = service(capturedRequest).multiSearch(null);

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("Multi-search params must be a valid object", response.message());
  }

  @Test
  @DisplayName("handles thrown errors gracefully")
  void handlesThrownErrors() {
    CapturingRequest capturedRequest =
        CapturingRequest.of(TestMocks.createMockBlnkRequest(true, "Network Error"));

    ApiResponse<JsonNode> response =
        service(capturedRequest)
            .multiSearch(MultiSearchParams.create().add("ledgers", SearchParams.create().q("a")));

    assertEquals(500, response.status());
    assertEquals("Network Error", response.message());
  }
}
