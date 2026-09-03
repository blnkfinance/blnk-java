package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkErrorCodes;
import com.blnkfinance.blnk.types.BulkCommitInflightItem;
import com.blnkfinance.blnk.types.BulkCommitInflightRequest;
import com.blnkfinance.blnk.types.BulkTransactions;
import com.blnkfinance.blnk.types.BulkVoidInflightRequest;
import com.blnkfinance.blnk.types.CreateHookData;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.DryRunBulkTransactionResponse;
import com.blnkfinance.blnk.types.DryRunTransactionResponse;
import com.blnkfinance.blnk.types.RefundTransactionRequest;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live coverage of Core 0.15.3 additions. Gated on {@code BLNK_E2E=1}.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("Core 0.15.3 SDK capabilities")
class Core0153IntegrationTest {

  private static final Blnk client =
      Blnk.init(
          TestUtils.BLNK_API_KEY,
          BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  private static CreateTransactions baseTxn() {
    return CreateTransactions.create()
        .precision(100)
        .currency("USD")
        .description("Core 0.15.3 integration")
        .allowOverdraft(true);
  }

  private static String createLedger(String name) {
    ApiResponse<JsonNode> response = client.ledgers().create(CreateLedger.create().name(name));
    assertEquals(201, response.status(), "ledger create: " + response.message());
    return response.data().get("ledger_id").asText();
  }

  private static String createBalance(String ledgerId) {
    ApiResponse<JsonNode> response =
        client
            .ledgerBalances()
            .create(CreateLedgerBalance.create().currency("USD").ledgerId(ledgerId));
    assertEquals(201, response.status(), "balance create: " + response.message());
    return response.data().get("balance_id").asText();
  }

  @Test
  @DisplayName("dry-run create previews without writing a transaction")
  void dryRunCreatePreviewsWithoutWriting() {
    String destination = createBalance(createLedger("0.15.3 Dry-run create"));
    String reference = TestUtils.generateRandomNumbersWithPrefix("dry-run-create", 8);

    ApiResponse<JsonNode> preview =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(25)
                    .reference(reference)
                    .source("@FundingPool")
                    .destination(destination)
                    .dryRun(true));

    assertEquals(200, preview.status(), preview.message());
    DryRunTransactionResponse dryRun = DryRunTransactionResponse.fromJson(preview.data());
    assertEquals(true, dryRun.dryRun());
    assertEquals(true, dryRun.wouldApply());
    assertEquals(reference, dryRun.reference());
    assertFalse(preview.data().hasNonNull("transaction_id"));
    assertFalse(dryRun.balances().isEmpty());

    ApiResponse<JsonNode> lookup = client.transactions().getByReference(reference);
    assertNotEquals(200, lookup.status(), "dry-run must not consume the reference");

    ApiResponse<JsonNode> real =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(25)
                    .reference(reference)
                    .source("@FundingPool")
                    .destination(destination)
                    .skipQueue(true));
    assertEquals(201, real.status(), real.message());
    assertTrue(real.data().hasNonNull("transaction_id"));
  }

  @Test
  @DisplayName("dry-run create projects a rejection as 200 would_apply=false")
  void dryRunCreateProjectsRejection() {
    String destination = createBalance(createLedger("0.15.3 Dry-run reject"));
    String reference = TestUtils.generateRandomNumbersWithPrefix("dry-run-reject", 8);

    ApiResponse<JsonNode> preview =
        client
            .transactions()
            .create(
                CreateTransactions.create()
                    .amount(50)
                    .precision(100)
                    .currency("USD")
                    .description("Should not overdraft")
                    .allowOverdraft(false)
                    .reference(reference)
                    .source(destination)
                    .destination("@FundingPool")
                    .dryRun(true));

    assertEquals(200, preview.status(), preview.message());
    DryRunTransactionResponse dryRun = DryRunTransactionResponse.fromJson(preview.data());
    assertEquals(true, dryRun.dryRun());
    assertEquals(false, dryRun.wouldApply());
    assertNotNull(dryRun.rejection());
    assertNotNull(dryRun.rejection().code());
  }

  @Test
  @DisplayName("dry-run bulk create previews without writing")
  void dryRunBulkCreatePreviewsWithoutWriting() {
    String ledgerId = createLedger("0.15.3 Dry-run bulk");
    String destination = createBalance(ledgerId);
    String ref1 = TestUtils.generateRandomNumbersWithPrefix("dry-bulk-1", 8);
    String ref2 = TestUtils.generateRandomNumbersWithPrefix("dry-bulk-2", 8);

    ApiResponse<JsonNode> preview =
        client
            .transactions()
            .createBulk(
                BulkTransactions.create()
                    .dryRun(true)
                    .transactions(
                        List.of(
                            baseTxn()
                                .amount(10)
                                .reference(ref1)
                                .source("@FundingPool")
                                .destination(destination),
                            baseTxn()
                                .amount(15)
                                .reference(ref2)
                                .source("@FundingPool")
                                .destination(destination))));

    assertEquals(200, preview.status(), preview.message());
    assertTrue(
        DryRunTransactionResponse.isDryRun(preview.data())
            || preview.data().isArray()
            || preview.data().has("transactions")
            || preview.data().has("results"),
        "bulk dry-run body: " + preview.data());
    DryRunBulkTransactionResponse bulk = DryRunBulkTransactionResponse.fromJson(preview.data());
    assertTrue(
        !bulk.transactions().isEmpty() || DryRunTransactionResponse.isDryRun(preview.data()),
        "bulk dry-run parsed no items: " + preview.data());

    assertNotEquals(200, client.transactions().getByReference(ref1).status());
    assertNotEquals(200, client.transactions().getByReference(ref2).status());
  }

  @Test
  @DisplayName("dry-run refund, inflight update, bulk commit, and bulk void")
  void dryRunRefundAndInflightFlows() {
    String destination = createBalance(createLedger("0.15.3 Dry-run inflight"));

    ApiResponse<JsonNode> applied =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(80)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("dry-refund-src", 8))
                    .source("@FundingPool")
                    .destination(destination)
                    .skipQueue(true));
    assertEquals(201, applied.status(), applied.message());
    String appliedId = applied.data().get("transaction_id").asText();

    ApiResponse<JsonNode> refundPreview =
        client
            .transactions()
            .refund(
                appliedId,
                RefundTransactionRequest.create()
                    .dryRun(true)
                    .description("Preview refund")
                    .metaData(Map.of("channel", "sdk-test")));
    assertEquals(200, refundPreview.status(), refundPreview.message());
    DryRunTransactionResponse refundDryRun = DryRunTransactionResponse.fromJson(refundPreview.data());
    assertEquals(true, refundDryRun.dryRun());
    assertEquals(true, refundDryRun.wouldApply());

    ApiResponse<JsonNode> inflight =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(40)
                    .inflight(true)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("dry-inflight", 8))
                    .source("@FundingPool")
                    .destination(destination)
                    .skipQueue(true));
    assertEquals(201, inflight.status(), inflight.message());
    String inflightId = inflight.data().get("transaction_id").asText();

    ApiResponse<JsonNode> commitPreview =
        client
            .transactions()
            .updateStatus(
                inflightId, UpdateTransactionStatus.create().status("commit").dryRun(true));
    assertEquals(200, commitPreview.status(), commitPreview.message());
    DryRunTransactionResponse commitDryRun = DryRunTransactionResponse.fromJson(commitPreview.data());
    assertEquals(true, commitDryRun.dryRun());
    assertTrue("commit".equals(commitDryRun.operation()) || commitDryRun.wouldApply());

    ApiResponse<JsonNode> stillInflight = client.transactions().get(inflightId);
    assertEquals(200, stillInflight.status());
    assertEquals("INFLIGHT", stillInflight.data().get("status").asText());

    ApiResponse<JsonNode> bulkCommitPreview =
        client
            .transactions()
            .bulkCommitInflight(
                BulkCommitInflightRequest.create()
                    .dryRun(true)
                    .transactions(
                        List.of(BulkCommitInflightItem.create().transactionId(inflightId))));
    assertEquals(200, bulkCommitPreview.status(), bulkCommitPreview.message());

    ApiResponse<JsonNode> inflight2 =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(20)
                    .inflight(true)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("dry-void", 8))
                    .source("@FundingPool")
                    .destination(destination)
                    .skipQueue(true));
    assertEquals(201, inflight2.status(), inflight2.message());
    String inflight2Id = inflight2.data().get("transaction_id").asText();

    ApiResponse<JsonNode> bulkVoidPreview =
        client
            .transactions()
            .bulkVoidInflight(
                BulkVoidInflightRequest.create().dryRun(true).transactionIds(List.of(inflight2Id)));
    assertEquals(200, bulkVoidPreview.status(), bulkVoidPreview.message());

    assertEquals("INFLIGHT", client.transactions().get(inflight2Id).data().get("status").asText());
  }

  @Test
  @DisplayName("refund description and meta_data are applied")
  void refundDescriptionAndMetaDataAreApplied() {
    String destination = createBalance(createLedger("0.15.3 Refund extras"));
    ApiResponse<JsonNode> applied =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(30)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("refund-meta-src", 8))
                    .source("@FundingPool")
                    .destination(destination)
                    .metaData(Map.of("keep", "original", "replace", "old"))
                    .skipQueue(true));
    assertEquals(201, applied.status(), applied.message());

    ApiResponse<JsonNode> refund =
        client
            .transactions()
            .refund(
                applied.data().get("transaction_id").asText(),
                RefundTransactionRequest.create()
                    .skipQueue(true)
                    .description("SDK refund narration")
                    .metaData(Map.of("replace", "new", "added", "yes")));
    assertEquals(201, refund.status(), refund.message());
    assertEquals("SDK refund narration", refund.data().get("description").asText());
    JsonNode meta = refund.data().get("meta_data");
    assertNotNull(meta);
    assertEquals("new", meta.get("replace").asText());
    assertEquals("yes", meta.get("added").asText());
  }

  @Test
  @DisplayName("creates an internal balance with indicator")
  void createsInternalBalanceWithIndicator() {
    String indicator = "@SdkRev" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    ApiResponse<JsonNode> created =
        client
            .ledgerBalances()
            .create(
                CreateLedgerBalance.create()
                    .ledgerId("general_ledger_id")
                    .currency("USD")
                    .indicator(indicator));
    assertEquals(201, created.status(), created.message());
    assertEquals(indicator, created.data().get("indicator").asText());
    assertEquals("general_ledger_id", created.data().get("ledger_id").asText());

    ApiResponse<JsonNode> duplicate =
        client
            .ledgerBalances()
            .create(
                CreateLedgerBalance.create()
                    .ledgerId("general_ledger_id")
                    .currency("USD")
                    .indicator(indicator));
    assertEquals(409, duplicate.status(), duplicate.message());
    assertNotNull(duplicate.error());
    assertEquals(BlnkErrorCodes.GEN_CONFLICT, duplicate.error().code());
  }

  @Test
  @DisplayName("hooks.list without type returns PRE and POST hooks")
  void hooksListWithoutTypeReturnsAllHooks() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    ApiResponse<JsonNode> pre =
        client
            .hooks()
            .create(
                CreateHookData.create()
                    .name("sdk-pre-" + suffix)
                    .url("https://example.com/hooks/pre-" + suffix)
                    .type("PRE_TRANSACTION")
                    .active(true)
                    .timeout(30)
                    .retryCount(1));
    ApiResponse<JsonNode> post =
        client
            .hooks()
            .create(
                CreateHookData.create()
                    .name("sdk-post-" + suffix)
                    .url("https://example.com/hooks/post-" + suffix)
                    .type("POST_TRANSACTION")
                    .active(true)
                    .timeout(30)
                    .retryCount(1));
    assertTrue(pre.status() == 200 || pre.status() == 201, pre.message());
    assertTrue(post.status() == 200 || post.status() == 201, post.message());

    ApiResponse<JsonNode> listed = client.hooks().list();
    assertEquals(200, listed.status(), listed.message());
    assertTrue(listed.data().isArray());
    boolean sawPre = false;
    boolean sawPost = false;
    for (JsonNode hook : listed.data()) {
      String type = hook.path("type").asText();
      if ("PRE_TRANSACTION".equals(type)) {
        sawPre = true;
      }
      if ("POST_TRANSACTION".equals(type)) {
        sawPost = true;
      }
    }
    assertTrue(sawPre, "list without type should include PRE_TRANSACTION");
    assertTrue(sawPost, "list without type should include POST_TRANSACTION");
  }

  @Test
  @DisplayName("Core rejects negative amount with TXN_INVALID_AMOUNT")
  void coreRejectsNegativeAmount() {
    String destination = createBalance(createLedger("0.15.3 invalid amount"));
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                CreateTransactions.create()
                    .amount(-10)
                    .precision(100)
                    .currency("USD")
                    .description("Negative amount")
                    .reference(TestUtils.generateRandomNumbersWithPrefix("neg-amt", 8))
                    .source("@FundingPool")
                    .destination(destination));
    assertTrue(response.status() >= 400, response.message());
    assertNotNull(response.error());
    // Core 0.15.3 reports this as TXN_VALIDATION_ERROR ("amount cannot be negative").
    // TXN_INVALID_AMOUNT remains a documented catalog code the SDK surfaces when Core sends it.
    assertTrue(
        BlnkErrorCodes.TXN_VALIDATION_ERROR.equals(response.error().code())
            || BlnkErrorCodes.TXN_INVALID_AMOUNT.equals(response.error().code()),
        "unexpected code: " + response.error().code() + " " + response.message());
  }

  @Test
  @DisplayName("Core rejects source equal to destination")
  void coreRejectsSourceEqualToDestination() {
    String destination = createBalance(createLedger("0.15.3 same source dest"));
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                baseTxn()
                    .amount(10)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("same-legs", 8))
                    .source(destination)
                    .destination(destination));
    assertTrue(response.status() >= 400, response.message());
    assertNotNull(response.error());
    assertNotNull(response.error().code());
  }
}
