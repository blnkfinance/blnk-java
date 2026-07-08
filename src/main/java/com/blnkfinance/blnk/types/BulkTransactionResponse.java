package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Parse-tolerant view of the {@code POST transactions/bulk} response:
 * {@code batch_id}, {@code status}, and optionally {@code transaction_count}
 * or {@code message}.
 *
 * <p>Optional convenience wrapper — endpoint methods still return
 * {@code ApiResponse<JsonNode>}. Absent fields read as {@code null}:
 * {@code transaction_count} is present on synchronous successes only;
 * {@code message} only when {@code run_async} is true.
 */
public final class BulkTransactionResponse {

  private final ObjectNode json;

  private BulkTransactionResponse(ObjectNode json) {
    this.json = json;
  }

  public static BulkTransactionResponse fromJson(JsonNode json) {
    return new BulkTransactionResponse(
        json instanceof ObjectNode objectNode ? objectNode.deepCopy() : BlnkJson.objectNode());
  }

  public ObjectNode toJson() {
    return json.deepCopy();
  }

  public String batchId() {
    JsonNode node = json.get("batch_id");
    return node != null && node.isTextual() ? node.asText() : null;
  }

  public String status() {
    JsonNode node = json.get("status");
    return node != null && node.isTextual() ? node.asText() : null;
  }

  /** Present when processing finished synchronously; null otherwise. */
  public Integer transactionCount() {
    JsonNode node = json.get("transaction_count");
    return node != null && node.isNumber() ? node.asInt() : null;
  }

  /** Present when {@code run_async} is true; null otherwise. */
  public String message() {
    JsonNode node = json.get("message");
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
