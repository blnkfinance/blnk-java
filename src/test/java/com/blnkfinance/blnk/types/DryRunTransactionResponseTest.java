package com.blnkfinance.blnk.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the Core 0.15.3 dry-run preview types. */
class DryRunTransactionResponseTest {

  @Test
  @DisplayName("parses an applying dry-run preview")
  void parsesAnApplyingDryRunPreview() {
    DryRunTransactionResponse response =
        DryRunTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                {
                  "dry_run": true,
                  "would_apply": true,
                  "status": "APPLIED",
                  "reference": "ref_card_settle_4821",
                  "currency": "USD",
                  "amount": 120,
                  "precise_amount": "12000",
                  "precision": 100,
                  "balances": [
                    {
                      "balance_id": "bln_source-bal-001",
                      "role": "source",
                      "currency": "USD",
                      "virtual": false,
                      "current_balance": "50000",
                      "resulting_balance": "38000"
                    },
                    {
                      "balance_id": "bln_dest-bal-001",
                      "role": "destination",
                      "currency": "USD",
                      "current_balance": "0",
                      "resulting_balance": "12000"
                    }
                  ]
                }
                """));

    assertTrue(DryRunTransactionResponse.isDryRun(response.toJson()));
    assertEquals(true, response.dryRun());
    assertEquals(true, response.wouldApply());
    assertNull(response.rejection());
    assertEquals("APPLIED", response.status());
    assertEquals("ref_card_settle_4821", response.reference());
    assertEquals("USD", response.currency());
    assertEquals(120.0, response.amount());
    assertEquals("12000", response.preciseAmount());
    assertEquals(100.0, response.precision());
    assertEquals(2, response.balances().size());
    assertEquals("source", response.balances().get(0).role());
    assertEquals("50000", response.balances().get(0).currentBalance());
    assertEquals("38000", response.balances().get(0).resultingBalance());
    assertEquals(false, response.balances().get(0).virtual());
  }

  @Test
  @DisplayName("parses a rejected dry-run preview")
  void parsesARejectedDryRunPreview() {
    DryRunTransactionResponse response =
        DryRunTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                {
                  "dry_run": true,
                  "would_apply": false,
                  "rejection": {
                    "code": "TXN_INSUFFICIENT_FUNDS",
                    "reason": "insufficient_funds",
                    "message": "insufficient funds in source balance"
                  },
                  "reference": "ref_card_settle_4821",
                  "currency": "USD",
                  "amount": 120,
                  "precise_amount": "12000",
                  "precision": 100
                }
                """));

    assertEquals(false, response.wouldApply());
    assertEquals("TXN_INSUFFICIENT_FUNDS", response.rejection().code());
    assertEquals("insufficient_funds", response.rejection().reason());
    assertEquals("insufficient funds in source balance", response.rejection().message());
    assertNull(response.status());
  }

  @Test
  @DisplayName("parses inflight operation on a preview")
  void parsesInflightOperationOnAPreview() {
    DryRunTransactionResponse response =
        DryRunTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                {
                  "dry_run": true,
                  "would_apply": true,
                  "operation": "commit",
                  "status": "APPLIED"
                }
                """));

    assertEquals("commit", response.operation());
  }

  @Test
  @DisplayName("does not treat a recorded transaction as a dry run")
  void doesNotTreatARecordedTransactionAsADryRun() {
    assertFalse(
        DryRunTransactionResponse.isDryRun(
            BlnkJson.parse("{\"transaction_id\":\"txn_1\",\"status\":\"APPLIED\"}")));
  }

  @Test
  @DisplayName("parses a bulk dry-run array")
  void parsesABulkDryRunArray() {
    DryRunBulkTransactionResponse response =
        DryRunBulkTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                [
                  {"dry_run":true,"would_apply":true,"reference":"bulk_1"},
                  {"dry_run":true,"would_apply":false,"reference":"bulk_2"}
                ]
                """));

    assertEquals(2, response.transactions().size());
    assertEquals(true, response.transactions().get(0).wouldApply());
    assertEquals(false, response.transactions().get(1).wouldApply());
  }
}
