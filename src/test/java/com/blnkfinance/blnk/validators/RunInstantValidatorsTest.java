package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.types.ExternalTransaction;
import com.blnkfinance.blnk.types.RunInstantReconData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for instant-reconciliation payload validation in {@link ReconciliationValidators}. */
@DisplayName("ValidateRunInstantReconData")
class RunInstantValidatorsTest {

  /**
   * Builds a valid instant-reconciliation payload. The optional {@code dry_run}
   * key is deliberately absent: an absent key counts as "not provided" and
   * passes validation.
   */
  private static RunInstantReconData validData() {
    return RunInstantReconData.create()
        .externalTransactions(
            List.of(
                ExternalTransaction.create()
                    .id("txn_1")
                    .amount(5.49)
                    .reference("INV-2023-002")
                    .currency("GBP")
                    .description("Card payment")
                    .date("2024-11-15T14:25:30Z")
                    .source("bank-api")))
        .strategy("one_to_one")
        .matchingRuleIds(List.of("rule_abc123"));
  }

  private static String validate(RunInstantReconData data) {
    return ReconciliationValidators.validateRunInstantReconData(data.toMap());
  }

  @Test
  @DisplayName("accepts valid payload")
  void acceptsValidPayload() {
    assertNull(validate(validData()));
  }

  @Test
  @DisplayName("rejects empty external_transactions")
  void rejectsEmptyExternalTransactions() {
    String result = validate(validData().externalTransactions(List.of()));

    assertTrue(result.contains("external_transactions"));
  }

  @Test
  @DisplayName("rejects too many external_transactions")
  void rejectsTooManyExternalTransactions() {
    List<ExternalTransaction> transactions = new ArrayList<>();
    for (int i = 0; i < 10001; i++) {
      transactions.add(
          ExternalTransaction.create()
              .id("txn_" + i)
              .amount(1)
              .reference("ref_" + i)
              .currency("USD")
              .description("desc")
              .date("2024-11-15T14:25:30Z")
              .source("bank-api"));
    }

    String result = validate(validData().externalTransactions(transactions));

    assertTrue(result.contains("too many external_transactions"));
  }

  @Test
  @DisplayName("rejects invalid strategy")
  void rejectsInvalidStrategy() {
    String result = validate(validData().strategy("invalid"));

    assertTrue(result.contains("strategy"));
  }

  @Test
  @DisplayName("rejects empty matching_rule_ids")
  void rejectsEmptyMatchingRuleIds() {
    String result = validate(validData().matchingRuleIds(List.of()));

    assertTrue(result.contains("matching_rule_ids"));
  }
}
