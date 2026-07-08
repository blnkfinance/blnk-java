package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BulkCommitInflightItem;
import com.blnkfinance.blnk.types.BulkCommitInflightRequest;
import com.blnkfinance.blnk.types.BulkTransactions;
import com.blnkfinance.blnk.types.BulkVoidInflightRequest;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.MultipleSourcesT;
import com.blnkfinance.blnk.types.RefundTransactionRequest;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests exercising SDK capabilities against a live Blnk Core
 * instance at {@code http://localhost:5001} ({@code docker compose up} in the
 * Blnk repository). Gated on {@code BLNK_E2E=1}.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("SDK integration — each added capability vs Blnk Core")
class SdkCapabilitiesIntegrationTest {

  private static final Blnk client =
      Blnk.init(
          TestUtils.BLNK_API_KEY,
          BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  /** Common transaction fields shared by every request in this suite. */
  private static CreateTransactions baseTxn() {
    return CreateTransactions.create()
        .precision(100)
        .currency("USD")
        .description("SDK integration test")
        .allowOverdraft(true);
  }

  private static String createLedger(String name) {
    ApiResponse<JsonNode> response =
        client.ledgers().create(CreateLedger.create().name(name));
    assertEquals(201, response.status(), "ledger create " + name + ": " + response.status());
    return response.data().get("ledger_id").asText();
  }

  private static String createBalance(String ledgerId) {
    ApiResponse<JsonNode> response =
        client
            .ledgerBalances()
            .create(
                CreateLedgerBalance.create()
                    .currency("USD")
                    .ledgerId(ledgerId)
                    .metaData(Map.of()));
    assertEquals(201, response.status(), "balance create: " + response.status());
    return response.data().get("balance_id").asText();
  }

  private static String createInflight(String destination, String refPrefix) {
    ApiResponse<JsonNode> createResp =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(1000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix(refPrefix, 6))
                    .source("@FundingPool")
                    .destination(destination)
                    .inflight(true)
                    .inflightExpiryDate("2026-12-31T23:59:59Z"));
    assertEquals(201, createResp.status());
    return createResp.data().get("transaction_id").asText();
  }

  @Test
  @DisplayName("System.health returns UP")
  void systemHealth() {
    ApiResponse<JsonNode> response = client.system().health();

    assertEquals(200, response.status());
    assertEquals("UP", response.data().get("status").asText());
  }

  @Test
  @DisplayName("skip_queue + effective_date")
  void skipQueueEffectiveDate() {
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(1000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue40-skip", 6))
                    .source("@FundingPool")
                    .destination("@Recipient")
                    .skipQueue(true)
                    .effectiveDate("2025-02-15T10:30:00Z"));

    assertEquals(201, response.status());
    assertTrue(response.data().get("skip_queue").asBoolean());
    assertEquals("2025-02-15T10:30:00Z", response.data().get("effective_date").asText());
  }

  @Test
  @DisplayName("inflight_commit_date + scheduled_for")
  void inflightCommitDateScheduledFor() {
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(1000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue40-dates", 6))
                    .source("@FundingPool")
                    .destination("@Recipient")
                    .inflight(true)
                    .inflightExpiryDate("2026-12-31T23:59:59Z")
                    .inflightCommitDate("2024-04-22T15:28:03+00:00")
                    .scheduledFor("2025-12-31T23:59:59Z"));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("Transactions.createBulk serializes date fields")
  void createBulkDateFields() {
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .createBulk(
                BulkTransactions.create()
                    .transactions(
                        List.of(
                            baseTxn()
                                .amount(500)
                                .reference(
                                    TestUtils.generateRandomNumbersWithPrefix(
                                        "issue40-bulk-1", 6))
                                .source("@FundingPool")
                                .destination("@Recipient")
                                .effectiveDate("2025-02-15T10:30:00Z")
                                .inflightCommitDate("2025-06-01T12:00:00Z"),
                            baseTxn()
                                .amount(750)
                                .reference(
                                    TestUtils.generateRandomNumbersWithPrefix(
                                        "issue40-bulk-2", 6))
                                .source("@FundingPool")
                                .destination("@Recipient")
                                .scheduledFor("2025-07-01T08:00:00Z"))));

    assertEquals(201, response.status());
    JsonNode bulk = response.data();
    assertEquals(2, bulk.get("transaction_count").asInt(), "Core accepted 2 bulk transactions");
    assertFalse(bulk.get("batch_id").asText().isEmpty(), "batch_id returned from Core");
    assertTrue(bulk.get("status").isTextual(), "status returned from Core");
  }

  @Test
  @DisplayName("Decimal distribution (240.23) + ISO dates")
  void decimalDistributionIsoDates() {
    String ledgerId = createLedger("Issue41 Ledger");
    String destA = createBalance(ledgerId);
    String destB = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(1000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue41", 6))
                    .source("@FundingPool")
                    .destinations(
                        List.of(
                            MultipleSourcesT.create().identifier(destA).distribution("240.23"),
                            MultipleSourcesT.create().identifier(destB).distribution("left")))
                    .effectiveDate("2024-04-22T15:28:03+00:00")
                    .inflightExpiryDate("2025-08-01T08:00:00Z"));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("Multiple sources → single destination")
  void multipleSourcesSingleDestination() {
    String ledgerId = createLedger("Issue42 Sources");
    String alice = createBalance(ledgerId);
    String bob = createBalance(ledgerId);
    String sarah = createBalance(ledgerId);
    String destination = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(30000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue42-src", 6))
                    .sources(
                        List.of(
                            MultipleSourcesT.create().identifier(alice).distribution("10%"),
                            MultipleSourcesT.create().identifier(bob).distribution("20000"),
                            MultipleSourcesT.create().identifier(sarah).distribution("left")))
                    .destination(destination));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("Single source → multiple destinations")
  void singleSourceMultipleDestinations() {
    String ledgerId = createLedger("Issue42 Dests");
    String alice = createBalance(ledgerId);
    String bob = createBalance(ledgerId);
    String charlie = createBalance(ledgerId);
    String source = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(30000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue42-dest", 6))
                    .source(source)
                    .destinations(
                        List.of(
                            MultipleSourcesT.create().identifier(alice).distribution("10%"),
                            MultipleSourcesT.create().identifier(bob).distribution("20000"),
                            MultipleSourcesT.create()
                                .identifier(charlie)
                                .distribution("left"))));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("precise_distribution legs")
  void preciseDistributionLegs() {
    String ledgerId = createLedger("Issue42 Precise");
    String merchant = createBalance(ledgerId);
    String fee = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(10000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue42-precise", 6))
                    .source("@FundingPool")
                    .destinations(
                        List.of(
                            MultipleSourcesT.create()
                                .identifier(merchant)
                                .preciseDistribution("9733"),
                            MultipleSourcesT.create()
                                .identifier(fee)
                                .preciseDistribution("267"))));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("precise_amount-only create")
  void preciseAmountOnlyCreate() {
    String ledgerId = createLedger("Issue42 PreciseAmt");
    String destination = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .preciseAmount(75000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue42-pamt", 6))
                    .source("@FundingPool")
                    .destination(destination));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("Transactions.updateStatus commit")
  void updateStatusCommit() {
    String ledgerId = createLedger("Issue42 Inflight");
    String destination = createBalance(ledgerId);

    ApiResponse<JsonNode> createResp =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(5000)
                    .reference(
                        TestUtils.generateRandomNumbersWithPrefix("issue42-inflight", 6))
                    .source("@FundingPool")
                    .destination(destination)
                    .inflight(true)
                    .inflightExpiryDate("2026-12-31T23:59:59Z"));

    assertEquals(201, createResp.status());
    TestUtils.sleepSeconds(2);

    ApiResponse<JsonNode> commitResp =
        client
            .transactions()
            .updateStatus(
                createResp.data().get("transaction_id").asText(),
                UpdateTransactionStatus.create().status("commit"));
    assertEquals(200, commitResp.status());
  }

  @Test
  @DisplayName("Create response includes hash, parent_transaction, inflight")
  void createResponseShape() {
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(1000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue43-resp", 6))
                    .source("@FundingPool")
                    .destination("@Recipient")
                    .allowOverdraft(false)
                    .inflight(false));

    assertEquals(201, response.status());
    JsonNode data = response.data();
    assertFalse(data.get("hash").asText().isEmpty(), "hash present");
    assertEquals(64, data.get("hash").asText().length(), "hash is SHA-256 hex");
    assertTrue(data.get("parent_transaction").isTextual());
    assertFalse(data.get("inflight").asBoolean());
    assertTrue(data.get("allow_overdraft").isBoolean());
    assertEquals("0001-01-01T00:00:00Z", data.get("scheduled_for").asText());
    assertEquals("0001-01-01T00:00:00Z", data.get("inflight_expiry_date").asText());
    // Core omits inflight_commit_date when inflight is false.
    if (data.has("inflight_commit_date")) {
      assertTrue(data.get("inflight_commit_date").isTextual());
    }
  }

  @Test
  @DisplayName("Decimal percentage split (33.33% / 66.67%)")
  void decimalPercentageSplit() {
    String ledgerId = createLedger("Issue43 Ledger");
    String destA = createBalance(ledgerId);
    String destB = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .preciseAmount(30000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue43", 6))
                    .source("@FundingPool")
                    .destinations(
                        List.of(
                            MultipleSourcesT.create().identifier(destA).distribution("33.33%"),
                            MultipleSourcesT.create()
                                .identifier(destB)
                                .distribution("66.67%"))));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("Mixed decimal % + precise_distribution + left")
  void mixedDistributions() {
    String ledgerId = createLedger("Issue43 Mixed");
    String a = createBalance(ledgerId);
    String b = createBalance(ledgerId);
    String c = createBalance(ledgerId);

    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(30000)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("issue43-mix", 6))
                    .source("@FundingPool")
                    .destinations(
                        List.of(
                            MultipleSourcesT.create().identifier(a).distribution("33.33%"),
                            MultipleSourcesT.create().identifier(b).preciseDistribution("5000"),
                            MultipleSourcesT.create().identifier(c).distribution("left"))));

    assertEquals(201, response.status());
    assertFalse(response.data().get("transaction_id").asText().isEmpty());
  }

  @Test
  @DisplayName("createBulk skip_queue + BulkTransactionResponse shape")
  void createBulkSkipQueue() {
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .createBulk(
                BulkTransactions.create()
                    .skipQueue(true)
                    .transactions(
                        List.of(
                            baseTxn()
                                .amount(500)
                                .reference(
                                    TestUtils.generateRandomNumbersWithPrefix(
                                        "issue44-bulk-1", 6))
                                .source("@FundingPool")
                                .destination("@Recipient"),
                            baseTxn()
                                .amount(750)
                                .reference(
                                    TestUtils.generateRandomNumbersWithPrefix(
                                        "issue44-bulk-2", 6))
                                .source("@FundingPool")
                                .destination("@Recipient"))));

    assertEquals(201, response.status());
    JsonNode bulk = response.data();
    assertFalse(bulk.get("batch_id").asText().isEmpty(), "batch_id present");
    assertTrue(bulk.get("status").isTextual(), "status present");
    assertEquals(2, bulk.get("transaction_count").asInt(), "transaction_count matches batch size");
  }

  @Test
  @DisplayName("Partial commit with precise_amount")
  void partialCommitPreciseAmount() {
    String ledgerId = createLedger("Issue45 PartialCommit");
    String destination = createBalance(ledgerId);

    ApiResponse<JsonNode> createResp =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(1000)
                    .reference(
                        TestUtils.generateRandomNumbersWithPrefix("issue45-inflight", 6))
                    .source("@FundingPool")
                    .destination(destination)
                    .inflight(true)
                    .inflightExpiryDate("2026-12-31T23:59:59Z"));

    assertEquals(201, createResp.status());
    TestUtils.sleepSeconds(2);

    ApiResponse<JsonNode> partialCommitResp =
        client
            .transactions()
            .updateStatus(
                createResp.data().get("transaction_id").asText(),
                UpdateTransactionStatus.create()
                    .status("commit")
                    .preciseAmount(50000)
                    .skipQueue(true));
    assertEquals(200, partialCommitResp.status());
    assertEquals("APPLIED", partialCommitResp.data().get("status").asText());
  }

  @Test
  @DisplayName("updateStatus queued by default")
  void updateStatusQueuedByDefault() {
    String ledgerId = createLedger("Issue117 QueuedCommit");
    String destination = createBalance(ledgerId);
    String transactionId = createInflight(destination, "issue117-queued");
    TestUtils.sleepSeconds(2);

    ApiResponse<JsonNode> commitResp =
        client
            .transactions()
            .updateStatus(transactionId, UpdateTransactionStatus.create().status("commit"));
    assertEquals(200, commitResp.status());
    assertTrue(commitResp.data().get("queued").asBoolean());
  }

  @Test
  @DisplayName("updateStatus skip_queue synchronous commit")
  void updateStatusSkipQueue() {
    String ledgerId = createLedger("Issue117 SyncCommit");
    String destination = createBalance(ledgerId);
    String transactionId = createInflight(destination, "issue117-sync");
    TestUtils.sleepSeconds(2);

    ApiResponse<JsonNode> commitResp =
        client
            .transactions()
            .updateStatus(
                transactionId,
                UpdateTransactionStatus.create().status("commit").skipQueue(true));
    assertEquals(200, commitResp.status());
    assertEquals("APPLIED", commitResp.data().get("status").asText());
  }

  @Test
  @DisplayName("bulkCommitInflight queued vs skip_queue")
  void bulkCommitInflight() {
    String ledgerId = createLedger("Issue117 BulkCommit");
    String destination = createBalance(ledgerId);
    String txnQueued = createInflight(destination, "issue117-bulk-q");
    String txnSync = createInflight(destination, "issue117-bulk-s");
    TestUtils.sleepSeconds(2);

    ApiResponse<JsonNode> queuedResp =
        client
            .transactions()
            .bulkCommitInflight(
                BulkCommitInflightRequest.create()
                    .transactions(
                        List.of(BulkCommitInflightItem.create().transactionId(txnQueued))));
    assertEquals(200, queuedResp.status());
    assertEquals("queued", queuedResp.data().get("results").get(0).get("status").asText());

    ApiResponse<JsonNode> syncResp =
        client
            .transactions()
            .bulkCommitInflight(
                BulkCommitInflightRequest.create()
                    .skipQueue(true)
                    .transactions(
                        List.of(BulkCommitInflightItem.create().transactionId(txnSync))));
    assertEquals(200, syncResp.status());
    assertEquals("succeeded", syncResp.data().get("results").get(0).get("status").asText());
  }

  @Test
  @DisplayName("bulkVoidInflight queued vs skip_queue")
  void bulkVoidInflight() {
    String ledgerId = createLedger("Issue117 BulkVoid");
    String destination = createBalance(ledgerId);
    String txnQueued = createInflight(destination, "issue117-bulk-void-q");
    String txnSync = createInflight(destination, "issue117-bulk-void-s");
    TestUtils.sleepSeconds(2);

    ApiResponse<JsonNode> queuedResp =
        client
            .transactions()
            .bulkVoidInflight(
                BulkVoidInflightRequest.create().transactionIds(List.of(txnQueued)));
    assertEquals(200, queuedResp.status());
    assertEquals("queued", queuedResp.data().get("results").get(0).get("status").asText());

    ApiResponse<JsonNode> syncResp =
        client
            .transactions()
            .bulkVoidInflight(
                BulkVoidInflightRequest.create()
                    .skipQueue(true)
                    .transactionIds(List.of(txnSync)));
    assertEquals(200, syncResp.status());
    assertEquals("succeeded", syncResp.data().get("results").get(0).get("status").asText());
  }

  @Test
  @DisplayName("get returns created transaction")
  void getReturnsCreatedTransaction() {
    String reference = TestUtils.generateRandomNumbersWithPrefix("issue12-ref", 6);
    ApiResponse<JsonNode> createResp =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(500)
                    .reference(reference)
                    .source("@FundingPool")
                    .destination("@Recipient"));

    assertEquals(201, createResp.status());
    String transactionId = createResp.data().get("transaction_id").asText();
    assertFalse(transactionId.isEmpty());

    ApiResponse<JsonNode> getResp = client.transactions().get(transactionId);
    assertEquals(200, getResp.status());
    assertEquals(transactionId, getResp.data().get("transaction_id").asText());
    assertEquals(reference, getResp.data().get("reference").asText());
    assertEquals("USD", getResp.data().get("currency").asText());
    assertTrue(getResp.data().get("amount").isNumber());
  }

  @Test
  @DisplayName("getByReference returns created transaction")
  void getByReferenceReturnsCreatedTransaction() {
    String reference = TestUtils.generateRandomNumbersWithPrefix("issue14-ref", 6);
    ApiResponse<JsonNode> createResp =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(500)
                    .reference(reference)
                    .source("@FundingPool")
                    .destination("@Recipient"));

    assertEquals(201, createResp.status());
    assertFalse(createResp.data().get("transaction_id").asText().isEmpty());

    ApiResponse<JsonNode> getResp = client.transactions().getByReference(reference);
    assertEquals(200, getResp.status());
    assertEquals(
        createResp.data().get("transaction_id").asText(),
        getResp.data().get("transaction_id").asText());
    assertEquals(reference, getResp.data().get("reference").asText());
    assertEquals("USD", getResp.data().get("currency").asText());
  }

  @Test
  @DisplayName("Refund queued vs synchronous skip_queue")
  void refundQueuedVsSkipQueue() {
    String ledgerId = createLedger("Issue46 Refund");
    String destination = createBalance(ledgerId);

    ApiResponse<JsonNode> createResp =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(500)
                    .reference(
                        TestUtils.generateRandomNumbersWithPrefix("issue46-refund-src", 6))
                    .source("@FundingPool")
                    .destination(destination)
                    .skipQueue(true));

    assertEquals(201, createResp.status());
    assertEquals("APPLIED", createResp.data().get("status").asText());
    String originalTxnId = createResp.data().get("transaction_id").asText();

    ApiResponse<JsonNode> queuedRefundResp = client.transactions().refund(originalTxnId);
    assertEquals(201, queuedRefundResp.status());
    assertFalse(queuedRefundResp.data().get("transaction_id").asText().isEmpty());
    assertEquals(originalTxnId, queuedRefundResp.data().get("parent_transaction").asText());

    ApiResponse<JsonNode> createResp2 =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(500)
                    .reference(
                        TestUtils.generateRandomNumbersWithPrefix("issue46-refund-sync", 6))
                    .source("@FundingPool")
                    .destination(destination)
                    .skipQueue(true));

    assertEquals(201, createResp2.status());
    String originalTxnId2 = createResp2.data().get("transaction_id").asText();

    ApiResponse<JsonNode> syncRefundResp =
        client
            .transactions()
            .refund(originalTxnId2, RefundTransactionRequest.create().skipQueue(true));
    assertEquals(201, syncRefundResp.status());
    assertEquals("APPLIED", syncRefundResp.data().get("status").asText());
    assertEquals(originalTxnId2, syncRefundResp.data().get("parent_transaction").asText());
  }
}
