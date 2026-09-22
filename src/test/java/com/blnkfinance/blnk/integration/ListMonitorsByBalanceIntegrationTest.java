package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.MonitorCondition;
import com.blnkfinance.blnk.types.MonitorData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live coverage of {@code balanceMonitor().listByBalanceId} against Core's
 * {@code GET /balance-monitors/balances/:balance_id} (in Core since ~0.14.x;
 * this SDK is aligned with 0.15.4). Gated on {@code BLNK_E2E=1}. When Core
 * is down these tests are skipped, not marked passed.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("listByBalanceId — live")
class ListMonitorsByBalanceIntegrationTest {

  private static final Blnk client =
      Blnk.init(
          TestUtils.BLNK_API_KEY,
          BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  @Test
  @DisplayName("create a monitor on a balance then GET by that balance id")
  void listByBalanceIdFindsCreatedMonitor() {
    ApiResponse<JsonNode> ledger =
        client.ledgers().create(CreateLedger.create().name("Java List Monitors " + UUID.randomUUID()));
    assertEquals(201, ledger.status(), ledger.message());
    String ledgerId = ledger.data().get("ledger_id").asText();

    ApiResponse<JsonNode> balance =
        client
            .ledgerBalances()
            .create(CreateLedgerBalance.create().ledgerId(ledgerId).currency("USD"));
    assertEquals(201, balance.status(), balance.message());
    String balanceId = balance.data().get("balance_id").asText();

    ApiResponse<JsonNode> monitor =
        client
            .balanceMonitor()
            .create(
                MonitorData.create()
                    .balanceId(balanceId)
                    .condition(
                        MonitorCondition.create()
                            .field("credit_balance")
                            .operator(">=")
                            .precision(100)
                            .value(1))
                    .description("list-by-balance coverage"));
    assertEquals(201, monitor.status(), monitor.message());
    String monitorId = monitor.data().get("monitor_id").asText();

    ApiResponse<JsonNode> byBalance = client.balanceMonitor().listByBalanceId(balanceId);
    assertEquals(200, byBalance.status(), byBalance.message());
    assertNotNull(byBalance.data());
    assertTrue(byBalance.data().isArray(), byBalance.data().toString());
    boolean found = false;
    for (JsonNode row : byBalance.data()) {
      if (monitorId.equals(row.path("monitor_id").asText())) {
        found = true;
        break;
      }
    }
    assertTrue(found, "created monitor " + monitorId + " missing from listByBalanceId");
  }
}
