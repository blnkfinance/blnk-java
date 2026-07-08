package com.blnkfinance.blnk.types;

import com.blnkfinance.blnk.testsupport.CoreBulkTransactionResponse;
import com.blnkfinance.blnk.util.ValueFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BulkTransactionResponse}: parses the reference sync,
 * async, and inflight bulk responses returned by Blnk Core and exposes their
 * fields.
 */
@DisplayName("BulkTransactionResponse API shape")
class BulkTransactionResponseTest {

  @Test
  @DisplayName("accepts Core API reference sync bulk response")
  void acceptsCoreApiReferenceSyncBulkResponse() {
    BulkTransactionResponse response =
        BulkTransactionResponse.fromJson(
            CoreBulkTransactionResponse.coreBulkTransactionReferenceResponse());

    assertEquals("bulk_c62f200b-905f-4983-a349-cadd279234aa", response.batchId());
    assertEquals("applied", response.status());
    assertEquals(Integer.valueOf(4), response.transactionCount());
  }

  @Test
  @DisplayName("accepts Core API reference async bulk response")
  void acceptsCoreApiReferenceAsyncBulkResponse() {
    BulkTransactionResponse response =
        BulkTransactionResponse.fromJson(
            CoreBulkTransactionResponse.coreBulkTransactionAsyncReferenceResponse());

    assertEquals("bulk_c62f200b-905f-4983-a349-cadd279234aa", response.batchId());
    assertEquals("queued", response.status());
    assertEquals("Bulk transaction processing started", response.message());
  }

  @Test
  @DisplayName("batch_id field is present on reference response")
  void batchIdFieldIsPresentOnReferenceResponse() {
    BulkTransactionResponse response =
        BulkTransactionResponse.fromJson(
            CoreBulkTransactionResponse.coreBulkTransactionReferenceResponse());

    assertTrue(ValueFormat.isTruthy(response.batchId()));
  }

  @Test
  @DisplayName("status field is present on reference response")
  void statusFieldIsPresentOnReferenceResponse() {
    BulkTransactionResponse response =
        BulkTransactionResponse.fromJson(
            CoreBulkTransactionResponse.coreBulkTransactionReferenceResponse());

    assertInstanceOf(String.class, response.status());
  }

  @Test
  @DisplayName("transaction_count is optional on async response")
  void transactionCountIsOptionalOnAsyncResponse() {
    BulkTransactionResponse response =
        BulkTransactionResponse.fromJson(
            CoreBulkTransactionResponse.coreBulkTransactionAsyncReferenceResponse());

    // An absent transaction_count key reads as null.
    assertNull(response.transactionCount());
  }

  @Test
  @DisplayName("accepts inflight bulk status")
  void acceptsInflightBulkStatus() {
    ObjectNode inflightJson = BlnkJson.objectNode();
    inflightJson.put("batch_id", "bulk_4192d961-5b0e-46ca-bf2f-9386763057f8");
    inflightJson.put("status", "inflight");
    inflightJson.put("transaction_count", 2);
    BulkTransactionResponse inflightResponse = BulkTransactionResponse.fromJson(inflightJson);

    assertEquals("inflight", inflightResponse.status());
    assertEquals(Integer.valueOf(2), inflightResponse.transactionCount());
  }
}
