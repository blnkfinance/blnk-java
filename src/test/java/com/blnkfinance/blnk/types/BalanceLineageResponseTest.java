package com.blnkfinance.blnk.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BalanceLineageResponse}: the DTO must accept both the
 * string and numeric minor-unit amount representations returned by Blnk Core.
 */
@DisplayName("BalanceLineageResponse API shape")
class BalanceLineageResponseTest {

  private static final BalanceLineageResponse referenceResponse =
      new BalanceLineageResponse(
          "bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f",
          "bln_aggregate_shadow_balance_id",
          "7500",
          List.of(
              new LineageProviderBreakdown(
                  "stripe", "10000", "7500", "2500", "bln_shadow_balance_id")));

  @Test
  @DisplayName("accepts Core API reference response")
  void acceptsCoreApiReferenceResponse() {
    assertEquals("bln_5ce86029-3c2e-4e2a-aae2-7fb931ca4c4f", referenceResponse.balanceId());
    assertEquals(1, referenceResponse.providers().size());
    assertEquals("stripe", referenceResponse.providers().get(0).provider());
  }

  @Test
  @DisplayName("accepts numeric minor-unit amounts")
  void acceptsNumericMinorUnitAmounts() {
    BalanceLineageResponse numericResponse =
        new BalanceLineageResponse(
            referenceResponse.balanceId(),
            referenceResponse.aggregateBalanceId(),
            7500,
            List.of(
                new LineageProviderBreakdown(
                    referenceResponse.providers().get(0).provider(),
                    10000,
                    7500,
                    2500,
                    referenceResponse.providers().get(0).shadowBalanceId())));

    assertTrue(numericResponse.totalWithLineage() instanceof Number);
  }
}
