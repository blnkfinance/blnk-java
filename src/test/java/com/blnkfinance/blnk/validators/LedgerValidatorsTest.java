package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for ledger create and update payload validation in {@link LedgerValidators}. */
class LedgerValidatorsTest {

  @Nested
  @DisplayName("ValidateUpdateLedger")
  class ValidateUpdateLedger {

    @Test
    @DisplayName("accepts a valid name")
    void acceptsAValidName() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("name", "Updated Customer Savings Account");
      assertNull(LedgerValidators.validateUpdateLedger(data));
    }

    @Test
    @DisplayName("rejects missing name")
    void rejectsMissingName() {
      assertEquals(
          "name field must be a valid string",
          LedgerValidators.validateUpdateLedger(new LinkedHashMap<>()));
    }

    @Test
    @DisplayName("rejects empty name")
    void rejectsEmptyName() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("name", "");
      assertEquals("name field is required", LedgerValidators.validateUpdateLedger(data));
    }

    @Test
    @DisplayName("rejects whitespace-only name")
    void rejectsWhitespaceOnlyName() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("name", "   ");
      assertEquals("name field is required", LedgerValidators.validateUpdateLedger(data));
    }
  }

  @Test
  @DisplayName("ValidateCreateLedger still accepts valid create payloads")
  void validateCreateLedgerStillAcceptsValidCreatePayloads() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("name", "My Ledger");
    assertNull(LedgerValidators.validateCreateLedger(data));
  }
}
