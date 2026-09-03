package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional body for {@code POST refund-transaction/{transaction_id}}:
 * {@code skip_queue}, {@code dry_run}, {@code description}, and
 * {@code meta_data}.
 */
public final class RefundTransactionRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private RefundTransactionRequest() {}

  public static RefundTransactionRequest create() {
    return new RefundTransactionRequest();
  }

  public RefundTransactionRequest skipQueue(boolean skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public RefundTransactionRequest skipQueue(Object skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Preview the refund without writing it. */
  public RefundTransactionRequest dryRun(boolean dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public RefundTransactionRequest dryRun(Object dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /**
   * Narration for the refund. When omitted or empty, Core copies the original
   * transaction's description.
   */
  public RefundTransactionRequest description(String description) {
    fields.put("description", description);
    return this;
  }

  /**
   * Merged onto metadata inherited from the original. Sent keys replace
   * matching keys; other keys stay. An empty object is ignored. The original
   * transaction is not modified.
   */
  public RefundTransactionRequest metaData(Map<String, Object> metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-object input reach the validator. */
  public RefundTransactionRequest metaData(Object metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public RefundTransactionRequest putAdditional(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
