package com.blnkfinance.blnk.types;

import com.blnkfinance.blnk.testsupport.CoreCreateTransactionResponse;
import com.blnkfinance.blnk.util.ValueFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CreateTransactionResponse}: parses the reference
 * create-transaction response returned by Blnk Core. The reference fixture
 * carries no {@code rate} or {@code effective_date}, and its nano-precision
 * {@code created_at} string is preserved verbatim.
 */
@DisplayName("CreateTransactionResponse API shape")
class CreateTransactionResponseTest {

  @Test
  @DisplayName("accepts Core 0.15.0 create response without rate")
  void acceptsCore0150CreateResponseWithoutRate() {
    CreateTransactionResponse response =
        CreateTransactionResponse.fromJson(
            CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse());

    // An absent rate key reads as null.
    assertNull(response.rate());
  }

  @Test
  @DisplayName("accepts Core API reference create response")
  void acceptsCoreApiReferenceCreateResponse() {
    CreateTransactionResponse response =
        CreateTransactionResponse.fromJson(
            CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse());

    assertEquals(64, response.hash().length());
    assertEquals("", response.parentTransaction());
    assertEquals(false, response.allowOverdraft());
    assertEquals(false, response.inflight());
    assertEquals("0001-01-01T00:00:00Z", response.scheduledFor());
    assertEquals("0001-01-01T00:00:00Z", response.inflightExpiryDate());
    assertEquals("0001-01-01T00:00:00Z", response.inflightCommitDate());
  }

  @Test
  @DisplayName("hash field is present on reference response")
  void hashFieldIsPresentOnReferenceResponse() {
    CreateTransactionResponse response =
        CreateTransactionResponse.fromJson(
            CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse());

    assertTrue(ValueFormat.isTruthy(response.hash()));
  }

  @Test
  @DisplayName("parent_transaction field is present on reference response")
  void parentTransactionFieldIsPresentOnReferenceResponse() {
    CreateTransactionResponse response =
        CreateTransactionResponse.fromJson(
            CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse());

    assertInstanceOf(String.class, response.parentTransaction());
  }

  @Test
  @DisplayName("allow_overdraft field is present on reference response")
  void allowOverdraftFieldIsPresentOnReferenceResponse() {
    CreateTransactionResponse response =
        CreateTransactionResponse.fromJson(
            CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse());

    assertInstanceOf(Boolean.class, response.allowOverdraft());
  }

  @Test
  @DisplayName("inflight date fields use ISO strings on reference response")
  void inflightDateFieldsUseIsoStringsOnReferenceResponse() {
    CreateTransactionResponse response =
        CreateTransactionResponse.fromJson(
            CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse());

    assertEquals("0001-01-01T00:00:00Z", response.inflightExpiryDate());
    assertEquals("0001-01-01T00:00:00Z", response.inflightCommitDate());
    assertEquals("0001-01-01T00:00:00Z", response.scheduledFor());
  }

  @Test
  @DisplayName("accepts inflight create response with custom dates")
  void acceptsInflightCreateResponseWithCustomDates() {
    ObjectNode inflightJson =
        CoreCreateTransactionResponse.coreCreateTransactionReferenceResponse();
    inflightJson.put("status", "INFLIGHT");
    inflightJson.put("inflight", true);
    inflightJson.put("inflight_expiry_date", "2026-12-31T23:59:59Z");
    inflightJson.put("inflight_commit_date", "2024-04-22T15:28:03+00:00");
    inflightJson.put("scheduled_for", "2025-12-31T23:59:59Z");
    inflightJson.put("effective_date", "2025-02-15T10:30:00Z");
    inflightJson.put("allow_overdraft", true);
    CreateTransactionResponse inflightResponse = CreateTransactionResponse.fromJson(inflightJson);

    assertEquals("2026-12-31T23:59:59Z", inflightResponse.inflightExpiryDate());
    // The numeric-offset form is preserved exactly as sent, not normalized to "Z".
    assertEquals("2024-04-22T15:28:03+00:00", inflightResponse.inflightCommitDate());
    assertEquals(true, inflightResponse.allowOverdraft());
  }
}
