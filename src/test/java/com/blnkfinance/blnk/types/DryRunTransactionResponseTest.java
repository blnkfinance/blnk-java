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

  @Test
  @DisplayName("parses notes on a preview that would still apply")
  void parsesNotesOnAPreviewThatWouldStillApply() {
    DryRunTransactionResponse response =
        DryRunTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                {
                  "dry_run": true,
                  "would_apply": true,
                  "status": "APPLIED",
                  "reference": "probe-notes-1",
                  "currency": "USD",
                  "amount": 10,
                  "precise_amount": "1000",
                  "precision": 100,
                  "balances": [],
                  "notes": [
                    "currency mismatch: transaction is USD but destination balance bln_77debc36 is EUR; the ledger applies this as raw minor units"
                  ]
                }
                """));

    assertEquals(true, response.wouldApply());
    assertEquals(1, response.notes().size());
    assertTrue(response.notes().get(0).startsWith("currency mismatch:"));
  }

  @Test
  @DisplayName("returns no notes when the preview omits them")
  void returnsNoNotesWhenThePreviewOmitsThem() {
    DryRunTransactionResponse response =
        DryRunTransactionResponse.fromJson(
            BlnkJson.parse("{\"dry_run\":true,\"would_apply\":true}"));

    assertTrue(response.notes().isEmpty());
    assertTrue(response.legs().isEmpty());
  }

  @Test
  @DisplayName("parses split-leg projections and the queueing note")
  void parsesSplitLegProjectionsAndTheQueueingNote() {
    DryRunTransactionResponse response =
        DryRunTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                {
                  "dry_run": true,
                  "would_apply": true,
                  "status": "APPLIED",
                  "currency": "USD",
                  "amount": 100,
                  "precise_amount": "10000",
                  "precision": 100,
                  "legs": [
                    {
                      "identifier": "bln_7c9c9bfc",
                      "role": "destination",
                      "precise_amount": "6000",
                      "amount": 60
                    },
                    {
                      "identifier": "@Revenue",
                      "role": "destination",
                      "precise_amount": "4000",
                      "amount": 40
                    }
                  ],
                  "notes": [
                    "skip_queue is false: legs are queued for independent async processing with no ordering guarantee, so each is projected on its own rather than cumulatively"
                  ]
                }
                """));

    assertEquals(2, response.legs().size());
    assertEquals("bln_7c9c9bfc", response.legs().get(0).identifier());
    assertEquals("destination", response.legs().get(0).role());
    assertEquals("6000", response.legs().get(0).preciseAmount());
    assertEquals(60.0, response.legs().get(0).amount());
    assertEquals("@Revenue", response.legs().get(1).identifier());
    assertEquals("4000", response.legs().get(1).preciseAmount());
    assertEquals(1, response.notes().size());
    assertTrue(response.notes().get(0).contains("no ordering guarantee"));
  }

  @Test
  @DisplayName("parses a bulk preview with batch-level and per-item notes")
  void parsesABulkPreviewWithBatchLevelAndPerItemNotes() {
    DryRunBulkTransactionResponse response =
        DryRunBulkTransactionResponse.fromJson(
            BlnkJson.parse(
                """
                {
                  "dry_run": true,
                  "would_apply": true,
                  "cumulative": false,
                  "atomic": false,
                  "results": [
                    {
                      "dry_run": true,
                      "would_apply": true,
                      "reference": "probe-notes-b1",
                      "currency": "USD",
                      "amount": 10
                    },
                    {
                      "dry_run": true,
                      "would_apply": true,
                      "reference": "probe-notes-b2",
                      "currency": "EUR",
                      "amount": 11,
                      "notes": [
                        "currency mismatch: transaction is EUR but source balance bln_d29f47b8 is USD; the ledger applies this as raw minor units"
                      ]
                    }
                  ],
                  "notes": [
                    "items are dispatched concurrently unless skip_queue is set, so each item is projected independently against current balances and real execution order is not guaranteed"
                  ]
                }
                """));

    assertEquals(true, response.dryRun());
    assertEquals(true, response.wouldApply());
    assertEquals(false, response.cumulative());
    assertEquals(false, response.atomic());
    assertEquals(1, response.notes().size());
    assertTrue(response.notes().get(0).contains("dispatched concurrently"));

    assertEquals(2, response.transactions().size());
    assertTrue(response.transactions().get(0).notes().isEmpty());
    assertEquals(1, response.transactions().get(1).notes().size());
    assertTrue(response.transactions().get(1).notes().get(0).startsWith("currency mismatch:"));
  }

  @Test
  @DisplayName("returns no batch notes for a bulk array response")
  void returnsNoBatchNotesForABulkArrayResponse() {
    DryRunBulkTransactionResponse response =
        DryRunBulkTransactionResponse.fromJson(
            BlnkJson.parse("[{\"dry_run\":true,\"would_apply\":true}]"));

    assertTrue(response.notes().isEmpty());
    assertEquals(1, response.transactions().size());
  }
}
