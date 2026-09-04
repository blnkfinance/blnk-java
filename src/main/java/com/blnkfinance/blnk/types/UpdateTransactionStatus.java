package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code PUT transactions/inflight/{id}}: a required
 * {@code status} plus optional {@code amount}, {@code precise_amount},
 * {@code meta_data} and {@code skip_queue}.
 *
 * <p>The body is forwarded to the wire exactly as built; no field is rewritten
 * before the request.
 */
public final class UpdateTransactionStatus {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private UpdateTransactionStatus() {}

  public static UpdateTransactionStatus create() {
    return new UpdateTransactionStatus();
  }

  public UpdateTransactionStatus status(String status) {
    fields.put("status", status);
    return this;
  }

  public UpdateTransactionStatus amount(double amount) {
    fields.put("amount", amount);
    return this;
  }

  public UpdateTransactionStatus preciseAmount(double preciseAmount) {
    fields.put("precise_amount", preciseAmount);
    return this;
  }

  public UpdateTransactionStatus preciseAmount(String preciseAmount) {
    fields.put("precise_amount", preciseAmount);
    return this;
  }

  public UpdateTransactionStatus metaData(Map<String, Object> metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-object input reach the validator. */
  public UpdateTransactionStatus metaData(Object metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  public UpdateTransactionStatus skipQueue(boolean skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Preview the inflight commit or void without applying it. */
  public UpdateTransactionStatus dryRun(boolean dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public UpdateTransactionStatus dryRun(Object dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public UpdateTransactionStatus skipQueue(Object skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public UpdateTransactionStatus putAdditional(String key, Object value) {
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
