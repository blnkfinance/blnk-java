package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateBalanceSnapshotRequest;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.GetBalanceAtRequest;
import com.blnkfinance.blnk.types.GetBalanceRequest;
import com.blnkfinance.blnk.types.UpdateBalanceIdentity;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the LedgerBalances service: balance creation, lookups,
 * snapshots, and identity updates. Note: a few assertion labels intentionally
 * name a status code other than the one asserted; the asserted values are the
 * ones that matter.
 */
@DisplayName("Ledger Balance Tests")
class LedgerBalancesTest {

  /** Logger shared by every test in the suite. */
  private static final BlnkLogger mockLogger = TestMocks.createMockLogger();

  private static LedgerBalances service(CapturingRequest capturedRequest) {
    return new LedgerBalances(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
  }

  @Test
  @DisplayName("it should create a ledger balance when valid data is provided")
  void itShouldCreateALedgerBalanceWhenValidDataIsProvided() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .currency("USD")
            .ledgerId(TestMocks.LEDGER_ID)
            .metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    // Verify that the request function was called and with the right parameters.
    assertEquals(
        List.of(new CapturingRequest.Call("balances", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status(), "Response is 201");
    assertEquals(TestMocks.LEDGER_ID, response.data().get("ledger_id").asText());
  }

  @Test
  @DisplayName("it should handle missing optional fields")
  void itShouldHandleMissingOptionalFields() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .currency("USD")
            .ledgerId(TestMocks.LEDGER_ID)
            .metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    // Verify that the request function was called and with the right parameters.
    assertEquals(
        List.of(new CapturingRequest.Call("balances", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status(), "Response is 200"); // label intentionally mismatched
    assertNull(response.data().get("identity_id")); // key absent from the response entirely
  }

  @Test
  @DisplayName("it should handle missing required fields")
  void itShouldHandleMissingRequiredFields() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    // Simulates a caller omitting the required ledger_id.
    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .currency("USD")
            .metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    // Request won't get called since this fails validation.
    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status(), "Response is 500"); // label intentionally mismatched
    assertNull(response.data());
  }

  @Test
  @DisplayName("it should handle thrown during balance creation")
  void itShouldHandleThrownDuringBalanceCreation() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, "Network Error");
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .currency("USD")
            .ledgerId(TestMocks.LEDGER_ID)
            .metaData(Map.of("company_name", "Test Company"));

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    // The request WAS called (recorded), then threw.
    assertEquals(
        List.of(new CapturingRequest.Call("balances", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(500, response.status(), "Response is 500");
    assertNull(response.data());
    assertEquals("Network Error", response.message());
  }

  @Test
  @DisplayName("Create forwards track_fund_lineage and allocation_strategy")
  void createForwardsTrackFundLineageAndAllocationStrategy() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .ledgerId(TestMocks.LEDGER_ID)
            .identityId("idt_3b63c8da-af29-4cc3-ad38-df17d87456e6")
            .currency("USD")
            .trackFundLineage(true)
            .allocationStrategy("PROPORTIONAL");

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    assertEquals(
        List.of(new CapturingRequest.Call("balances", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
  }

  @Test
  @DisplayName("Create rejects invalid allocation_strategy")
  void createRejectsInvalidAllocationStrategy() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .ledgerId(TestMocks.LEDGER_ID)
            .currency("USD")
            .allocationStrategy("INVALID");

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals(
        "allocation_strategy must be one of FIFO, LIFO, or PROPORTIONAL", response.message());
  }

  @Test
  @DisplayName("Create forwards indicator for a General Ledger balance")
  void createForwardsIndicatorForGeneralLedgerBalance() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .ledgerId("general_ledger_id")
            .currency("USD")
            .indicator("@Revenue");

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    assertEquals(
        List.of(new CapturingRequest.Call("balances", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
    assertEquals("@Revenue", data.toJson().get("indicator").asText());
  }

  @Test
  @DisplayName("Create rejects indicator on a non-general ledger")
  void createRejectsIndicatorOnNonGeneralLedger() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.create(
            CreateLedgerBalance.create()
                .ledgerId(TestMocks.LEDGER_ID)
                .currency("USD")
                .indicator("@Revenue"));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals(
        "indicator is only valid when ledger_id is general_ledger_id", response.message());
  }

  @Test
  @DisplayName("it should handle meta_data if it is not an object")
  void itShouldHandleMetaDataIfItIsNotAnObject() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    // Simulates a caller passing a meta_data value that is not an object.
    CreateLedgerBalance data =
        CreateLedgerBalance.create()
            .currency("USD")
            .ledgerId(TestMocks.LEDGER_ID)
            .metaData(5);

    ApiResponse<JsonNode> response = ledgerBalance.create(data);

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status(), "Response is 400");
    assertNull(response.data());
    assertEquals("meta_data must be a valid object if provided", response.message());
  }

  @Test
  @DisplayName("getLineage calls correct endpoint")
  void getLineageCallsCorrectEndpoint() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ApiResponse<JsonNode> response = ledgerBalance.getLineage(balanceId);

    assertEquals(
        List.of(new CapturingRequest.Call("balances/" + balanceId + "/lineage", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("getLineage rejects empty balance id")
  void getLineageRejectsEmptyBalanceId() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response = ledgerBalance.getLineage("");

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("balance id is required", response.message());
  }

  @Test
  @DisplayName("getByIndicator calls correct endpoint")
  void getByIndicatorCallsCorrectEndpoint() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response = ledgerBalance.getByIndicator("@World", "USD");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/indicator/%40World/currency/USD", null, "GET", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("getByIndicator path-escapes special characters")
  void getByIndicatorPathEscapesSpecialCharacters() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ledgerBalance.getByIndicator("@user/name", "USD/EUR");

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/indicator/%40user%2Fname/currency/USD%2FEUR", null, "GET", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("getByIndicator rejects empty indicator")
  void getByIndicatorRejectsEmptyIndicator() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response = ledgerBalance.getByIndicator("", "USD");

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("indicator is required", response.message());
  }

  @Test
  @DisplayName("getByIndicator rejects empty currency")
  void getByIndicatorRejectsEmptyCurrency() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response = ledgerBalance.getByIndicator("@World", "");

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("currency is required", response.message());
  }

  @Test
  @DisplayName("updateIdentity calls PUT /balances/{id}/identity")
  void updateIdentityCallsPutBalancesIdIdentity() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";
    UpdateBalanceIdentity data =
        UpdateBalanceIdentity.create().identityId("idt_3b63c8da-af29-4cc3-ad38-df17d87456e6");

    ApiResponse<JsonNode> response = ledgerBalance.updateIdentity(balanceId, data);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/" + balanceId + "/identity", data.toJson(), "PUT", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("updateIdentity rejects empty balance id")
  void updateIdentityRejectsEmptyBalanceId() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.updateIdentity(
            "", UpdateBalanceIdentity.create().identityId("idt_3b63c8da-af29-4cc3-ad38-df17d87456e6"));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("balance id is required", response.message());
  }

  @Test
  @DisplayName("updateIdentity rejects missing identity_id")
  void updateIdentityRejectsMissingIdentityId() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.updateIdentity("bln_123", UpdateBalanceIdentity.create());

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("identity_id is required", response.message());
  }

  @Test
  @DisplayName("createSnapshot calls POST /balances-snapshots")
  void createSnapshotCallsPostBalancesSnapshots() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response = ledgerBalance.createSnapshot();

    assertEquals(
        List.of(new CapturingRequest.Call("balances-snapshots", null, "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
  }

  @Test
  @DisplayName("createSnapshot forwards batch_size query param")
  void createSnapshotForwardsBatchSizeQueryParam() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ledgerBalance.createSnapshot(CreateBalanceSnapshotRequest.create().batchSize(500));

    assertEquals(
        List.of(new CapturingRequest.Call("balances-snapshots?batch_size=500", null, "POST", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("createSnapshot omits query when batch_size is zero")
  void createSnapshotOmitsQueryWhenBatchSizeIsZero() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ledgerBalance.createSnapshot(CreateBalanceSnapshotRequest.create().batchSize(0));

    assertEquals(
        List.of(new CapturingRequest.Call("balances-snapshots", null, "POST", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("createSnapshot rejects negative batch_size")
  void createSnapshotRejectsNegativeBatchSize() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.createSnapshot(CreateBalanceSnapshotRequest.create().batchSize(-1));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("batch_size must be positive", response.message());
  }

  @Test
  @DisplayName("get calls GET /balances/{id}")
  void getCallsGetBalancesId() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ledgerBalance.get(balanceId);

    assertEquals(
        List.of(new CapturingRequest.Call("balances/" + balanceId, null, "GET", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("get forwards from_source query param")
  void getForwardsFromSourceQueryParam() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ledgerBalance.get(balanceId, GetBalanceRequest.create().fromSource(true));

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/" + balanceId + "?from_source=true", null, "GET", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("get forwards with_queued query param")
  void getForwardsWithQueuedQueryParam() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ledgerBalance.get(balanceId, GetBalanceRequest.create().withQueued(true));

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/" + balanceId + "?with_queued=true", null, "GET", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("get forwards from_source and with_queued query params")
  void getForwardsFromSourceAndWithQueuedQueryParams() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ledgerBalance.get(
        balanceId, GetBalanceRequest.create().fromSource(true).withQueued(true));

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/" + balanceId + "?from_source=true&with_queued=true",
                null,
                "GET",
                null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("get rejects invalid from_source")
  void getRejectsInvalidFromSource() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.get("bln_123", GetBalanceRequest.create().fromSource("true"));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("from_source must be a boolean if provided", response.message());
  }

  @Test
  @DisplayName("get rejects invalid with_queued")
  void getRejectsInvalidWithQueued() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.get("bln_123", GetBalanceRequest.create().withQueued("true"));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("with_queued must be a boolean if provided", response.message());
  }

  @Test
  @DisplayName("getAt calls GET /balances/{id}/at")
  void getAtCallsGetBalancesIdAt() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ledgerBalance.getAt(balanceId, GetBalanceAtRequest.create().timestamp("2025-02-24T08:55:26Z"));

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/" + balanceId + "/at?timestamp=2025-02-24T08%3A55%3A26Z",
                null,
                "GET",
                null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("getAt forwards from_source query param")
  void getAtForwardsFromSourceQueryParam() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);
    String balanceId = "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f";

    ledgerBalance.getAt(
        balanceId,
        GetBalanceAtRequest.create().timestamp("2025-02-24T08:55:26Z").fromSource(true));

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "balances/" + balanceId + "/at?timestamp=2025-02-24T08%3A55%3A26Z&from_source=true",
                null,
                "GET",
                null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("getAt rejects empty balance id")
  void getAtRejectsEmptyBalanceId() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.getAt("", GetBalanceAtRequest.create().timestamp("2025-02-24T08:55:26Z"));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("balance id is required", response.message());
  }

  @Test
  @DisplayName("getAt rejects empty timestamp")
  void getAtRejectsEmptyTimestamp() {
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 200);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    LedgerBalances ledgerBalance = service(capturedRequest);

    ApiResponse<JsonNode> response =
        ledgerBalance.getAt("bln_123", GetBalanceAtRequest.create().timestamp(""));

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals("timestamp is required", response.message());
  }
}
