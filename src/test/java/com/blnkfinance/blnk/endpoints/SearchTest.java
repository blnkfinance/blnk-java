package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.SearchParams;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the Search service: collection routing and parameter validation. */
@DisplayName("Search.search identities collection")
class SearchTest {

  @Test
  @DisplayName("search forwards identities collection")
  void searchForwardsIdentitiesCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    SearchParams params =
        SearchParams.create()
            .q("jane")
            .queryBy("first_name,last_name,email_address")
            .perPage(10);

    ApiResponse<JsonNode> response = search.search(params, "identities");

    assertEquals(
        List.of(new CapturingRequest.Call("search/identities", params.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("search rejects invalid collection")
  void searchRejectsInvalidCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.search(SearchParams.create().q("test"), "accounts");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals(
        "collection must be ledgers, transactions, balances, or identities",
        response.message());
  }

  @Test
  @DisplayName("search rejects empty q")
  void searchRejectsEmptyQ() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.search(SearchParams.create().q("   "), "identities");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("Field \"q\" must be filled", response.message());
  }

  @Test
  @DisplayName("Search forwards ledgers collection")
  void searchForwardsLedgersCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    SearchParams params = SearchParams.create().q("General").perPage(5);
    ApiResponse<JsonNode> response = search.search(params, "ledgers");

    assertEquals(
        List.of(new CapturingRequest.Call("search/ledgers", params.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
  }

  @Test
  @DisplayName("Search forwards transactions collection")
  void searchForwardsTransactionsCollection() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    SearchParams params = SearchParams.create().q("payment").perPage(5);
    ApiResponse<JsonNode> response = search.search(params, "transactions");

    assertEquals(
        List.of(
            new CapturingRequest.Call("search/transactions", params.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
  }

  @Test
  @DisplayName("search rejects invalid per_page")
  void searchRejectsInvalidPerPage() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Search search = new Search(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.search(SearchParams.create().q("*").perPage(500), "identities");

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals(
        "per_page must be an integer between 1 and 250 if provided", response.message());
  }
}
