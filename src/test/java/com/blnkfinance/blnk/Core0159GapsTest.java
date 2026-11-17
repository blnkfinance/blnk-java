package com.blnkfinance.blnk;

import com.blnkfinance.blnk.testsupport.CapturingTransport;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.FilterParams;
import com.blnkfinance.blnk.types.GetBalanceRequest;
import com.blnkfinance.blnk.types.ListOptions;
import com.blnkfinance.blnk.types.MonitorCondition;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.types.MonitorTrigger;
import com.blnkfinance.blnk.types.UpdateMetadataData;
import com.blnkfinance.blnk.types.UpdateMonitorData;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-level tests for the Blnk Core 0.15.9 alignment. Each test drives a real
 * {@link Blnk} client through a capturing transport, so it asserts on the URL
 * and the encoded JSON body that would actually be sent, and decodes a
 * Core-shaped reply.
 */
@DisplayName("Core 0.15.9 gaps")
class Core0159GapsTest {

  private static final String BASE = "http://mock-api.com/";

  /** A client whose transport answers every request with {@code reply} (HTTP 200). */
  private static final class Harness {
    final CapturingTransport transport;
    final Blnk blnk;

    Harness(String reply) {
      BlnkTransport replying = (url, request) -> TransportResponse.builder()
          .ok(true)
          .status(200)
          .statusText("OK")
          .headers(request.headers())
          .text(() -> reply)
          .json(() -> BlnkJson.parse(reply))
          .build();
      transport = CapturingTransport.of(replying);
      blnk = new Blnk(
          "test_key",
          TestMocks.createMockBlnkClientOptions(),
          ServiceRegistry.defaultServices(),
          HttpClientUtils.FORMAT_RESPONSE,
          transport);
    }

    int requestCount() {
      return transport.calls.size();
    }

    String url() {
      return transport.calls.get(0).url();
    }

    String method() {
      return transport.calls.get(0).request().method();
    }

    JsonNode body() {
      byte[] bytes = transport.calls.get(0).request().body();
      return bytes == null ? null : BlnkJson.parse(new String(bytes, StandardCharsets.UTF_8));
    }
  }

  private static JsonNode json(String text) {
    return BlnkJson.parse(text);
  }

  private static MonitorCondition belowHundredDollars() {
    return MonitorCondition.create().field("balance").operator("<").value(100).precision(100);
  }

  @Nested
  @DisplayName("balance monitor trigger")
  class MonitorTriggerTests {

    @Test
    @DisplayName("create sends trigger and reads condition_state and precise_value back")
    void createSendsTrigger() {
      Harness h = new Harness("""
          {"monitor_id":"mon_1","balance_id":"bln_1","trigger":"level",
           "condition_state":false,
           "condition":{"field":"balance","operator":"<","value":100,"precision":100,
                        "precise_value":10000}}""");

      ApiResponse<JsonNode> response = h.blnk.balanceMonitor().create(
          MonitorData.create()
              .balanceId("bln_1")
              .condition(belowHundredDollars())
              .trigger(MonitorTrigger.LEVEL));

      assertEquals(BASE + "balance-monitors", h.url());
      assertEquals("POST", h.method());
      assertEquals(json("""
          {"balance_id":"bln_1",
           "condition":{"field":"balance","operator":"<","value":100,"precision":100},
           "trigger":"level"}"""), h.body());
      assertEquals(200, response.status());
      assertFalse(response.data().get("condition_state").asBoolean());
      assertEquals(10000L, response.data().get("condition").get("precise_value").asLong());
    }

    @Test
    @DisplayName("create without a trigger sends no trigger key (Core defaults to edge)")
    void createWithoutTriggerOmitsKey() {
      Harness h = new Harness("{\"monitor_id\":\"mon_1\"}");

      h.blnk.balanceMonitor().create(
          MonitorData.create().balanceId("bln_1").condition(belowHundredDollars()));

      assertFalse(h.body().has("trigger"));
    }

    @Test
    @DisplayName("create accepts an empty trigger, which Core reads as its default")
    void createAcceptsEmptyTrigger() {
      Harness h = new Harness("{\"monitor_id\":\"mon_1\"}");

      ApiResponse<JsonNode> response = h.blnk.balanceMonitor().create(
          MonitorData.create().balanceId("bln_1").condition(belowHundredDollars()).trigger(""));

      assertEquals(200, response.status());
      assertEquals("", h.body().get("trigger").asText());
    }

    @Test
    @DisplayName("create rejects an unknown or wrongly cased trigger without sending")
    void createRejectsBadTrigger() {
      for (String bad : new String[] {"EDGE", "sometimes", " level"}) {
        Harness h = new Harness("{}");
        ApiResponse<JsonNode> response = h.blnk.balanceMonitor().create(
            MonitorData.create()
                .balanceId("bln_1")
                .condition(belowHundredDollars())
                .trigger(bad));

        assertEquals(400, response.status(), bad);
        assertEquals("trigger must be either \"edge\" or \"level\" if provided", response.message());
        assertEquals(0, h.requestCount());
      }
    }
  }

  @Nested
  @DisplayName("balance monitor partial update")
  class MonitorPartialUpdateTests {

    private static final String UPDATED = "{\"message\":\"BalanceMonitor updated successfully\"}";

    @Test
    @DisplayName("updatePartial sends only the description")
    void sendsOnlyDescription() {
      Harness h = new Harness(UPDATED);

      ApiResponse<JsonNode> response = h.blnk.balanceMonitor().updatePartial(
          "mon_1", UpdateMonitorData.create().description("Low balance"));

      assertEquals(BASE + "balance-monitors/mon_1", h.url());
      assertEquals("PUT", h.method());
      assertEquals(json("{\"description\":\"Low balance\"}"), h.body());
      assertEquals(200, response.status());
      assertEquals("BalanceMonitor updated successfully", response.data().get("message").asText());
    }

    @Test
    @DisplayName("updatePartial can move the threshold to an explicit zero")
    void sendsExplicitZeroValue() {
      Harness h = new Harness(UPDATED);

      h.blnk.balanceMonitor().updatePartial(
          "mon_1", UpdateMonitorData.create().condition(MonitorCondition.create().value(0)));

      // Only "value" is sent: Core treats a lone value 0 as "move the line to zero".
      assertEquals(json("{\"condition\":{\"value\":0}}"), h.body());
    }

    @Test
    @DisplayName("updatePartial clears the description with an empty string")
    void clearsDescription() {
      Harness h = new Harness(UPDATED);

      h.blnk.balanceMonitor().updatePartial(
          "mon_1", UpdateMonitorData.create().description(""));

      assertEquals(json("{\"description\":\"\"}"), h.body());
    }

    @Test
    @DisplayName("updatePartial sends trigger and a partial condition, never call_back_url")
    void sendsTriggerAndPartialCondition() {
      Harness h = new Harness(UPDATED);

      h.blnk.balanceMonitor().updatePartial(
          "mon_1",
          UpdateMonitorData.create()
              .trigger(MonitorTrigger.EDGE)
              .condition(MonitorCondition.create().operator("<=").value(250)));

      assertEquals(
          json("{\"trigger\":\"edge\",\"condition\":{\"operator\":\"<=\",\"value\":250}}"),
          h.body());
      assertFalse(h.body().has("call_back_url"));
    }

    @Test
    @DisplayName("updatePartial surfaces precision_unchanged from Core")
    void surfacesPrecisionUnchanged() {
      Harness h = new Harness("""
          {"message":"BalanceMonitor updated successfully. precision was left unchanged because value was not sent with it.",
           "precision_unchanged":true}""");

      ApiResponse<JsonNode> response = h.blnk.balanceMonitor().updatePartial(
          "mon_1", UpdateMonitorData.create().condition(MonitorCondition.create().precision(1000)));

      assertEquals(json("{\"condition\":{\"precision\":1000}}"), h.body());
      assertTrue(response.data().get("precision_unchanged").asBoolean());
    }

    @Test
    @DisplayName("updatePartial rejects non-finite numbers, bad fields and empty bodies")
    void rejectsMalformedPartialBodies() {
      Harness h = new Harness(UPDATED);
      Map<String, UpdateMonitorData> cases = new java.util.LinkedHashMap<>();
      cases.put("condition.value must be a finite number if provided",
          UpdateMonitorData.create().condition(MonitorCondition.create().value(Double.NaN)));
      cases.put("condition.value must be a finite number if provided ",
          UpdateMonitorData.create().condition(
              MonitorCondition.create().value(Double.POSITIVE_INFINITY)));
      cases.put("condition.precision must be a finite number greater than 0 if provided",
          UpdateMonitorData.create().condition(
              MonitorCondition.create().precision(Double.NEGATIVE_INFINITY)));
      cases.put("condition.precision must be a finite number greater than 0 if provided ",
          UpdateMonitorData.create().condition(MonitorCondition.create().precision(0)));
      cases.put("condition.field must be one of balance, credit_balance, debit_balance, "
              + "inflight_balance, inflight_credit_balance, inflight_debit_balance if provided",
          UpdateMonitorData.create().condition(MonitorCondition.create().field("status")));
      cases.put("condition must set at least one of field, operator, value or precision",
          UpdateMonitorData.create().condition(MonitorCondition.create()));
      cases.put("at least one field must be set to update a monitor",
          UpdateMonitorData.create());
      cases.put("balance_id must be a non-empty string if provided",
          UpdateMonitorData.create().balanceId(" "));

      cases.forEach((message, body) -> {
        ApiResponse<JsonNode> response = h.blnk.balanceMonitor().updatePartial("mon_1", body);
        assertEquals(400, response.status(), message);
        assertEquals(message.strip(), response.message());
      });
      assertEquals(0, h.requestCount());
    }

    @Test
    @DisplayName("create rejects NaN and infinite condition numbers without sending")
    void createRejectsNonFiniteCondition() {
      Harness h = new Harness("{}");

      for (MonitorCondition condition : List.of(
          belowHundredDollars().value(Double.NaN),
          belowHundredDollars().precision(Double.POSITIVE_INFINITY))) {
        ApiResponse<JsonNode> response = h.blnk.balanceMonitor().create(
            MonitorData.create().balanceId("bln_1").condition(condition));
        assertEquals(400, response.status());
        assertEquals("condition must be a valid MonitorCondition object", response.message());
      }
      assertEquals(0, h.requestCount());
    }

    @Test
    @DisplayName("updatePartial validates the id and body before sending")
    void validatesBeforeSending() {
      Harness h = new Harness(UPDATED);

      assertEquals(400, h.blnk.balanceMonitor()
          .updatePartial("", UpdateMonitorData.create().description("x")).status());
      assertEquals(400, h.blnk.balanceMonitor().updatePartial("mon_1", null).status());

      ApiResponse<JsonNode> badTrigger = h.blnk.balanceMonitor()
          .updatePartial("mon_1", UpdateMonitorData.create().trigger("Level"));
      assertEquals("trigger must be either \"edge\" or \"level\" if provided", badTrigger.message());

      ApiResponse<JsonNode> badOperator = h.blnk.balanceMonitor().updatePartial(
          "mon_1", UpdateMonitorData.create().condition(MonitorCondition.create().operator("~")));
      assertEquals(
          "condition.operator must be one of != < <= = > >= if provided", badOperator.message());

      ApiResponse<JsonNode> badValue = h.blnk.balanceMonitor().updatePartial(
          "mon_1", UpdateMonitorData.create().condition(MonitorCondition.create().value("ten")));
      assertEquals("condition.value must be a finite number if provided", badValue.message());

      assertEquals(0, h.requestCount());
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("deprecated update still sends the full MonitorData")
    void deprecatedUpdateStillWorks() {
      Harness h = new Harness(UPDATED);

      ApiResponse<JsonNode> response = h.blnk.balanceMonitor().update(
          "mon_1",
          MonitorData.create()
              .balanceId("bln_1")
              .condition(belowHundredDollars())
              .trigger(MonitorTrigger.EDGE));

      assertEquals(200, response.status());
      assertEquals("PUT", h.method());
      assertEquals(BASE + "balance-monitors/mon_1", h.url());
      assertEquals(json("""
          {"balance_id":"bln_1",
           "condition":{"field":"balance","operator":"<","value":100,"precision":100},
           "trigger":"edge"}"""), h.body());
    }
  }

  @Nested
  @DisplayName("transactions")
  class TransactionTests {

    @Test
    @DisplayName("updateStatus sends reference for an idempotent commit")
    void inflightCommitSendsReference() {
      Harness h = new Harness("{\"transaction_id\":\"txn_2\",\"status\":\"APPLIED\"}");

      ApiResponse<JsonNode> response = h.blnk.transactions().updateStatus(
          "txn_1",
          UpdateTransactionStatus.create().status("commit").reference("commit-txn_1-1"));

      assertEquals(BASE + "transactions/inflight/txn_1", h.url());
      assertEquals("PUT", h.method());
      assertEquals(json("{\"status\":\"commit\",\"reference\":\"commit-txn_1-1\"}"), h.body());
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("updateStatus without a reference sends no reference key")
    void inflightWithoutReference() {
      Harness h = new Harness("{}");

      h.blnk.transactions().updateStatus("txn_1", UpdateTransactionStatus.create().status("void"));

      assertEquals(json("{\"status\":\"void\"}"), h.body());
    }

    @Test
    @DisplayName("create sends overdraft_limit")
    void createSendsOverdraftLimit() {
      Harness h = new Harness("{\"transaction_id\":\"txn_1\",\"status\":\"QUEUED\"}");

      ApiResponse<JsonNode> response = h.blnk.transactions().create(
          CreateTransactions.create()
              .amount(50)
              .precision(100)
              .currency("USD")
              .reference("ref_overdraft_1")
              .description("Overdraft test")
              .source("bln_src")
              .destination("bln_dst")
              .allowOverdraft(true)
              .overdraftLimit(200));

      assertEquals(BASE + "transactions", h.url());
      JsonNode body = h.body();
      assertTrue(body.get("allow_overdraft").asBoolean());
      assertEquals(200.0, body.get("overdraft_limit").asDouble());
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("create without overdraft_limit leaves the body unchanged")
    void createWithoutOverdraftLimit() {
      Harness h = new Harness("{}");

      h.blnk.transactions().create(
          CreateTransactions.create()
              .amount(50)
              .precision(100)
              .currency("USD")
              .reference("ref_plain_1")
              .description("Overdraft test")
              .source("bln_src")
              .destination("bln_dst"));

      assertFalse(h.body().has("overdraft_limit"));
    }

    @Test
    @DisplayName("create rejects a negative overdraft_limit without sending")
    void createRejectsNegativeOverdraftLimit() {
      Harness h = new Harness("{}");

      ApiResponse<JsonNode> response = h.blnk.transactions().create(
          CreateTransactions.create()
              .amount(50)
              .precision(100)
              .currency("USD")
              .reference("ref_neg_1")
              .description("Overdraft test")
              .source("bln_src")
              .destination("bln_dst")
              .allowOverdraft(true)
              .overdraftLimit(-1));

      assertEquals(400, response.status());
      assertEquals("overdraft_limit must be zero or more.", response.message());
      assertEquals(0, h.requestCount());
    }

    private CreateTransactions overdraftTxn(String reference, double limit) {
      return CreateTransactions.create()
          .amount(50)
          .precision(100)
          .currency("USD")
          .reference(reference)
          .description("Overdraft test")
          .source("bln_src")
          .destination("bln_dst")
          .allowOverdraft(true)
          .overdraftLimit(limit);
    }

    @Test
    @DisplayName("create rejects a NaN or infinite overdraft_limit without sending")
    void createRejectsNonFiniteOverdraftLimit() {
      Harness h = new Harness("{}");

      for (double bad : new double[] {
          Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
        ApiResponse<JsonNode> response =
            h.blnk.transactions().create(overdraftTxn("ref_bad_" + bad, bad));
        assertEquals(400, response.status(), String.valueOf(bad));
        assertEquals("overdraft_limit must be a number if provided.", response.message());
      }
      assertEquals(0, h.requestCount());
    }

    @Test
    @DisplayName("createBulk rejects a negative, NaN or infinite overdraft_limit on any item")
    void createBulkRejectsBadOverdraftLimit() {
      Harness h = new Harness("{}");
      Map<Double, String> cases = new java.util.LinkedHashMap<>();
      cases.put(-1.0, "overdraft_limit must be zero or more.");
      cases.put(Double.NaN, "overdraft_limit must be a number if provided.");
      cases.put(Double.POSITIVE_INFINITY, "overdraft_limit must be a number if provided.");

      cases.forEach((bad, message) -> {
        ApiResponse<JsonNode> response = h.blnk.transactions().createBulk(
            com.blnkfinance.blnk.types.BulkTransactions.create()
                .transactions(List.of(
                    overdraftTxn("ref_bulk_ok", 200),
                    overdraftTxn("ref_bulk_bad", bad))));
        assertEquals(400, response.status(), String.valueOf(bad));
        assertEquals("Transaction at index 1: " + message, response.message());
      });
      assertEquals(0, h.requestCount());
    }

    @Test
    @DisplayName("createBulk sends overdraft_limit on each item")
    void createBulkSendsOverdraftLimit() {
      Harness h = new Harness("{\"batch_id\":\"bulk_1\",\"status\":\"applied\"}");

      ApiResponse<JsonNode> response = h.blnk.transactions().createBulk(
          com.blnkfinance.blnk.types.BulkTransactions.create()
              .transactions(List.of(overdraftTxn("ref_bulk_a", 200), overdraftTxn("ref_bulk_b", 0))));

      assertEquals(200, response.status(), response.message());
      assertEquals(200.0, h.body().get("transactions").get(0).get("overdraft_limit").asDouble());
      assertEquals(0.0, h.body().get("transactions").get(1).get("overdraft_limit").asDouble());
    }

    @Test
    @DisplayName("updateStatus rejects a blank or whitespace-only reference on commit and void")
    void inflightRejectsBlankReference() {
      Harness h = new Harness("{}");

      for (String status : new String[] {"commit", "void"}) {
        for (String blank : new String[] {"", " ", "\t\n"}) {
          ApiResponse<JsonNode> response = h.blnk.transactions().updateStatus(
              "txn_1", UpdateTransactionStatus.create().status(status).reference(blank));
          assertEquals(400, response.status(), status + " [" + blank + "]");
          assertEquals("reference must be a non-empty string if provided.", response.message());
        }
      }
      assertEquals(0, h.requestCount());
    }

    @Test
    @DisplayName("updateStatus sends reference on void")
    void inflightVoidSendsReference() {
      Harness h = new Harness("{\"status\":\"VOID\"}");

      h.blnk.transactions().updateStatus(
          "txn_1", UpdateTransactionStatus.create().status("void").reference("void-txn_1-1"));

      assertEquals(json("{\"status\":\"void\",\"reference\":\"void-txn_1-1\"}"), h.body());
    }
  }

  @Nested
  @DisplayName("get balance include")
  class BalanceIncludeTests {

    @Test
    @DisplayName("get sends one include per value and returns the expanded objects")
    void sendsIncludes() {
      Harness h = new Harness("""
          {"balance_id":"bln_1",
           "identity":{"identity_id":"idt_1","first_name":"Ada"},
           "ledger":{"ledger_id":"ldg_1","name":"Wallets"}}""");

      ApiResponse<JsonNode> response = h.blnk.ledgerBalances().get(
          "bln_1",
          GetBalanceRequest.create()
              .include(GetBalanceRequest.INCLUDE_IDENTITY, GetBalanceRequest.INCLUDE_LEDGER));

      assertEquals(BASE + "balances/bln_1?include=identity&include=ledger", h.url());
      assertEquals("GET", h.method());
      assertNull(h.body());
      assertEquals("idt_1", response.data().get("identity").get("identity_id").asText());
      assertEquals("ldg_1", response.data().get("ledger").get("ledger_id").asText());
    }

    @Test
    @DisplayName("include combines with with_queued")
    void combinesWithQueued() {
      Harness h = new Harness("{}");

      h.blnk.ledgerBalances().get(
          "bln_1",
          GetBalanceRequest.create().withQueued(true).include(GetBalanceRequest.INCLUDE_LEDGER));

      assertEquals(BASE + "balances/bln_1?with_queued=true&include=ledger", h.url());
    }

    @Test
    @DisplayName("get rejects an unknown include value without sending")
    void rejectsUnknownInclude() {
      Harness h = new Harness("{}");

      ApiResponse<JsonNode> response = h.blnk.ledgerBalances().get(
          "bln_1", GetBalanceRequest.create().include("ledgers"));

      assertEquals(400, response.status());
      assertEquals("include values must be \"identity\" or \"ledger\"", response.message());
      assertEquals(0, h.requestCount());
    }
  }

  @Nested
  @DisplayName("identity pagination")
  class IdentityPaginationTests {

    @Test
    @DisplayName("identity().list() takes no ListOptions, because GET /identities ignores them")
    void listHasNoListOptionsOverload() {
      assertThrows(
          NoSuchMethodException.class,
          () -> com.blnkfinance.blnk.endpoints.Identity.class.getMethod("list", ListOptions.class));

      Harness h = new Harness("[]");
      h.blnk.identity().list();
      assertEquals(BASE + "identities", h.url());
    }

    @Test
    @DisplayName("search().filter pages identities through POST /identities/filter")
    void filterRoutePagesIdentities() {
      Harness h = new Harness("[{\"identity_id\":\"idt_3\"}]");

      ApiResponse<JsonNode> response = h.blnk.search().filter(
          FilterParams.create().filters(List.of()).limit(2).offset(2), "identities");

      assertEquals(BASE + "identities/filter", h.url());
      assertEquals("POST", h.method());
      assertEquals(json("{\"filters\":[],\"limit\":2,\"offset\":2}"), h.body());
      assertEquals("idt_3", response.data().get(0).get("identity_id").asText());
    }
  }

  @Test
  @DisplayName("metadata update returns Core's meta_data object")
  void metadataUpdateReadsMetaData() {
    Harness h = new Harness("{\"meta_data\":{\"tier\":\"gold\",\"kyc\":true}}");

    ApiResponse<JsonNode> response = h.blnk.metadata().update(
        "bln_1", UpdateMetadataData.create().metaData(Map.of("tier", "gold")));

    assertEquals(BASE + "bln_1/metadata", h.url());
    assertEquals(json("{\"meta_data\":{\"tier\":\"gold\"}}"), h.body());
    assertEquals("gold", response.data().get("meta_data").get("tier").asText());
    assertTrue(response.data().get("meta_data").get("kyc").asBoolean());
  }
}
