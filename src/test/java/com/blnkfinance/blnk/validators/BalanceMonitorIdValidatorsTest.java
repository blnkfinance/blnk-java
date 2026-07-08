package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for balance-monitor id validation in {@link BalanceMonitorValidators}. */
class BalanceMonitorIdValidatorsTest {

  @Test
  @DisplayName("ValidateMonitorId")
  void validateMonitorId() {
    assertNull(BalanceMonitorValidators.validateMonitorId("mon_test_123"));
    assertEquals("monitor id is required", BalanceMonitorValidators.validateMonitorId(""));
  }
}
