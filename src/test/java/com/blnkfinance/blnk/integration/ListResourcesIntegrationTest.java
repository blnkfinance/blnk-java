package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.ListOptions;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live coverage of GET list routes ({@code /ledgers}, {@code /balances},
 * {@code /transactions}; in Core since ~0.14.x). Gated on {@code BLNK_E2E=1}.
 * When Core is down these tests are skipped, not marked passed.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("list ledgers, balances, and transactions — live")
class ListResourcesIntegrationTest {

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
            client.ledgers()::list,
            row -> ledgerId.equals(row.path("ledger_id").asText()) || name.equals(row.path("name").asText()),
            "created ledger " + ledgerId + " missing from list");
    assertEquals(name, found.path("name").asText());
  }

  @Test
  @DisplayName("create a balance and transaction then page until both lists include them")
  void listIncludesCreatedBalanceAndTransaction() {
    ApiResponse<JsonNode> ledger =
        client.ledgers().create(CreateLedger.create().name("Java List Nested " + UUID.randomUUID()));
    assertEquals(201, ledger.status(), ledger.message());
    String ledgerId = ledger.data().get("ledger_id").asText();

    ApiResponse<JsonNode> created =
        client
            .ledgerBalances()
            .create(CreateLedgerBalance.create().ledgerId(ledgerId).currency("USD"));
    assertEquals(201, created.status(), created.message());
    String balanceId = created.data().get("balance_id").asText();

    pageUntil(
        client.ledgerBalances()::list,
        row -> balanceId.equals(row.path("balance_id").asText()),
        "created balance " + balanceId + " missing from list");

    String reference = TestUtils.generateRandomNumbersWithPrefix("list", 8);
    ApiResponse<JsonNode> txn =
        client
            .transactions()
            .create(
                CreateTransactions.create()
                    .amount(100)
                    .precision(100)
                    .currency("USD")
                    .reference(reference)
                    .description("list resources coverage")
                    .source("@WorldUSD")
                    .destination(balanceId)
                    .allowOverdraft(true));
    assertTrue(txn.status() == 200 || txn.status() == 201, txn.message());
    String txnId = txn.data().get("transaction_id").asText();

    pageUntil(
        client.transactions()::list,
        row -> txnId.equals(row.path("transaction_id").asText()),
        "created transaction " + txnId + " missing from list");
  }

  private static JsonNode pageUntil(
      Function<ListOptions, ApiResponse<JsonNode>> listFn,
      Predicate<JsonNode> predicate,
      String missing) {
    JsonNode found = null;
    int offset = 0;
    int limit = 50;
    while (true) {
      ApiResponse<JsonNode> listed = listFn.apply(ListOptions.create().limit(limit).offset(offset));
      assertEquals(200, listed.status(), listed.message());
      assertNotNull(listed.data());
      assertTrue(listed.data().isArray(), listed.data().toString());
      for (JsonNode row : listed.data()) {
        if (predicate.test(row)) {
          found = row;
          break;
        }
      }
      if (found != null || listed.data().size() < limit) {
        break;
      }
      offset += limit;
    }
    assertNotNull(found, missing);
    return found;
  }
}
