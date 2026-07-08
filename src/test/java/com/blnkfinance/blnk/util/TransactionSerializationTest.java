package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.types.CreateTransactions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TransactionSerialization}: date-field serialization to
 * the datetime formats Blnk Core accepts.
 */
@DisplayName("Transaction serialization")
class TransactionSerializationTest {

  @Test
  @DisplayName("serializes Date fields to ISO strings")
  void serializesDateFieldsToIso8601Strings() {
    Date effectiveDate = Date.from(Instant.parse("2025-02-15T10:30:00.000Z"));
    CreateTransactions data =
        CreateTransactions.create()
            .amount(1000)
            .precision(100)
            .reference("ref_001")
            .description("Backdated transaction")
            .currency("USD")
            .source("@FundingPool")
            .destination("@Recipient")
            .effectiveDate(effectiveDate)
            .inflightCommitDate(Date.from(Instant.parse("2025-06-01T12:00:00.000Z")))
            .scheduledFor(Date.from(Instant.parse("2025-07-01T08:00:00.000Z")))
            .inflightExpiryDate(Date.from(Instant.parse("2025-08-01T08:00:00.000Z")));

    ObjectNode serialized = TransactionSerialization.serializeCreateTransaction(data.toJson());

    assertEquals("2025-02-15T10:30:00Z", serialized.get("effective_date").asText());
    assertEquals("2025-06-01T12:00:00Z", serialized.get("inflight_commit_date").asText());
    assertEquals("2025-07-01T08:00:00Z", serialized.get("scheduled_for").asText());
    assertEquals("2025-08-01T08:00:00Z", serialized.get("inflight_expiry_date").asText());
  }

  @Test
  @DisplayName("passes through ISO date strings unchanged")
  void passesThroughIsoDateStringsUnchanged() {
    CreateTransactions data =
        CreateTransactions.create()
            .amount(1000)
            .precision(100)
            .reference("ref_002")
            .description("Backdated transaction")
            .currency("USD")
            .source("@FundingPool")
            .destination("@Recipient")
            .effectiveDate("2025-02-15T10:30:00Z")
            .inflightCommitDate("2025-06-01T12:00:00Z");

    ObjectNode serialized = TransactionSerialization.serializeCreateTransaction(data.toJson());

    assertEquals("2025-02-15T10:30:00Z", serialized.get("effective_date").asText());
    assertEquals("2025-06-01T12:00:00Z", serialized.get("inflight_commit_date").asText());
  }

  @Test
  @DisplayName("serializeTransactionDate returns undefined for undefined input")
  void serializeTransactionDateReturnsUndefinedForUndefinedInput() {
    // A date that was never provided (null) stays null — no field is emitted.
    assertNull(TransactionSerialization.serializeTransactionDate(null));
  }

  @Test
  @DisplayName("accepts Core example datetime formats")
  void acceptsCoreExampleDatetimeFormats() {
    assertTrue(
        TransactionSerialization.isValidTransactionDateInput("2024-04-22T15:28:03+00:00"),
        "Core model_test inflight_commit_date example");
    assertTrue(
        TransactionSerialization.isValidTransactionDateInput("2024-04-22T15:28:03+0000"),
        "Core time.Parse offset without colon");
    assertFalse(
        TransactionSerialization.isValidTransactionDateInput("2024-04-22T15:28:03.000Z"),
        "fractional seconds rejected for string date fields");
  }
}
