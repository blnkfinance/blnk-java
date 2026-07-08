package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.types.BlnkJson;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Sample {@code POST /transactions/bulk} responses, with values taken
 * verbatim from the Blnk Core API reference.
 */
public final class CoreBulkTransactionResponse {

  private CoreBulkTransactionResponse() {}

  /** Sync bulk response. Fresh node per call so tests can override fields safely. */
  public static ObjectNode coreBulkTransactionReferenceResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("batch_id", "bulk_c62f200b-905f-4983-a349-cadd279234aa");
    node.put("status", "applied");
    node.put("transaction_count", 4);
    return node;
  }

  /** Async bulk response when {@code run_async} is true. */
  public static ObjectNode coreBulkTransactionAsyncReferenceResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("batch_id", "bulk_c62f200b-905f-4983-a349-cadd279234aa");
    node.put("status", "queued");
    node.put("message", "Bulk transaction processing started");
    return node;
  }
}
