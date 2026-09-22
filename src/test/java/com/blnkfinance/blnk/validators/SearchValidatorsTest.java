package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for search collection and parameter validation in {@link SearchValidators}. */
@DisplayName("Search validators")
class SearchValidatorsTest {

  @Test
  @DisplayName("ValidateSearchCollection accepts identities")
  void validateSearchCollectionAcceptsIdentities() {
    assertNull(SearchValidators.validateSearchCollection("identities"));
    assertNull(SearchValidators.validateSearchCollection("ledgers"));
  }

  @Test
  @DisplayName("ValidateSearchCollection rejects unknown collection")
  void validateSearchCollectionRejectsUnknownCollection() {
    assertEquals(
        "collection must be ledgers, transactions, balances, or identities",
        SearchValidators.validateSearchCollection("accounts"));
  }

  @Test
  @DisplayName("ValidateSearchParams accepts API-valid payload")
  void validateSearchParamsAcceptsApiValidPayload() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("q", "*");
    data.put("query_by", "first_name,last_name,email_address");
    data.put("filter_by", "identity_type:=individual");
    data.put("sort_by", "created_at:desc");
    data.put("page", 1);
    data.put("per_page", 25);
    assertNull(SearchValidators.validateSearchParams(data));
  }

  @Test
  @DisplayName("ValidateSearchParams rejects empty q")
  void validateSearchParamsRejectsEmptyQ() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("q", "");
    assertEquals("Field \"q\" must be filled", SearchValidators.validateSearchParams(data));
  }

  @Test
  @DisplayName("ValidateSearchParams rejects invalid page")
  void validateSearchParamsRejectsInvalidPage() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("q", "*");
    data.put("page", 0);
    assertEquals(
        "page must be a positive integer if provided",
        SearchValidators.validateSearchParams(data));
  }

  @Test
  @DisplayName("ValidateMultiSearchParams accepts a well-formed body")
  void validateMultiSearchParamsAcceptsWellFormedBody() {
    assertNull(
        SearchValidators.validateMultiSearchParams(
            Map.of("searches", List.of(
                Map.of("collection", "ledgers", "q", "savings"),
                Map.of("collection", "identities", "q", "jane", "per_page", 5)))));
  }

  @Test
  @DisplayName("ValidateMultiSearchParams rejects null, missing, and empty searches")
  void validateMultiSearchParamsRejectsMissingSearches() {
    assertEquals(
        "Multi-search params must be a valid object",
        SearchValidators.validateMultiSearchParams(null));
    assertEquals(
        "searches must be a non-empty array", SearchValidators.validateMultiSearchParams(Map.of()));
    assertEquals(
        "searches must be a non-empty array",
        SearchValidators.validateMultiSearchParams(Map.of("searches", List.of())));
    assertEquals(
        "searches must be a non-empty array",
        SearchValidators.validateMultiSearchParams(Map.of("searches", "ledgers")));
  }

  @Test
  @DisplayName("ValidateMultiSearchParams rejects a non-object entry")
  void validateMultiSearchParamsRejectsNonObjectEntry() {
    assertEquals(
        "searches[0] must be a valid object",
        SearchValidators.validateMultiSearchParams(Map.of("searches", List.of("ledgers"))));
  }

  @Test
  @DisplayName("ValidateMultiSearchParams rejects a missing or unknown collection")
  void validateMultiSearchParamsRejectsBadCollection() {
    assertEquals(
        "searches[0].collection must be ledgers, transactions, balances, or identities",
        SearchValidators.validateMultiSearchParams(Map.of("searches", List.of(Map.of("q", "x")))));
    assertEquals(
        "searches[1].collection must be ledgers, transactions, balances, or identities",
        SearchValidators.validateMultiSearchParams(
            Map.of("searches", List.of(
                Map.of("collection", "ledgers", "q", "x"),
                Map.of("collection", "Ledgers", "q", "x")))));
  }

  @Test
  @DisplayName("ValidateMultiSearchParams prefixes per-entry param errors")
  void validateMultiSearchParamsPrefixesParamErrors() {
    assertEquals(
        "searches[1]: per_page must be an integer between 1 and 250 if provided",
        SearchValidators.validateMultiSearchParams(
            Map.of("searches", List.of(
                Map.of("collection", "ledgers", "q", "x"),
                Map.of("collection", "balances", "q", "x", "per_page", 500)))));
  }
}
