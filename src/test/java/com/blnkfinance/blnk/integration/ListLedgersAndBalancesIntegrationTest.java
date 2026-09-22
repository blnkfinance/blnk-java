package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.ListOptions;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Live coverage of {@code ledgers().list} and {@code ledgerBalances().list}
 * ({@code GET /ledgers}, {@code GET /balances}; in Core since ~0.14.x). Gated
 * on {@code BLNK_E2E=1}. That flag is the skip condition: if it is set and
 * Core is unreachable, these tests fail rather than skip.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("list ledgers and balances — live")
class ListLedgersAndBalancesIntegrationTest {

  private static final Blnk client =
      Blnk.init(
          TestUtils.BLNK_API_KEY,
          BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  @Test
  @DisplayName("create a ledger then page until list includes it")
  void listIncludesCreatedLedger() {
    String name = "Java List Ledger " + UUID.randomUUID();
    ApiResponse<JsonNode> created = client.ledgers().create(CreateLedger.create().name(name));
    assertEquals(201, created.status(), created.message());
    String ledgerId = created.data().get("ledger_id").asText();

    JsonNode found =
        pageUntil(
            options -> client.ledgers().list(options),
            row -> ledgerId.equals(row.path("ledger_id").asText()) || name.equals(row.path("name").asText()),
            "created ledger " + ledgerId + " missing from list");
    assertEquals(name, found.path("name").asText());
  }

  @Test
  @DisplayName("create a balance then page until list includes it")
  void listIncludesCreatedBalance() {
    ApiResponse<JsonNode> ledger =
        client.ledgers().create(CreateLedger.create().name("Java List Balance " + UUID.randomUUID()));
    assertEquals(201, ledger.status(), ledger.message());
    String ledgerId = ledger.data().get("ledger_id").asText();

    ApiResponse<JsonNode> created =
        client
            .ledgerBalances()
            .create(CreateLedgerBalance.create().ledgerId(ledgerId).currency("USD"));
    assertEquals(201, created.status(), created.message());
    String balanceId = created.data().get("balance_id").asText();

    pageUntil(
        options -> client.ledgerBalances().list(options),
        row -> balanceId.equals(row.path("balance_id").asText()),
        "created balance " + balanceId + " missing from list");
  }

  @Test
  @DisplayName("ledger list offset=1 is a different row than offset=0")
  void ledgerListOffsetChangesPage() {
    createLedger("Java List Page A " + UUID.randomUUID());
    createLedger("Java List Page B " + UUID.randomUUID());
    assertDistinctPages(
        options -> client.ledgers().list(options), "ledger_id", "ledgers");
  }

  @Test
  @DisplayName("balance list offset=1 is a different row than offset=0")
  void balanceListOffsetChangesPage() {
    String ledgerId = createLedger("Java List Page Bal " + UUID.randomUUID());
    createBalance(ledgerId);
    createBalance(ledgerId);
    assertDistinctPages(
        options -> client.ledgerBalances().list(options), "balance_id", "balances");
  }

  private static String createLedger(String name) {
    ApiResponse<JsonNode> created = client.ledgers().create(CreateLedger.create().name(name));
    assertEquals(201, created.status(), created.message());
    return created.data().get("ledger_id").asText();
  }

  private static void createBalance(String ledgerId) {
    ApiResponse<JsonNode> created =
        client
            .ledgerBalances()
            .create(CreateLedgerBalance.create().ledgerId(ledgerId).currency("USD"));
    assertEquals(201, created.status(), created.message());
  }

  private static void assertDistinctPages(
      java.util.function.Function<ListOptions, ApiResponse<JsonNode>> listFn,
      String idField,
      String resource) {
    JsonNode first = listPage(listFn, 0);
    JsonNode second = listPage(listFn, 1);
    assertEquals(1, first.size(), resource + " page 0 should have one row: " + first);
    assertEquals(1, second.size(), resource + " page 1 should have one row: " + second);
    String firstId = first.get(0).path(idField).asText();
    String secondId = second.get(0).path(idField).asText();
    assertTrue(!firstId.isEmpty(), resource + " page 0 missing " + idField);
    assertTrue(!secondId.isEmpty(), resource + " page 1 missing " + idField);
    assertNotEquals(
        firstId,
        secondId,
        resource + " offset did not change the page; both returned " + firstId);
  }

  private static JsonNode listPage(
      java.util.function.Function<ListOptions, ApiResponse<JsonNode>> listFn, int offset) {
    ApiResponse<JsonNode> response =
        listFn.apply(ListOptions.create().limit(1).offset(offset));
    assertEquals(200, response.status(), response.message());
    assertNotNull(response.data());
    assertTrue(response.data().isArray(), response.data().toString());
    return response.data();
  }

  private static final int PAGE_SIZE = 50;
  private static final int MAX_PAGES = 100;

  private static JsonNode pageUntil(
      java.util.function.Function<ListOptions, ApiResponse<JsonNode>> listFn,
      Predicate<JsonNode> predicate,
      String missing) {
    JsonNode found = null;
    int offset = 0;
    int pages = 0;
    while (true) {
      ApiResponse<JsonNode> listed =
          listFn.apply(ListOptions.create().limit(PAGE_SIZE).offset(offset));
      assertEquals(200, listed.status(), listed.message());
      assertNotNull(listed.data());
      assertTrue(listed.data().isArray(), listed.data().toString());
      pages++;
      for (JsonNode row : listed.data()) {
        if (predicate.test(row)) {
          found = row;
          break;
        }
      }
      if (found != null || listed.data().size() < PAGE_SIZE) {
        break;
      }
      if (pages >= MAX_PAGES) {
        fail(missing + "; last offset=" + offset);
      }
      offset += PAGE_SIZE;
    }
    assertNotNull(found, missing);
    return found;
  }
}
