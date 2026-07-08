package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link LedgerBalanceValidators}: indicator lookups, identity
 * updates, snapshots, balance creation, and point-in-time balance options.
 */
class LedgerBalanceValidatorsTest {

  @Nested
  @DisplayName("ValidateGetByIndicator")
  class ValidateGetByIndicator {

    @Test
    @DisplayName("accepts valid indicator and currency")
    void acceptsValidIndicatorAndCurrency() {
      assertNull(LedgerBalanceValidators.validateGetByIndicator("@World", "USD"));
    }

    @Test
    @DisplayName("rejects empty indicator")
    void rejectsEmptyIndicator() {
      assertEquals(
          "indicator is required", LedgerBalanceValidators.validateGetByIndicator("", "USD"));
    }

    @Test
    @DisplayName("rejects empty currency")
    void rejectsEmptyCurrency() {
      assertEquals(
          "currency is required", LedgerBalanceValidators.validateGetByIndicator("@World", ""));
    }
  }

  @Nested
  @DisplayName("ValidateUpdateBalanceIdentity")
  class ValidateUpdateBalanceIdentity {

    @Test
    @DisplayName("accepts valid identity_id")
    void acceptsValidIdentityId() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("identity_id", "idt_3b63c8da-af29-4cc3-ad38-df17d87456e6");
      assertNull(LedgerBalanceValidators.validateUpdateBalanceIdentity(data));
    }

    @Test
    @DisplayName("rejects missing identity_id")
    void rejectsMissingIdentityId() {
      assertEquals(
          "identity_id is required",
          LedgerBalanceValidators.validateUpdateBalanceIdentity(new LinkedHashMap<>()));
    }

    @Test
    @DisplayName("rejects empty identity_id")
    void rejectsEmptyIdentityId() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("identity_id", "");
      assertEquals(
          "identity_id is required",
          LedgerBalanceValidators.validateUpdateBalanceIdentity(data));
    }
  }

  @Nested
  @DisplayName("ValidateCreateBalanceSnapshot")
  class ValidateCreateBalanceSnapshot {

    @Test
    @DisplayName("accepts empty options")
    void acceptsEmptyOptions() {
      // Omitted options (null) are valid — the snapshot call has no required fields.
      assertNull(LedgerBalanceValidators.validateCreateBalanceSnapshot(null));
    }

    @Test
    @DisplayName("accepts positive batch_size")
    void acceptsPositiveBatchSize() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("batch_size", 500);
      assertNull(LedgerBalanceValidators.validateCreateBalanceSnapshot(data));
    }

    @Test
    @DisplayName("accepts zero batch_size")
    void acceptsZeroBatchSize() {
      // Intentional: zero passes even though the error message says "positive";
      // only negative values are rejected.
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("batch_size", 0);
      assertNull(LedgerBalanceValidators.validateCreateBalanceSnapshot(data));
    }

    @Test
    @DisplayName("rejects negative batch_size")
    void rejectsNegativeBatchSize() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("batch_size", -1);
      assertEquals(
          "batch_size must be positive",
          LedgerBalanceValidators.validateCreateBalanceSnapshot(data));
    }
  }

  @Nested
  @DisplayName("ValidateCreateLedgerBalance lineage fields")
  class ValidateCreateLedgerBalanceLineageFields {

    @Test
    @DisplayName("accepts track_fund_lineage and allocation_strategy")
    void acceptsTrackFundLineageAndAllocationStrategy() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("ledger_id", "ldg_123");
      data.put("currency", "USD");
      data.put("identity_id", "idt_123");
      data.put("track_fund_lineage", true);
      data.put("allocation_strategy", "LIFO");
      assertNull(LedgerBalanceValidators.validateCreateLedgerBalance(data));
    }

    @Test
    @DisplayName("accepts request without lineage fields")
    void acceptsRequestWithoutLineageFields() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("ledger_id", "ldg_123");
      data.put("currency", "USD");
      assertNull(LedgerBalanceValidators.validateCreateLedgerBalance(data));
    }

    @Test
    @DisplayName("rejects non-boolean track_fund_lineage")
    void rejectsNonBooleanTrackFundLineage() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("ledger_id", "ldg_123");
      data.put("currency", "USD");
      data.put("track_fund_lineage", "true");
      assertEquals(
          "track_fund_lineage must be a boolean if provided",
          LedgerBalanceValidators.validateCreateLedgerBalance(data));
    }

    @Test
    @DisplayName("rejects invalid allocation_strategy")
    void rejectsInvalidAllocationStrategy() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("ledger_id", "ldg_123");
      data.put("currency", "USD");
      data.put("allocation_strategy", "INVALID");
      assertEquals(
          "allocation_strategy must be one of FIFO, LIFO, or PROPORTIONAL",
          LedgerBalanceValidators.validateCreateLedgerBalance(data));
    }
  }

  @Nested
  @DisplayName("ValidateGetBalance")
  class ValidateGetBalance {

    @Test
    @DisplayName("accepts from_source flag")
    void acceptsFromSourceFlag() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("from_source", true);
      assertNull(LedgerBalanceValidators.validateGetBalance(data));
    }

    @Test
    @DisplayName("accepts empty options object")
    void acceptsEmptyOptionsObject() {
      assertNull(LedgerBalanceValidators.validateGetBalance(new LinkedHashMap<>()));
    }

    @Test
    @DisplayName("rejects non-boolean from_source")
    void rejectsNonBooleanFromSource() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("from_source", "true");
      assertEquals(
          "from_source must be a boolean if provided",
          LedgerBalanceValidators.validateGetBalance(data));
    }
  }

  @Nested
  @DisplayName("ValidateGetBalanceAt")
  class ValidateGetBalanceAt {

    @Test
    @DisplayName("accepts valid timestamp")
    void acceptsValidTimestamp() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("timestamp", "2025-02-24T08:55:26Z");
      assertNull(LedgerBalanceValidators.validateGetBalanceAt(data));
    }

    @Test
    @DisplayName("accepts from_source flag")
    void acceptsFromSourceFlag() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("timestamp", "2025-02-24T08:55:26Z");
      data.put("from_source", true);
      assertNull(LedgerBalanceValidators.validateGetBalanceAt(data));
    }

    @Test
    @DisplayName("rejects empty timestamp")
    void rejectsEmptyTimestamp() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("timestamp", "");
      assertEquals(
          "timestamp is required", LedgerBalanceValidators.validateGetBalanceAt(data));
    }
  }
}
