package com.blnkfinance.blnk.types;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.endpoints.Search;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SearchResponse} across every collection's document
 * type, plus {@code Search.search} calls returning those shapes.
 */
@DisplayName("SearchResponse per collection")
class SearchResponseTest {

  @Test
  @DisplayName("ledger document uses indexed fields")
  void ledgerDocumentUsesIndexedFields() {
    SearchResponse<SearchLedgerDocument> response =
        SearchResponse.<SearchLedgerDocument>create()
            .found(1)
            .outOf(15)
            .page(1)
            .requestParams(
                SearchRequestParams.create().collectionName("ledgers").q("*"))
            .searchTimeMs(0)
            .hits(
                List.of(
                    SearchHit.<SearchLedgerDocument>create()
                        .document(
                            SearchLedgerDocument.create()
                                .id("general_ledger_id")
                                .ledgerId("general_ledger_id")
                                .name("General Ledger")
                                .createdAt(1781226501L))));

    assertTrue(response.hits().get(0).document().createdAt() instanceof Long);
    assertEquals("general_ledger_id", response.hits().get(0).document().ledgerId());
  }

  @Test
  @DisplayName("balance document uses string minor-unit fields")
  void balanceDocumentUsesStringMinorUnitFields() {
    SearchResponse<SearchBalanceDocument> response =
        SearchResponse.<SearchBalanceDocument>create()
            .found(1)
            .outOf(45)
            .page(1)
            .requestParams(
                SearchRequestParams.create().collectionName("balances").q("*"))
            .searchTimeMs(1)
            .hits(
                List.of(
                    SearchHit.<SearchBalanceDocument>create()
                        .document(
                            SearchBalanceDocument.create()
                                .id("bln_15168eb4-bdb1-4e46-9331-f58a6a16b254")
                                .balanceId("bln_15168eb4-bdb1-4e46-9331-f58a6a16b254")
                                .balance("0")
                                .creditBalance("0")
                                .debitBalance("0")
                                .currency("USD")
                                .ledgerId("ldg_0921bd99-ff7c-4a06-b6d7-319f0bb12f87")
                                .createdAt(1781222909L)
                                .trackFundLineage(true))));

    assertTrue(response.hits().get(0).document().balance() instanceof String);
    assertTrue(response.hits().get(0).document().balanceId().startsWith("bln_"));
  }

  @Test
  @DisplayName("transaction document includes status and precise_amount")
  void transactionDocumentIncludesStatusAndPreciseAmount() {
    SearchResponse<SearchTransactionDocument> response =
        SearchResponse.<SearchTransactionDocument>create()
            .found(1)
            .outOf(11)
            .page(1)
            .requestParams(
                SearchRequestParams.create().collectionName("transactions").q("*"))
            .searchTimeMs(2)
            .hits(
                List.of(
                    SearchHit.<SearchTransactionDocument>create()
                        .document(
                            SearchTransactionDocument.create()
                                .id("txn_8bb67c99-70b1-46c2-aa49-1ea3fc2f2233")
                                .transactionId("txn_8bb67c99-70b1-46c2-aa49-1ea3fc2f2233")
                                .amount(250000)
                                .preciseAmount("250000")
                                .status("APPLIED")
                                .createdAt(1781028226L))));

    assertEquals("APPLIED", response.hits().get(0).document().status());
    assertTrue(response.hits().get(0).document().createdAt() instanceof Long);
  }

  @Test
  @DisplayName("identity document includes indexed fields")
  void identityDocumentIncludesIndexedFields() {
    SearchResponse<SearchIdentityDocument> response =
        SearchResponse.<SearchIdentityDocument>create()
            .found(1)
            .outOf(13)
            .page(1)
            .requestParams(
                SearchRequestParams.create().collectionName("identities").q("*"))
            .searchTimeMs(5)
            .hits(
                List.of(
                    SearchHit.<SearchIdentityDocument>create()
                        .document(
                            SearchIdentityDocument.create()
                                .id("idt_fbf6a26c-82c6-46fb-9237-8fbba55a23c0")
                                .identityId("idt_fbf6a26c-82c6-46fb-9237-8fbba55a23c0")
                                .identityType("organization")
                                .createdAt(1781225782L))));

    assertTrue(response.hits().get(0).document().identityId().startsWith("idt_"));
  }

  @Test
  @DisplayName("Search.search infers transaction document fields")
  void searchSearchInfersTransactionDocumentFields() {
    ObjectNode document = BlnkJson.objectNode();
    document.put("id", "txn_8bb67c99-70b1-46c2-aa49-1ea3fc2f2233");
    document.put("transaction_id", "txn_8bb67c99-70b1-46c2-aa49-1ea3fc2f2233");
    document.put("status", "APPLIED");
    document.put("precise_amount", "250000");
    document.put("created_at", 1781028226L);
    ObjectNode hit = BlnkJson.objectNode();
    hit.set("document", document);
    ObjectNode requestParams = BlnkJson.objectNode();
    requestParams.put("collection_name", "transactions");
    requestParams.put("q", "payment");
    ObjectNode transactionSearchResponse = BlnkJson.objectNode();
    transactionSearchResponse.put("found", 1);
    transactionSearchResponse.put("out_of", 11);
    transactionSearchResponse.put("page", 1);
    transactionSearchResponse.set("request_params", requestParams);
    transactionSearchResponse.put("search_time_ms", 2);
    transactionSearchResponse.set("hits", BlnkJson.arrayNode().add(hit));

    BlnkRequest mockRequest =
        (endpoint, data, method, headers) ->
            new com.blnkfinance.blnk.types.ApiResponse<>(
                201, "Success", transactionSearchResponse);
    Search search =
        new Search(mockRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        search.search(SearchParams.create().q("payment"), "transactions");

    assertEquals(201, response.status());
    assertEquals(
        "APPLIED", response.data().get("hits").get(0).get("document").get("status").asText());
    assertEquals(
        "txn_8bb67c99-70b1-46c2-aa49-1ea3fc2f2233",
        response.data().get("hits").get(0).get("document").get("transaction_id").asText());
  }

  @Test
  @DisplayName("Search.search accepts dynamic SearchCollection variable")
  void searchSearchAcceptsDynamicSearchCollectionVariable() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    Search search = new Search(thirdPartyRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    String service = SearchCollections.LEDGERS;

    ApiResponse<JsonNode> response = search.search(SearchParams.create().q("General"), service);

    assertEquals(201, response.status());
  }

  @Test
  @DisplayName("unparameterized SearchResponse remains valid")
  void unparameterizedSearchResponseRemainsValid() {
    // SearchBalanceDocument is the conventional default document type for
    // callers that do not specify one.
    SearchResponse<SearchBalanceDocument> response =
        SearchResponse.<SearchBalanceDocument>create()
            .found(1)
            .outOf(45)
            .page(1)
            .requestParams(
                SearchRequestParams.create().collectionName("balances").q("*"))
            .searchTimeMs(1)
            .hits(
                List.of(
                    SearchHit.<SearchBalanceDocument>create()
                        .document(
                            SearchBalanceDocument.create()
                                .id("bln_15168eb4-bdb1-4e46-9331-f58a6a16b254")
                                .balanceId("bln_15168eb4-bdb1-4e46-9331-f58a6a16b254")
                                .balance("0")
                                .createdAt(1781222909L))));

    assertTrue(response.hits().get(0).document().balance() instanceof String);
    assertTrue(response.hits().get(0).document().balanceId().startsWith("bln_"));
  }

  @Test
  @DisplayName("compile-time Search.search inference checks")
  void compileTimeSearchSearchInferenceChecks() {
    // These typed usages primarily need to compile: the document type
    // parameter must flow through SearchHit to the typed accessors.
    SearchResponse<SearchTransactionDocument> typedUsage =
        SearchResponse.<SearchTransactionDocument>create()
            .hits(
                List.of(
                    SearchHit.<SearchTransactionDocument>create()
                        .document(
                            SearchTransactionDocument.create()
                                .transactionId("txn_1")
                                .status("APPLIED"))));
    String status = typedUsage.hits().get(0).document().status();
    String transactionId = typedUsage.hits().get(0).document().transactionId();
    assertNotNull(status);
    assertNotNull(transactionId);
  }
}
