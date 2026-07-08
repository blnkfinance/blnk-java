package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional body for {@code POST refund-transaction/{transaction_id}}: an
 * optional {@code skip_queue} flag.
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
