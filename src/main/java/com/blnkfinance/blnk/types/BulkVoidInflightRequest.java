package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST transactions/inflight/bulk/void}: an optional
 * {@code skip_queue} flag plus the list of {@code transaction_ids}.
 *
 * <p>The body is forwarded to the wire exactly as built; no field is rewritten
 * before the request.
 */
public final class BulkVoidInflightRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private BulkVoidInflightRequest() {}

  public static BulkVoidInflightRequest create() {
    return new BulkVoidInflightRequest();
  }

  public BulkVoidInflightRequest skipQueue(boolean skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public BulkVoidInflightRequest skipQueue(Object skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Preview the bulk void without applying it. */
  public BulkVoidInflightRequest dryRun(boolean dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public BulkVoidInflightRequest dryRun(Object dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  public BulkVoidInflightRequest transactionIds(List<String> transactionIds) {
    fields.put("transaction_ids", new ArrayList<>(transactionIds));
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public BulkVoidInflightRequest putAdditional(String key, Object value) {
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
