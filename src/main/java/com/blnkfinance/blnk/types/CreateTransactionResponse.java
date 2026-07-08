package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;

/**
 * Parse-tolerant view of the {@code POST transactions} response.
 *
 * <p>Optional convenience wrapper — endpoint methods still return
 * {@code ApiResponse<JsonNode>}; this class only eases field access. Absent
 * fields read as {@code null} — e.g. {@code rate} was removed from Core
 * 0.15.0 responses. Date fields may arrive as strings or as raw date objects
 * and are returned as-is.
 */
public final class CreateTransactionResponse {

  private final ObjectNode json;

  private CreateTransactionResponse(ObjectNode json) {
    this.json = json;
  }

  public static CreateTransactionResponse fromJson(JsonNode json) {
    return new CreateTransactionResponse(
        json instanceof ObjectNode objectNode ? objectNode.deepCopy() : BlnkJson.objectNode());
  }

  public ObjectNode toJson() {
    return json.deepCopy();
  }

  public String transactionId() {
    return text("transaction_id");
  }

  public Double amount() {
    return number("amount");
  }

  public Double precision() {
    return number("precision");
  }

  /** May arrive as a number or a string — returns a Double, a String, or null when absent. */
  public Object preciseAmount() {
    JsonNode node = json.get("precise_amount");
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isNumber()) {
      return node.doubleValue();
    }
    return node;
  }

  public String reference() {
    return text("reference");
  }

  public String description() {
    return text("description");
  }

  /** Removed from Core 0.15.0 responses — null when absent. */
  public Double rate() {
    return number("rate");
  }

  public String currency() {
    return text("currency");
  }

  public String status() {
    return text("status");
  }

  /** SHA-256 hash of the transaction details. */
  public String hash() {
    return text("hash");
  }

  /** Parent transaction ID, or empty string when none. */
  public String parentTransaction() {
    return text("parent_transaction");
  }

  public String source() {
    return text("source");
  }

  public String destination() {
    return text("destination");
  }

  public JsonNode sources() {
    return json.get("sources");
  }

  public JsonNode destinations() {
    return json.get("destinations");
  }

  public Boolean allowOverdraft() {
    return bool("allow_overdraft");
  }

  public Boolean skipQueue() {
    return bool("skip_queue");
  }

  public Boolean inflight() {
    return bool("inflight");
  }

  public Boolean queued() {
    return bool("queued");
  }

  public Boolean atomic() {
    return bool("atomic");
  }

  public Double overdraftLimit() {
    return number("overdraft_limit");
  }

  public Object createdAt() {
    return dateOrString("created_at");
  }

  public Object scheduledFor() {
    return dateOrString("scheduled_for");
  }

  public Object inflightExpiryDate() {
    return dateOrString("inflight_expiry_date");
  }

  public Object inflightCommitDate() {
    return dateOrString("inflight_commit_date");
  }

  public Object effectiveDate() {
    return dateOrString("effective_date");
  }

  public JsonNode metaData() {
    return json.get("meta_data");
  }

  private String text(String field) {
    JsonNode node = json.get(field);
    return node != null && node.isTextual() ? node.asText() : null;
  }

  private Double number(String field) {
    JsonNode node = json.get(field);
    return node != null && node.isNumber() ? node.doubleValue() : null;
  }

  private Boolean bool(String field) {
    JsonNode node = json.get(field);
    return node != null && node.isBoolean() ? node.booleanValue() : null;
  }

  private Object dateOrString(String field) {
    JsonNode node = json.get(field);
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node instanceof POJONode pojoNode) {
      return pojoNode.getPojo(); // raw date-object payloads are returned unwrapped
    }
    return node;
  }
}
