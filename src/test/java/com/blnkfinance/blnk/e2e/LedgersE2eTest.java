package com.blnkfinance.blnk.e2e;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.Criteria;
import com.blnkfinance.blnk.types.IdentityData;
import com.blnkfinance.blnk.types.Matcher;
import com.blnkfinance.blnk.types.MonitorCondition;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.types.RunReconData;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end flows against a live Blnk Core instance at
 * {@code http://localhost:5001}. Gated on {@code BLNK_E2E=1}. Later tests
 * reuse ids captured by earlier ones, so execution is ordered.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgersE2eTest {

  private static final Blnk client =
      Blnk.init("", BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  private static String ledgerId = "";
  private static String ledgerBalanceId = "";
  private static String transactionId = "";
  private static String identityId = "";
  private static String balanceMonitorId = "";
  private static String uploadId = "";
  private static String matchingRuleId = "";

  private static final List<String> LEDGER_BALANCE_FIELDS =
      List.of(
          "balance",
          "version",
          "inflight_balance",
          "credit_balance",
          "debit_balance",
          "inflight_debit_balance",
          "inflight_credit_balance",
          "ledger_id",
          "identity_id",
          "balance_id",
          "currency",
          "created_at",
          "inflight_expires_at");

  private static final List<String> TRANSACTION_FIELDS =
      List.of(
          "transaction_id",
          "amount",
          "currency",
          "source",
          "destination",
          "reference",
          "status",
          "created_at",
          "precision");

  private static void assertHasProps(JsonNode node, List<String> props) {
    for (String prop : props) {
      assertTrue(node.has(prop), "expected property: " + prop);
    }
  }

  // --- Ledgers end to end test ---

  @Test
  @Order(1)
  @DisplayName("create a ledger")
  void createALedger() {
    ApiResponse<JsonNode> response =
        client.ledgers().create(CreateLedger.create().name("Test Ledger"));

    assertNotNull(response, "response is successful");
    assertEquals(201, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("ledger_id", "name", "created_at"));
    assertTrue(response.data().get("created_at").isTextual());
    assertTrue(response.data().get("ledger_id").isTextual());
    ledgerId = response.data().get("ledger_id").asText();
  }

  @Test
  @Order(2)
  @DisplayName("create a ledger fail")
  void createALedgerFail() {
    // Empty object fails validation locally.
    ApiResponse<JsonNode> response = client.ledgers().create(CreateLedger.create());

    assertNotNull(response, "response is returned");
    assertEquals(400, response.status());
  }

  @Test
  @Order(3)
  @DisplayName("It should retrieve a ledger created")
  void retrievesCreatedLedger() {
    ApiResponse<JsonNode> response = client.ledgers().get(ledgerId);

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("ledger_id", "name", "created_at"));
  }

  @Test
  @Order(4)
  @DisplayName("It should return 400")
  void ledgerGetReturns400() {
    ApiResponse<JsonNode> response = client.ledgers().get("123456789");

    assertNotNull(response, "response is returned");
    assertEquals(400, response.status());
  }

  // --- Identity ---

  @Test
  @Order(5)
  @DisplayName("create an individual identity")
  void createsIndividualIdentity() {
    ApiResponse<JsonNode> response =
        client
            .identity()
            .create(
                IdentityData.create()
                    .category("cutomer")
                    .city("Ikeja")
                    .country("NG")
                    .firstName("Test")
                    .emailAddress("test@test.com")
                    .identityType("individual")
                    .phoneNumber("+2348012345678")
                    .postCode("100001")
                    .state("Lagos")
                    .street("123 Test Street")
                    .dob("1996-02-25T00:00:00Z")
                    .gender("male")
                    .lastName("Test")
                    .nationality("Nigerian"));

    assertNotNull(response, "identity is created");
    assertEquals(201, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("identity_id", "created_at", "identity_type"));
    identityId = response.data().get("identity_id").asText();
  }

  @Test
  @Order(6)
  @DisplayName("list all identities")
  void listsAllIdentities() {
    ApiResponse<JsonNode> response = client.identity().list();

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isArray());
  }

  @Test
  @Order(7)
  @DisplayName("get identity by id")
  void getsIdentityById() {
    ApiResponse<JsonNode> response = client.identity().get(identityId);

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("identity_id", "created_at", "identity_type"));
  }

  @Test
  @Order(8)
  @DisplayName("update identity to organization")
  void updatesIdentityToOrganization() {
    ApiResponse<JsonNode> response =
        client
            .identity()
            .update(
                identityId,
                IdentityData.create()
                    .category("cutomer")
                    .city("Ikeja")
                    .country("NG")
                    .emailAddress("test@test.com")
                    .identityType("organization")
                    .organizationName("Test Organization")
                    .phoneNumber("+2348012345678")
                    .postCode("100001")
                    .state("Lagos")
                    .street("123 Test Street")
                    .dob("1996-02-25T00:00:00Z")
                    .gender("male"));

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
  }

  // --- Ledger balances ---

  @Test
  @Order(9)
  @DisplayName("It should create a balance")
  void createsABalance() {
    ApiResponse<JsonNode> response =
        client
            .ledgerBalances()
            .create(
                CreateLedgerBalance.create()
                    .currency("USD")
                    .ledgerId(ledgerId)
                    .identityId(identityId)
                    .metaData(Map.of()));

    assertNotNull(response, "response is returned");
    assertEquals(201, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), LEDGER_BALANCE_FIELDS);
    ledgerBalanceId = response.data().get("balance_id").asText();
  }

  @Test
  @Order(10)
  @DisplayName("get ledger balances")
  void getsLedgerBalances() {
    ApiResponse<JsonNode> response = client.ledgerBalances().get(ledgerBalanceId);

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), LEDGER_BALANCE_FIELDS);
  }

  @Test
  @Order(11)
  @DisplayName("it should return 400")
  void balanceGetReturns400() {
    ApiResponse<JsonNode> response = client.ledgerBalances().get("123456789");

    assertNotNull(response, "response is returned");
    assertEquals(400, response.status());
  }

  // --- Ledger balance transactions ---

  @Test
  @Order(12)
  @DisplayName("It should create a transaction on a balance")
  void createsTransactionOnBalance() {
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .create(
                CreateTransactions.create()
                    .amount(1000)
                    .currency("USD")
                    .description("Test transaction")
                    .precision(100)
                    .reference(TestUtils.generateRandomNumbersWithPrefix("test", 4))
                    .source("@bank-account")
                    .destination(ledgerBalanceId)
                    .inflight(true)
                    .allowOverdraft(true)
                    .metaData(Map.of()));

    assertNotNull(response, "response is returned");
    assertEquals(201, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), TRANSACTION_FIELDS);
    assertHasProps(response.data(), List.of("precise_amount"));
    transactionId = response.data().get("transaction_id").asText();
  }

  @Test
  @Order(13)
  @DisplayName("it should commit the transaction")
  void commitsTheTransaction() {
    // Sleep so the transaction enqueues.
    TestUtils.sleepSeconds(2);
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .updateStatus(transactionId, UpdateTransactionStatus.create().status("commit"));

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), TRANSACTION_FIELDS);
    assertHasProps(response.data(), List.of("precise_amount"));
  }

  @Test
  @Order(14)
  @DisplayName("it should void the transaction")
  void voidsTheTransaction() {
    TestUtils.sleepSeconds(2);
    ApiResponse<JsonNode> response =
        client
            .transactions()
            .updateStatus(transactionId, UpdateTransactionStatus.create().status("void"));

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), TRANSACTION_FIELDS);
  }

  // --- Balance Monitors ---

  @Test
  @Order(15)
  @DisplayName("It should create a balance monitor")
  void createsBalanceMonitor() {
    ApiResponse<JsonNode> response =
        client
            .balanceMonitor()
            .create(
                MonitorData.create()
                    .balanceId(ledgerBalanceId)
                    .condition(
                        MonitorCondition.create()
                            .field("credit_balance")
                            .operator(">=")
                            .precision(100)
                            .value(1000))
                    .description("Tier 1 account"));

    assertNotNull(response, "response is returned");
    assertEquals(201, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("monitor_id", "balance_id"));
    balanceMonitorId = response.data().get("monitor_id").asText();
  }

  @Test
  @Order(16)
  @DisplayName("It should list all balance monitors")
  void listsAllBalanceMonitors() {
    ApiResponse<JsonNode> response = client.balanceMonitor().list();

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isArray());
  }

  @Test
  @Order(17)
  @DisplayName("It should fail to get the balance monitor")
  void failsToGetBalanceMonitor() {
    ApiResponse<JsonNode> response = client.balanceMonitor().get("123456789");

    assertNotNull(response, "response is returned");
    assertEquals(400, response.status());
  }

  @Test
  @Order(18)
  @DisplayName("It should get a balance monitor")
  void getsBalanceMonitor() {
    ApiResponse<JsonNode> response = client.balanceMonitor().get(balanceMonitorId);

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
  }

  @Test
  @Order(19)
  @DisplayName("It should update a balance monitor")
  void updatesBalanceMonitor() {
    ApiResponse<JsonNode> response =
        client
            .balanceMonitor()
            .update(
                balanceMonitorId,
                MonitorData.create()
                    .balanceId(ledgerBalanceId)
                    .condition(
                        MonitorCondition.create()
                            .field("credit_balance")
                            .operator(">=")
                            .precision(100)
                            .value(1000))
                    .description("Tier 1 account"));

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
  }

  // --- Reconciliation ---

  @Test
  @Order(20)
  @DisplayName("It should create a matching rule")
  void createsMatchingRule() {
    ApiResponse<JsonNode> response =
        client
            .reconciliation()
            .createMatchingRule(
                Matcher.create()
                    .description("Test matching rule")
                    .criteria(
                        List.of(
                            Criteria.create()
                                .field("amount")
                                .operator("equals")
                                .allowableDrift(0.01)))
                    .name("Test matching rule"));

    assertNotNull(response, "response is returned");
    assertEquals(201, response.status());
    matchingRuleId = response.data().get("rule_id").asText();
  }

  @Test
  @Order(21)
  @DisplayName("it should fail to upload a reconciliation file")
  void failsToUploadReconciliationFile() {
    ApiResponse<JsonNode> response = client.reconciliation().upload("no path", "Stripe");

    assertNotNull(response, "response is returned");
    assertEquals(404, response.status());
  }

  @Test
  @Order(22)
  @DisplayName("it should upload a reconciliation file")
  void uploadsReconciliationFile() {
    ApiResponse<JsonNode> response = client.reconciliation().upload("file.csv", "Stripe");

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("upload_id"));
    uploadId = response.data().get("upload_id").asText();
  }

  @Test
  @Order(23)
  @DisplayName("Run Reconciliation")
  void runsReconciliation() {
    ApiResponse<JsonNode> response =
        client
            .reconciliation()
            .run(
                RunReconData.create()
                    .dryRun(true)
                    .groupingCriteria("amount")
                    .matchingRuleIds(List.of(matchingRuleId))
                    .strategy("one_to_many")
                    .uploadId(uploadId));

    assertNotNull(response, "response is returned");
    assertEquals(200, response.status());
    assertTrue(response.data().isObject());
    assertHasProps(response.data(), List.of("reconciliation_id"));
  }
}
