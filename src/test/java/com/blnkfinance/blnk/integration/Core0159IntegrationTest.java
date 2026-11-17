package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.FilterParams;
import com.blnkfinance.blnk.types.GetBalanceRequest;
import com.blnkfinance.blnk.types.IdentityData;
import com.blnkfinance.blnk.types.MonitorCondition;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.types.MonitorTrigger;
import com.blnkfinance.blnk.types.UpdateMonitorData;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live coverage of the Core 0.15.9 alignment: monitor trigger and partial
 * update, inflight commit reference, overdraft_limit, balance include and
 * identity paging through the filter route. Gated on {@code BLNK_E2E=1}; if
 * the flag is set and Core is unreachable, these tests fail rather than skip.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("Core 0.15.9 SDK capabilities")
class Core0159IntegrationTest {

  private static final Blnk client =
      Blnk.init(
          TestUtils.BLNK_API_KEY,
          BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  private static String createLedger(String name) {
    ApiResponse<JsonNode> response =
        client.ledgers().create(CreateLedger.create().name(name + " " + UUID.randomUUID()));
    assertEquals(201, response.status(), "ledger create: " + response.message());
    return response.data().get("ledger_id").asText();
  }

  private static String createBalance(String ledgerId, String identityId) {
    CreateLedgerBalance body = CreateLedgerBalance.create().ledgerId(ledgerId).currency("USD");
    if (identityId != null) {
      body.identityId(identityId);
    }
    ApiResponse<JsonNode> response = client.ledgerBalances().create(body);
    assertEquals(201, response.status(), "balance create: " + response.message());
    return response.data().get("balance_id").asText();
  }

  private static String createIdentity(String firstName) {
    ApiResponse<JsonNode> response =
        client
            .identity()
            .create(
                IdentityData.create()
                    .identityType("individual")
                    .firstName(firstName)
                    .lastName("Core0159")
                    .emailAddress("core0159@example.com")
                    .category("test"));
    assertEquals(201, response.status(), "identity create: " + response.message());
    return response.data().get("identity_id").asText();
  }

  private static CreateTransactions txn(String source, String destination, double amount) {
    return CreateTransactions.create()
        .amount(amount)
        .precision(100)
        .currency("USD")
        .description("Core 0.15.9 integration")
        .reference(TestUtils.generateRandomNumbersWithPrefix("j0159", 12))
        .source(source)
        .destination(destination)
        .skipQueue(true);
  }

  private static JsonNode getMonitor(String monitorId) {
    ApiResponse<JsonNode> response = client.balanceMonitor().get(monitorId);
    assertEquals(200, response.status(), response.message());
    return response.data();
  }

  @Test
  @DisplayName("monitor trigger is stored and updatePartial keeps every field it omits")
  void monitorTriggerAndPartialUpdate() {
    String balanceId = createBalance(createLedger("0.15.9 monitor"), null);

    ApiResponse<JsonNode> created =
        client
            .balanceMonitor()
            .create(
                MonitorData.create()
                    .balanceId(balanceId)
                    .description("Below $100")
                    .callBackUrl("https://example.com/hooks/monitor")
                    .condition(
                        MonitorCondition.create()
                            .field("balance")
                            .operator("<")
                            .value(100)
                            .precision(100))
                    .trigger(MonitorTrigger.LEVEL));
    assertEquals(201, created.status(), created.message());
    String monitorId = created.data().get("monitor_id").asText();

    JsonNode stored = getMonitor(monitorId);
    assertEquals("level", stored.get("trigger").asText());
    // value is in major units: $100 at precision 100 is 10000 minor units.
    assertEquals(10000L, stored.get("condition").get("precise_value").asLong());

    // Description only: condition and trigger stay.
    ApiResponse<JsonNode> renamed =
        client
            .balanceMonitor()
            .updatePartial(monitorId, UpdateMonitorData.create().description("Low balance"));
    assertEquals(200, renamed.status(), renamed.message());
    stored = getMonitor(monitorId);
    assertEquals("Low balance", stored.get("description").asText());
    assertEquals("balance", stored.get("condition").get("field").asText());
    assertEquals("<", stored.get("condition").get("operator").asText());
    assertEquals(100.0, stored.get("condition").get("value").asDouble());
    assertEquals("level", stored.get("trigger").asText());

    // An explicit zero threshold, and a trigger change.
    ApiResponse<JsonNode> zeroed =
        client
            .balanceMonitor()
            .updatePartial(
                monitorId,
                UpdateMonitorData.create()
                    .trigger(MonitorTrigger.EDGE)
                    .condition(MonitorCondition.create().value(0)));
    assertEquals(200, zeroed.status(), zeroed.message());
    stored = getMonitor(monitorId);
    assertEquals(0.0, stored.get("condition").get("value").asDouble());
    assertEquals(0L, stored.get("condition").get("precise_value").asLong());
    assertEquals("edge", stored.get("trigger").asText());
    assertEquals("Low balance", stored.get("description").asText());

    // Precision without value is ignored, and Core says so.
    ApiResponse<JsonNode> precisionOnly =
        client
            .balanceMonitor()
            .updatePartial(
                monitorId,
                UpdateMonitorData.create().condition(MonitorCondition.create().precision(1000)));
    assertEquals(200, precisionOnly.status(), precisionOnly.message());
    assertTrue(precisionOnly.data().get("precision_unchanged").asBoolean());
    assertEquals(100.0, getMonitor(monitorId).get("condition").get("precision").asDouble());

    // "" clears the description.
    ApiResponse<JsonNode> cleared =
        client.balanceMonitor().updatePartial(monitorId, UpdateMonitorData.create().description(""));
    assertEquals(200, cleared.status(), cleared.message());
    // Core omits an empty description from the JSON, so path() reads "".
    assertEquals("", getMonitor(monitorId).path("description").asText());
  }

  @Test
  @DisplayName("a fractional threshold round-trips through create, get, list and updatePartial")
  void fractionalMonitorValueReadsBack() {
    String balanceId = createBalance(createLedger("0.15.9 fractional monitor"), null);

    ApiResponse<JsonNode> created =
        client
            .balanceMonitor()
            .create(
                MonitorData.create()
                    .balanceId(balanceId)
                    .condition(
                        MonitorCondition.create()
                            .field("balance")
                            .operator(">=")
                            .value(100.5)
                            .precision(100)));
    assertEquals(201, created.status(), created.message());
    String monitorId = created.data().get("monitor_id").asText();

    JsonNode stored = getMonitor(monitorId);
    assertEquals(100.5, stored.get("condition").get("value").asDouble());
    assertEquals(10050L, stored.get("condition").get("precise_value").asLong());

    ApiResponse<JsonNode> listed = client.balanceMonitor().listByBalanceId(balanceId);
    assertEquals(200, listed.status(), listed.message());
    assertEquals(100.5, listed.data().get(0).get("condition").get("value").asDouble());

    ApiResponse<JsonNode> moved =
        client
            .balanceMonitor()
            .updatePartial(
                monitorId, UpdateMonitorData.create().condition(MonitorCondition.create().value(250.75)));
    assertEquals(200, moved.status(), moved.message());
    stored = getMonitor(monitorId);
    assertEquals(250.75, stored.get("condition").get("value").asDouble());
    assertEquals(25075L, stored.get("condition").get("precise_value").asLong());
  }

  @Test
  @DisplayName("inflight commit with a reference applies once")
  void inflightCommitReferenceIsIdempotent() {
    String ledgerId = createLedger("0.15.9 inflight");
    String destination = createBalance(ledgerId, null);

    ApiResponse<JsonNode> inflight =
        client
            .transactions()
            .create(txn("@Core0159Funding", destination, 40).allowOverdraft(true).inflight(true));
    assertEquals(201, inflight.status(), inflight.message());
    String inflightId = inflight.data().get("transaction_id").asText();

    String reference = "commit-" + inflightId;
    ApiResponse<JsonNode> first =
        client
            .transactions()
            .updateStatus(
                inflightId,
                UpdateTransactionStatus.create().status("commit").reference(reference).skipQueue(true));
    assertTrue(first.status() < 300, first.status() + " " + first.message());

    ApiResponse<JsonNode> repeat =
        client
            .transactions()
            .updateStatus(
                inflightId,
                UpdateTransactionStatus.create().status("commit").reference(reference).skipQueue(true));
    assertTrue(repeat.status() < 300, repeat.status() + " " + repeat.message());
    assertEquals(
        first.data().get("transaction_id").asText(), repeat.data().get("transaction_id").asText());

    JsonNode balance = client.ledgerBalances().get(destination).data();
    assertEquals(4000L, balance.get("balance").asLong());
  }

  @Test
  @DisplayName("overdraft_limit caps how far the source can go below zero")
  void overdraftLimitIsEnforced() {
    String ledgerId = createLedger("0.15.9 overdraft");
    String source = createBalance(ledgerId, null);
    String destination = createBalance(ledgerId, null);

    ApiResponse<JsonNode> withinLimit =
        client
            .transactions()
            .create(txn(source, destination, 150).allowOverdraft(true).overdraftLimit(200));
    assertEquals(201, withinLimit.status(), withinLimit.message());
    assertEquals("APPLIED", withinLimit.data().get("status").asText());

    ApiResponse<JsonNode> overLimit =
        client
            .transactions()
            .create(txn(source, destination, 100).allowOverdraft(true).overdraftLimit(200));
    assertTrue(
        overLimit.status() >= 400 || "REJECTED".equals(overLimit.data().path("status").asText()),
        overLimit.status() + " " + overLimit.message());

    assertEquals(-15000L, client.ledgerBalances().get(source).data().get("balance").asLong());
  }

  @Test
  @DisplayName("get balance include expands identity and ledger")
  void balanceIncludeExpandsIdentityAndLedger() {
    String identityId = createIdentity("Include");
    String ledgerId = createLedger("0.15.9 include");
    String balanceId = createBalance(ledgerId, identityId);

    ApiResponse<JsonNode> expanded =
        client
            .ledgerBalances()
            .get(
                balanceId,
                GetBalanceRequest.create()
                    .include(GetBalanceRequest.INCLUDE_IDENTITY, GetBalanceRequest.INCLUDE_LEDGER));
    assertEquals(200, expanded.status(), expanded.message());
    assertEquals(identityId, expanded.data().get("identity").get("identity_id").asText());
    assertEquals(ledgerId, expanded.data().get("ledger").get("ledger_id").asText());
  }

  @Test
  @DisplayName("search().filter pages identities; GET /identities is not paged")
  void identityPagingUsesTheFilterRoute() {
    for (int i = 0; i < 3; i++) {
      createIdentity("Page" + i);
    }

    ApiResponse<JsonNode> page1 =
        client
            .search()
            .filter(FilterParams.create().filters(List.of()).limit(2).offset(0), "identities");
    ApiResponse<JsonNode> page2 =
        client
            .search()
            .filter(FilterParams.create().filters(List.of()).limit(2).offset(2), "identities");
    assertEquals(200, page1.status(), page1.message());
    assertEquals(200, page2.status(), page2.message());
    assertEquals(2, page1.data().size());
    assertTrue(page2.data().size() >= 1);

    Set<String> firstPageIds = new HashSet<>();
    page1.data().forEach(row -> firstPageIds.add(row.get("identity_id").asText()));
    page2.data().forEach(
        row -> assertFalse(firstPageIds.contains(row.get("identity_id").asText())));

    // Why identity().list() takes no ListOptions: Core 0.15.9 ignores them
    // without filters and returns every identity.
    ApiResponse<JsonNode> all = client.identity().list();
    assertEquals(200, all.status(), all.message());
    assertTrue(all.data().size() >= 3, "GET /identities returned " + all.data().size());
  }
}
