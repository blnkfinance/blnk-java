package com.blnkfinance.blnk.integration;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.testsupport.TestUtils;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.MultiSearchParams;
import com.blnkfinance.blnk.types.SearchParams;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live coverage of {@code search().multiSearch} against Core's {@code POST
 * /multi-search} (in Core since v0.10.0; this SDK is aligned with 0.15.4).
 * Gated on {@code BLNK_E2E=1}.
 */
@EnabledIfEnvironmentVariable(named = "BLNK_E2E", matches = "1")
@DisplayName("multiSearch — live mixed collections")
class MultiSearchIntegrationTest {

  private static final Blnk client =
      Blnk.init(
          TestUtils.BLNK_API_KEY,
          BlnkClientOptions.builder().baseUrl(TestUtils.BASE_URL).build());

  @Test
  @DisplayName("multiSearch ledgers then balances; first result bucket matches first search")
  void multiSearchMixedCollections() {
    String ledgerName = "Java MultiSearch " + UUID.randomUUID();
    ApiResponse<JsonNode> created = client.ledgers().create(CreateLedger.create().name(ledgerName));
    assertEquals(201, created.status(), created.message());

    long deadline = System.currentTimeMillis() + 30_000;
    ApiResponse<JsonNode> response = null;
    while (true) {
      response =
          client
              .search()
              .multiSearch(
                  MultiSearchParams.create()
                      .add("ledgers", SearchParams.create().q(ledgerName).queryBy("name").perPage(5))
                      .add(
                          "balances",
                          SearchParams.create().q("*").queryBy("currency").perPage(1)));
      if (response.status() == 200
          && response.data() != null
          && response.data().path("results").isArray()
          && response.data().get("results").size() == 2
          && response.data().get("results").get(0).path("found").asInt(0) > 0) {
        break;
      }
      if (System.currentTimeMillis() > deadline) {
        assertEquals(200, response.status(), response.message());
        assertNotNull(response.data());
        assertTrue(response.data().path("results").isArray(), response.data().toString());
        assertEquals(2, response.data().get("results").size(), response.data().toString());
        assertTrue(
            response.data().get("results").get(0).path("found").asInt(0) > 0,
            "expected indexed ledger in first results bucket: " + response.data());
        break;
      }
      TestUtils.sleepSeconds(0.5);
    }

    assertEquals(200, response.status(), response.message());
    JsonNode results = response.data().get("results");
    assertEquals(2, results.size());
    assertTrue(results.get(0).path("found").asInt(0) > 0);
    assertTrue(results.get(1).path("found").asInt(-1) >= 0);
  }
}
