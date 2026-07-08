package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests covering fields that Blnk Core 0.15.0 no longer returns.
 *
 * <p>Two flavors: optional dropped fields simply read as null when absent,
 * while fully removed fields must not exist on the search document types at
 * all — the latter is verified by reflecting over the DTO's declared members.
 */
@DisplayName("Core 0.15.0 dropped response fields")
class DroppedResponseFieldsTest {

  @Test
  @DisplayName("CreateTransactionResponse accepts response without rate")
  void createTransactionResponseAcceptsResponseWithoutRate() {
    // Minimal response body: no rate, no effective_date, no source/destination.
    ObjectNode json = BlnkJson.objectNode();
    json.put("transaction_id", "txn_test_123");
    json.put("amount", 10);
    json.put("precision", 100);
    json.put("precise_amount", 1000);
    json.put("reference", "ref_001");
    json.put("description", "test");
    json.put("currency", "USD");
    json.put("status", "QUEUED");
    json.put("hash", "0b9c25fb5b00d6c71cb4ca87026bf6dc316e63353d3330deb588bd0b3d74dcc0");
    json.put("parent_transaction", "");
    json.put("allow_overdraft", false);
    json.put("inflight", false);
    json.put("created_at", "2026-06-24T00:00:00Z");
    json.put("scheduled_for", "0001-01-01T00:00:00Z");
    json.put("inflight_expiry_date", "0001-01-01T00:00:00Z");
    json.put("inflight_commit_date", "0001-01-01T00:00:00Z");
    CreateTransactionResponse response = CreateTransactionResponse.fromJson(json);

    // The absent rate field reads as null.
    assertNull(response.rate());
  }

  @Test
  @DisplayName("CreateLedgerBalanceResp accepts response without currency_multiplier")
  void createLedgerBalanceRespAcceptsResponseWithoutCurrencyMultiplier() {
    // Create-balance responses are surfaced as the raw parsed JSON body (no
    // dedicated DTO), so the dropped optional field is simply an absent key.
    ObjectNode response = BlnkJson.objectNode();
    response.put("balance", 0);
    response.put("version", 0);
    response.put("inflight_balance", 0);
    response.put("credit_balance", 0);
    response.put("inflight_credit_balance", 0);
    response.put("debit_balance", 0);
    response.put("inflight_debit_balance", 0);
    response.put("ledger_id", "ldg_test");
    response.put("identity_id", "");
    response.put("balance_id", "bln_test");
    response.put("indicator", "");
    response.put("currency", "USD");
    response.put("created_at", "2026-06-24T00:00:00Z");

    assertNull(response.get("currency_multiplier"));
  }

  @Test
  @DisplayName("SearchTransactionDocument omits rate")
  void searchTransactionDocumentOmitsRate() {
    SearchTransactionDocument document =
        SearchTransactionDocument.create()
            .id("txn_test")
            .transactionId("txn_test")
            .status("APPLIED")
            .createdAt(1781028226L);

    // The removed field must not exist on the DTO at all.
    assertFalse(hasMember(document.getClass(), "rate"));
  }

  @Test
  @DisplayName("SearchBalanceDocument omits currency_multiplier")
  void searchBalanceDocumentOmitsCurrencyMultiplier() {
    SearchBalanceDocument document =
        SearchBalanceDocument.create()
            .id("bln_test")
            .balanceId("bln_test")
            .balance("0")
            .createdAt(1781222909L);

    // The removed field must not exist on the DTO at all (wire name or Java name).
    assertFalse(hasMember(document.getClass(), "currency_multiplier"));
    assertFalse(hasMember(document.getClass(), "currencyMultiplier"));
  }

  /** True when the class declares a field or method with the given name. */
  private static boolean hasMember(Class<?> type, String name) {
    for (Field field : type.getDeclaredFields()) {
      if (field.getName().equals(name)) {
        return true;
      }
    }
    for (Method method : type.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }
}
