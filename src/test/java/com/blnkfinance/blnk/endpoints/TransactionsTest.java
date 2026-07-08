package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.CoreCreateTransactionResponse;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.BulkCommitInflightItem;
import com.blnkfinance.blnk.types.BulkCommitInflightRequest;
import com.blnkfinance.blnk.types.BulkTransactions;
import com.blnkfinance.blnk.types.BulkVoidInflightRequest;
import com.blnkfinance.blnk.types.CreateTransactionResponse;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.MultipleSourcesT;
import com.blnkfinance.blnk.types.RecoverQueueRequest;
import com.blnkfinance.blnk.types.RefundTransactionRequest;
import com.blnkfinance.blnk.types.TransactionConstants;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.blnkfinance.blnk.util.ValueFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Transactions service: create, update-status, refund,
 * lookup, queue-recovery, and bulk flows with their validation rules.
 */
class TransactionsTest {

  private static final BlnkLogger mockLogger = TestMocks.createMockLogger();

  private static Transactions newTransactions(BlnkRequest request) {
    return new Transactions(request, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Creates a transaction")
  class CreatesATransaction {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    }

    @Test
    @DisplayName("Creates a transaction with valid data")
    void createsATransactionWithValidData() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(10000)
              .currency("USD")
              .description("Test transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("1234567890");

      ApiResponse<JsonNode> transaction = transactions.create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(data.toJson().get("amount"), transaction.data().get("amount"));
      assertEquals(data.toJson().get("currency"), transaction.data().get("currency"));
      assertEquals(data.toJson().get("description"), transaction.data().get("description"));
    }

    @Test
    @DisplayName("Creates a transaction with precise_amount only")
    void createsATransactionWithPreciseAmountOnly() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .preciseAmount(75000)
              .currency("USD")
              .description("Precise amount transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("precise_ref_001")
              .source("@FundingPool")
              .destination("bln_recipient");

      ApiResponse<JsonNode> transaction = transactions.create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, transaction.status());
    }

    @Test
    @DisplayName("Creates a transaction with ISO date strings unchanged")
    void createsATransactionWithIsoDateStringsUnchanged() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(10000)
              .currency("USD")
              .description("Scheduled inflight transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("issue_41_ref_001")
              .source("@FundingPool")
              .destination("bln_recipient")
              .inflight(true)
              .scheduledFor("2025-12-31T23:59:59Z")
              .inflightExpiryDate("2025-08-01T08:00:00Z");

      ApiResponse<JsonNode> transaction = transactions.create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, transaction.status());
    }

    @Test
    @DisplayName("Creates a transaction with decimal distribution split")
    void createsATransactionWithDecimalDistributionSplit() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(1000)
              .currency("USD")
              .description("Decimal distribution split")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("issue_41_ref_002")
              .source("@FundingPool")
              .destinations(
                  List.of(
                      MultipleSourcesT.create().identifier("bln_fee").distribution("240.23"),
                      MultipleSourcesT.create().identifier("bln_recipient").distribution("left")));

      ApiResponse<JsonNode> transaction = transactions.create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, transaction.status());
    }

    @Test
    @DisplayName("forwards atomic on split transaction create")
    void forwardsAtomicOnSplitTransactionCreate() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(1000)
              .currency("USD")
              .description("Atomic split transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("issue_5_atomic_split")
              .source("@FundingPool")
              .destinations(
                  List.of(
                      MultipleSourcesT.create().identifier("bln_fee").distribution("240.23"),
                      MultipleSourcesT.create().identifier("bln_recipient").distribution("left")))
              .atomic(true)
              .skipQueue(true);

      ApiResponse<JsonNode> transaction = transactions.create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, transaction.status());
    }

    @Test
    @DisplayName("rejects invalid atomic on create")
    void rejectsInvalidAtomicOnCreate() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(1000)
              .currency("USD")
              .description("Invalid atomic")
              .precision(100)
              .reference("issue_5_bad_atomic")
              .source("@FundingPool")
              .destinations(
                  List.of(
                      MultipleSourcesT.create().identifier("bln_fee").distribution("50%"),
                      MultipleSourcesT.create().identifier("bln_recipient").distribution("left")))
              .atomic((Object) "true");

      ApiResponse<JsonNode> response = transactions.create(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("atomic must be a boolean if provided.", response.message());
    }

    @Test
    @DisplayName("Returns Core create response fields")
    void returnsCoreCreateResponseFields() {
      ObjectNode coreResponse = CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse();
      BlnkRequest responseReturningRequest =
          (endpoint, requestData, method, headerOptions) ->
              new ApiResponse<>(201, "Success", coreResponse);
      Transactions transactions = newTransactions(responseReturningRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(1250.34)
              .currency("USD")
              .description("Card payment on Stripe")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("ref_2ye281ewiu-1e17-dh17-eh18728hd245")
              .source("@WorldUSD")
              .destination("@MyBalance")
              .allowOverdraft(false)
              .inflight(false);

      ApiResponse<JsonNode> transaction = transactions.create(data);

      assertEquals(201, transaction.status());
      assertEquals(coreResponse.get("hash"), transaction.data().get("hash"));
      assertEquals(
          coreResponse.get("parent_transaction"), transaction.data().get("parent_transaction"));
      assertEquals(
          coreResponse.get("allow_overdraft"), transaction.data().get("allow_overdraft"));
      assertEquals(coreResponse.get("inflight"), transaction.data().get("inflight"));
      assertEquals(coreResponse.get("scheduled_for"), transaction.data().get("scheduled_for"));
      assertEquals(
          coreResponse.get("inflight_expiry_date"),
          transaction.data().get("inflight_expiry_date"));
      assertEquals(
          coreResponse.get("inflight_commit_date"),
          transaction.data().get("inflight_commit_date"));
    }

    @Test
    @DisplayName("CreateTransactionResponse type includes hash, parent_transaction,"
        + " allow_overdraft, inflight_expiry_date, inflight_commit_date, and scheduled_for")
    void createTransactionResponseTypeIncludesHashParentOverdraftAndDateFields() {
      ObjectNode sampleJson = CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse();
      sampleJson.set("meta_data", BlnkJson.toObjectNode(Map.of("company_name", "Test Company")));
      CreateTransactionResponse sample = CreateTransactionResponse.fromJson(sampleJson);

      assertTrue(ValueFormat.isTruthy(sample.hash()));
      assertInstanceOf(String.class, sample.parentTransaction());
      assertInstanceOf(Boolean.class, sample.allowOverdraft());
      assertTrue(ValueFormat.isTruthy(sample.inflightExpiryDate()));
      assertTrue(ValueFormat.isTruthy(sample.inflightCommitDate()));
      assertTrue(ValueFormat.isTruthy(sample.scheduledFor()));
    }

    @Test
    @DisplayName("Creates a transaction with skip_queue, effective_date, and"
        + " inflight_commit_date and serializes dates")
    void createsATransactionWithSkipQueueAndDateFieldsAndSerializesDates() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      Date effectiveDate = Date.from(Instant.parse("2025-02-15T10:30:00.000Z"));
      CreateTransactions data =
          CreateTransactions.create()
              .amount(10000)
              .currency("USD")
              .description("Backdated skip-queue transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("issue_40_ref_001")
              .source("@FundingPool")
              .destination("bln_recipient")
              .skipQueue(true)
              .effectiveDate(effectiveDate)
              .inflightCommitDate("2025-06-01T12:00:00Z");

      ApiResponse<JsonNode> transaction = transactions.create(data);

      ObjectNode expectedBody = data.toJson();
      expectedBody.put("effective_date", "2025-02-15T10:30:00Z");
      assertEquals(
          List.of(new CapturingRequest.Call("transactions", expectedBody, "POST", null)),
          capturedRequest.calls);
      assertEquals(201, transaction.status());
    }

    @Test
    @DisplayName("It should handle missing required fields")
    void itShouldHandleMissingRequiredFields() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      // Simulates a caller omitting the compulsory reference field.
      CreateTransactions data =
          CreateTransactions.create()
              .currency("USD")
              .description("Test transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .amount(10000);

      ApiResponse<JsonNode> response = transactions.create(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
    }

    @Test
    @DisplayName("it should handle thrown errors during creation")
    void itShouldHandleThrownErrorsDuringCreation() {
      BlnkRequest thirdPartyRequest =
          TestMocks.createMockBlnkRequest(false, "Something went wrong");
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(10000)
              .currency("USD")
              .description("Test transaction")
              .metaData(Map.of("company_name", "Test Company"))
              .precision(100)
              .reference("1234567890");

      ApiResponse<JsonNode> response = transactions.create(data);

      // The request WAS invoked (the mock threw after being called).
      assertEquals(
          List.of(new CapturingRequest.Call("transactions", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertNull(response.data());
      assertEquals(500, response.status());
      assertEquals("Something went wrong", response.message());
    }

    @Test
    @DisplayName("it should handle meta_data if it is not an object")
    void itShouldHandleMetaDataIfItIsNotAnObject() {
      BlnkRequest thirdPartyRequest =
          TestMocks.createMockBlnkRequest(false, "Something went wrong");
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      CreateTransactions data =
          CreateTransactions.create()
              .amount(10000)
              .currency("USD")
              .description("Test transaction")
              .metaData((Object) "Test Company")
              .precision(100)
              .reference("1234567890");

      ApiResponse<JsonNode> response = transactions.create(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
      assertEquals("meta_data must be a valid object if provided", response.message());
    }
  }

  @Nested
  @DisplayName("Updates a transaction")
  class UpdatesATransaction {

    private final String id = "1234";
    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("Updates a transaction status with valid data")
    void updatesATransactionStatusWithValidData() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      UpdateTransactionStatus data = UpdateTransactionStatus.create().status("commit");

      ApiResponse<JsonNode> transaction = transactions.updateStatus(id, data);

      assertEquals(
          List.of(
              new CapturingRequest.Call("transactions/inflight/" + id, data.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(200, transaction.status());
    }

    @Test
    @DisplayName("Partial commit forwards precise_amount")
    void partialCommitForwardsPreciseAmount() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      UpdateTransactionStatus data =
          UpdateTransactionStatus.create().status("commit").preciseAmount(50000);

      ApiResponse<JsonNode> transaction = transactions.updateStatus(id, data);

      assertEquals(
          List.of(
              new CapturingRequest.Call("transactions/inflight/" + id, data.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(200, transaction.status());
    }

    @Test
    @DisplayName("Updates fails for a transaction status with invalid data")
    void updatesFailsForATransactionStatusWithInvalidData() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      UpdateTransactionStatus data =
          UpdateTransactionStatus.create().status("commit").metaData((Object) "Test Company");

      ApiResponse<JsonNode> transaction = transactions.updateStatus(id, data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(transaction.data());
      assertEquals(400, transaction.status());
    }

    @Test
    @DisplayName("updateStatus forwards skip_queue on request")
    void updateStatusForwardsSkipQueueOnRequest() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      UpdateTransactionStatus data =
          UpdateTransactionStatus.create().status("commit").skipQueue(true);

      ApiResponse<JsonNode> transaction = transactions.updateStatus(id, data);

      assertEquals(
          List.of(
              new CapturingRequest.Call("transactions/inflight/" + id, data.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(200, transaction.status());
    }

    @Test
    @DisplayName("updateStatus rejects invalid skip_queue")
    void updateStatusRejectsInvalidSkipQueue() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      UpdateTransactionStatus data =
          UpdateTransactionStatus.create().status("commit").skipQueue((Object) "true");

      ApiResponse<JsonNode> transaction = transactions.updateStatus(id, data);

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, transaction.status());
      assertTrue(transaction.message().contains("skip_queue must be a boolean if provided"));
    }
  }

  @Nested
  @DisplayName("GET transaction by id")
  class GetTransactionById {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("get calls correct endpoint")
    void getCallsCorrectEndpoint() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);
      String transactionId = "txn_issue12_abc123";

      ApiResponse<JsonNode> response = transactions.get(transactionId);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/" + transactionId, null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("get rejects empty transaction id")
    void getRejectsEmptyTransactionId() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response = transactions.get("");

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("transaction id is required", response.message());
    }
  }

  @Nested
  @DisplayName("GET transaction lineage")
  class GetTransactionLineage {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("getLineage calls correct endpoint")
    void getLineageCallsCorrectEndpoint() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);
      String transactionId = "txn_issue13_abc123";

      ApiResponse<JsonNode> response = transactions.getLineage(transactionId);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/" + transactionId + "/lineage", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("getLineage rejects empty transaction id")
    void getLineageRejectsEmptyTransactionId() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response = transactions.getLineage("");

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("transaction id is required", response.message());
    }
  }

  @Nested
  @DisplayName("POST recover queued transactions")
  class PostRecoverQueuedTransactions {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("recoverQueue calls default endpoint")
    void recoverQueueCallsDefaultEndpoint() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response = transactions.recoverQueue();

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/recover", null, "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("recoverQueue forwards threshold query param")
    void recoverQueueForwardsThresholdQueryParam() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response =
          transactions.recoverQueue(RecoverQueueRequest.create().threshold("5m"));

      assertEquals(
          List.of(
              new CapturingRequest.Call("transactions/recover?threshold=5m", null, "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("recoverQueue rejects invalid threshold before request")
    void recoverQueueRejectsInvalidThresholdBeforeRequest() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response =
          transactions.recoverQueue(RecoverQueueRequest.create().threshold("bogus"));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("threshold must be a valid duration string (e.g. 5m, 1h).", response.message());
    }
  }

  @Nested
  @DisplayName("GET transaction by reference")
  class GetTransactionByReference {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("getByReference calls correct endpoint")
    void getByReferenceCallsCorrectEndpoint() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);
      String reference = "ref_issue14_abc123";

      ApiResponse<JsonNode> response = transactions.getByReference(reference);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/reference/ref_issue14_abc123", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("getByReference path-escapes special characters")
    void getByReferencePathEscapesSpecialCharacters() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);
      String reference = "ref/with space?query#hash%25";

      transactions.getByReference(reference);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/reference/ref%2Fwith%20space%3Fquery%23hash%2525",
                  null,
                  "GET",
                  null)),
          capturedRequest.calls);
    }

    @Test
    @DisplayName("getByReference rejects empty reference")
    void getByReferenceRejectsEmptyReference() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response = transactions.getByReference("");

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("reference is required", response.message());
    }
  }

  @Nested
  @DisplayName("Refunds a transaction")
  class RefundsATransaction {

    private final String id = "txn_refund_1234";
    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    }

    @Test
    @DisplayName("Refund without body keeps backward-compatible call")
    void refundWithoutBodyKeepsBackwardCompatibleCall() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> refundResponse = transactions.refund(id);

      assertEquals(
          List.of(new CapturingRequest.Call("refund-transaction/" + id, null, "POST", null)),
          capturedRequest.calls);
      assertEquals(201, refundResponse.status());
    }

    @Test
    @DisplayName("Refund forwards skip_queue on request body")
    void refundForwardsSkipQueueOnRequestBody() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      RefundTransactionRequest options = RefundTransactionRequest.create().skipQueue(true);
      ApiResponse<JsonNode> refundResponse = transactions.refund(id, options);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "refund-transaction/" + id, options.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, refundResponse.status());
    }

    @Test
    @DisplayName("Refund rejects invalid skip_queue")
    void refundRejectsInvalidSkipQueue() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> refundResponse =
          transactions.refund(id, RefundTransactionRequest.create().skipQueue((Object) "true"));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, refundResponse.status());
      assertEquals("skip_queue must be a boolean if provided.", refundResponse.message());
    }
  }

  @Nested
  @DisplayName("Creates bulk transactions")
  class CreatesBulkTransactions {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    }

    @Test
    @DisplayName("Creates bulk transactions with valid data")
    void createsBulkTransactionsWithValidData() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .atomic(true)
              .inflight(false)
              .runAsync(false)
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Test transaction 1")
                          .metaData(Map.of("department", "sales", "project", "Q4_campaign"))
                          .precision(100)
                          .reference("bulk_txn_001")
                          .source("@source_account_1")
                          .destination("@destination_account_1"),
                      CreateTransactions.create()
                          .amount(2000)
                          .currency("USD")
                          .description("Test transaction 2")
                          .metaData(Map.of("department", "marketing", "project", "Q4_campaign"))
                          .precision(100)
                          .reference("bulk_txn_002")
                          .source("@source_account_2")
                          .destination("@destination_account_2")));

      ApiResponse<JsonNode> bulkResponse = transactions.createBulk(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/bulk", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, bulkResponse.status());
    }

    @Test
    @DisplayName("createBulk serializes date fields on each transaction")
    void createBulkSerializesDateFieldsOnEachTransaction() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      Date effectiveDate = Date.from(Instant.parse("2025-02-15T10:30:00.000Z"));
      Date scheduledDate = Date.from(Instant.parse("2025-07-01T08:00:00.000Z"));
      BulkTransactions data =
          BulkTransactions.create()
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Bulk txn with effective_date")
                          .metaData(Map.of("department", "sales", "project", "Q4_campaign"))
                          .precision(100)
                          .reference("bulk_txn_date_001")
                          .source("@source_account_1")
                          .destination("@destination_account_1")
                          .effectiveDate(effectiveDate)
                          .inflightCommitDate("2025-06-01T12:00:00Z"),
                      CreateTransactions.create()
                          .amount(2000)
                          .currency("USD")
                          .description("Bulk txn with scheduled_for")
                          .metaData(Map.of("department", "marketing", "project", "Q4_campaign"))
                          .precision(100)
                          .reference("bulk_txn_date_002")
                          .source("@source_account_2")
                          .destination("@destination_account_2")
                          .scheduledFor(scheduledDate)
                          .skipQueue(true)));

      ApiResponse<JsonNode> bulkResponse = transactions.createBulk(data);

      ObjectNode expectedBody = data.toJson();
      ((ObjectNode) expectedBody.get("transactions").get(0))
          .put("effective_date", "2025-02-15T10:30:00Z");
      ((ObjectNode) expectedBody.get("transactions").get(1))
          .put("scheduled_for", "2025-07-01T08:00:00Z");
      assertEquals(
          List.of(new CapturingRequest.Call("transactions/bulk", expectedBody, "POST", null)),
          capturedRequest.calls);
      assertEquals(201, bulkResponse.status());
    }

    @Test
    @DisplayName("Creates basic bulk transactions without optional flags")
    void createsBasicBulkTransactionsWithoutOptionalFlags() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1500)
                          .currency("USD")
                          .description("Basic bulk transaction")
                          .precision(100)
                          .reference("basic_bulk_txn_001")
                          .source("@source_account")
                          .destination("@destination_account")));

      ApiResponse<JsonNode> bulkResponse = transactions.createBulk(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/bulk", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, bulkResponse.status());
    }

    @Test
    @DisplayName("Should handle empty transactions array")
    void shouldHandleEmptyTransactionsArray() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data = BulkTransactions.create().atomic(true).transactions(List.of());

      ApiResponse<JsonNode> response = transactions.createBulk(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
      assertTrue(response.message().contains("Transactions array cannot be empty"));
    }

    @Test
    @DisplayName("Should handle invalid transaction data in bulk")
    void shouldHandleInvalidTransactionDataInBulk() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .atomic(true)
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Valid transaction")
                          .precision(100)
                          .reference("valid_txn_001")
                          .source("@source_account")
                          .destination("@destination_account"),
                      // Missing required fields (no description).
                      CreateTransactions.create()
                          .amount(2000)
                          .currency("USD")
                          .precision(100)
                          .reference("invalid_txn_002")));

      ApiResponse<JsonNode> response = transactions.createBulk(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
      assertTrue(response.message().contains("Transaction at index 1:"));
    }

    @Test
    @DisplayName("Should handle duplicate references in bulk")
    void shouldHandleDuplicateReferencesInBulk() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .atomic(true)
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Transaction 1")
                          .precision(100)
                          .reference("duplicate_ref")
                          .source("@source_account_1")
                          .destination("@destination_account_1"),
                      CreateTransactions.create()
                          .amount(2000)
                          .currency("USD")
                          .description("Transaction 2")
                          .precision(100)
                          .reference("duplicate_ref") // Same reference as above
                          .source("@source_account_2")
                          .destination("@destination_account_2")));

      ApiResponse<JsonNode> response = transactions.createBulk(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
      assertTrue(response.message().contains("All transactions must have unique references"));
    }

    @Test
    @DisplayName("Should handle invalid boolean flags")
    void shouldHandleInvalidBooleanFlags() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .atomic((Object) "true") // Should be boolean, not string
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Test transaction")
                          .precision(100)
                          .reference("test_txn_001")
                          .source("@source_account")
                          .destination("@destination_account")));

      ApiResponse<JsonNode> response = transactions.createBulk(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
      assertTrue(response.message().contains("Atomic must be a boolean if provided"));
    }

    @Test
    @DisplayName("Should handle thrown errors during bulk creation")
    void shouldHandleThrownErrorsDuringBulkCreation() {
      BlnkRequest thirdPartyRequest =
          TestMocks.createMockBlnkRequest(false, "Network error occurred");
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .atomic(true)
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Test transaction")
                          .precision(100)
                          .reference("test_txn_001")
                          .source("@source_account")
                          .destination("@destination_account")));

      ApiResponse<JsonNode> response = transactions.createBulk(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/bulk", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertNull(response.data());
      assertEquals(500, response.status());
      assertEquals("Network error occurred", response.message());
    }

    @Test
    @DisplayName("Should handle bulk transactions with multiple sources")
    void shouldHandleBulkTransactionsWithMultipleSources() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .atomic(false)
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(10000)
                          .currency("USD")
                          .description("Multi-source transaction")
                          .precision(100)
                          .reference("multi_source_txn_001")
                          .sources(
                              List.of(
                                  MultipleSourcesT.create()
                                      .identifier("@source_account_1")
                                      .distribution("60%")
                                      .narration("Primary source"),
                                  MultipleSourcesT.create()
                                      .identifier("@source_account_2")
                                      .distribution("40%")
                                      .narration("Secondary source")))
                          .destination("@destination_account")));

      ApiResponse<JsonNode> bulkResponse = transactions.createBulk(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/bulk", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, bulkResponse.status());
    }

    @Test
    @DisplayName("createBulk forwards skip_queue on bulk request")
    void createBulkForwardsSkipQueueOnBulkRequest() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkTransactions data =
          BulkTransactions.create()
              .skipQueue(true)
              .transactions(
                  List.of(
                      CreateTransactions.create()
                          .amount(1000)
                          .currency("USD")
                          .description("Bulk txn with skip_queue")
                          .metaData(Map.of("department", "sales", "project", "Q4_campaign"))
                          .precision(100)
                          .reference("bulk_skip_queue_001")
                          .source("@source_account_1")
                          .destination("@destination_account_1"),
                      CreateTransactions.create()
                          .amount(2000)
                          .currency("USD")
                          .description("Bulk txn 2")
                          .metaData(Map.of("department", "marketing", "project", "Q4_campaign"))
                          .precision(100)
                          .reference("bulk_skip_queue_002")
                          .source("@source_account_2")
                          .destination("@destination_account_2")));

      ApiResponse<JsonNode> bulkResponse = transactions.createBulk(data);

      assertEquals(
          List.of(new CapturingRequest.Call("transactions/bulk", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, bulkResponse.status());
    }

    @Test
    @DisplayName("createBulk rejects oversized transactions array")
    void createBulkRejectsOversizedTransactionsArray() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      List<CreateTransactions> items = new ArrayList<>();
      for (int i = 0; i < TransactionConstants.MAX_BULK_CREATE_ITEMS + 1; i++) {
        items.add(
            CreateTransactions.create()
                .amount(1000)
                .currency("USD")
                .description("Bulk txn " + i)
                .precision(100)
                .reference("bulk_max_ref_" + i)
                .source("@source_account")
                .destination("@destination_account"));
      }
      BulkTransactions data = BulkTransactions.create().transactions(items);

      ApiResponse<JsonNode> response = transactions.createBulk(data);

      assertEquals(List.of(), capturedRequest.calls);
      assertNull(response.data());
      assertEquals(400, response.status());
      assertTrue(
          response
              .message()
              .contains(
                  "Too many transactions; max is "
                      + TransactionConstants.MAX_BULK_CREATE_ITEMS
                      + "."));
    }
  }

  @Nested
  @DisplayName("bulkCommitInflight")
  class BulkCommitInflight {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("commits inflight transactions with valid data")
    void commitsInflightTransactionsWithValidData() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkCommitInflightRequest data =
          BulkCommitInflightRequest.create()
              .transactions(
                  List.of(
                      BulkCommitInflightItem.create()
                          .transactionId("txn_11111111-1111-4111-8111-111111111111"),
                      BulkCommitInflightItem.create()
                          .transactionId("txn_22222222-2222-4222-8222-222222222222")
                          .amount(40),
                      BulkCommitInflightItem.create()
                          .transactionId("txn_33333333-3333-4333-8333-333333333333")
                          .preciseAmount(125034)));

      ApiResponse<JsonNode> response = transactions.bulkCommitInflight(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/inflight/bulk/commit", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("rejects empty transactions array")
    void rejectsEmptyTransactionsArray() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response =
          transactions.bulkCommitInflight(
              BulkCommitInflightRequest.create().transactions(List.of()));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("Transactions array cannot be empty.", response.message());
    }

    @Test
    @DisplayName("rejects too many transactions")
    void rejectsTooManyTransactions() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      List<BulkCommitInflightItem> items = new ArrayList<>();
      for (int i = 0; i < TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + 1; i++) {
        items.add(BulkCommitInflightItem.create().transactionId("txn_test"));
      }

      ApiResponse<JsonNode> response =
          transactions.bulkCommitInflight(BulkCommitInflightRequest.create().transactions(items));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals(
          "Too many transactions; max is " + TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + ".",
          response.message());
    }

    @Test
    @DisplayName("rejects missing transaction_id")
    void rejectsMissingTransactionId() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response =
          transactions.bulkCommitInflight(
              BulkCommitInflightRequest.create()
                  .transactions(List.of(BulkCommitInflightItem.create().transactionId(""))));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("transaction_id is required at index 0.", response.message());
    }

    @Test
    @DisplayName("bulkCommitInflight forwards skip_queue on request")
    void bulkCommitInflightForwardsSkipQueueOnRequest() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkCommitInflightRequest data =
          BulkCommitInflightRequest.create()
              .skipQueue(true)
              .transactions(
                  List.of(
                      BulkCommitInflightItem.create()
                          .transactionId("txn_11111111-1111-4111-8111-111111111111")));

      ApiResponse<JsonNode> response = transactions.bulkCommitInflight(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/inflight/bulk/commit", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }
  }

  @Nested
  @DisplayName("bulkVoidInflight")
  class BulkVoidInflight {

    private BlnkRequest thirdPartyRequest;

    @BeforeEach
    void beforeEach() {
      thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    }

    @Test
    @DisplayName("voids inflight transactions with valid data")
    void voidsInflightTransactionsWithValidData() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkVoidInflightRequest data =
          BulkVoidInflightRequest.create()
              .transactionIds(
                  List.of(
                      "txn_11111111-1111-4111-8111-111111111111",
                      "txn_22222222-2222-4222-8222-222222222222"));

      ApiResponse<JsonNode> response = transactions.bulkVoidInflight(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/inflight/bulk/void", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("rejects empty transaction_ids array")
    void rejectsEmptyTransactionIdsArray() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response =
          transactions.bulkVoidInflight(
              BulkVoidInflightRequest.create().transactionIds(List.of()));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("transaction_ids array cannot be empty.", response.message());
    }

    @Test
    @DisplayName("rejects too many transaction_ids")
    void rejectsTooManyTransactionIds() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      List<String> transactionIds =
          Collections.nCopies(TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + 1, "txn_test");

      ApiResponse<JsonNode> response =
          transactions.bulkVoidInflight(
              BulkVoidInflightRequest.create().transactionIds(transactionIds));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals(
          "Too many transaction_ids; max is " + TransactionConstants.MAX_BULK_INFLIGHT_ITEMS + ".",
          response.message());
    }

    @Test
    @DisplayName("rejects missing transaction_id")
    void rejectsMissingTransactionId() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      ApiResponse<JsonNode> response =
          transactions.bulkVoidInflight(
              BulkVoidInflightRequest.create().transactionIds(List.of("")));

      assertEquals(List.of(), capturedRequest.calls);
      assertEquals(400, response.status());
      assertEquals("transaction_id is required at index 0.", response.message());
    }

    @Test
    @DisplayName("bulkVoidInflight forwards skip_queue on request")
    void bulkVoidInflightForwardsSkipQueueOnRequest() {
      CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
      Transactions transactions = newTransactions(capturedRequest);

      BulkVoidInflightRequest data =
          BulkVoidInflightRequest.create()
              .skipQueue(true)
              .transactionIds(List.of("txn_11111111-1111-4111-8111-111111111111"));

      ApiResponse<JsonNode> response = transactions.bulkVoidInflight(data);

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "transactions/inflight/bulk/void", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }
  }
}
