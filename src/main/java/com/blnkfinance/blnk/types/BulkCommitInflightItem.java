package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One transaction in a {@code POST transactions/inflight/bulk/commit} request:
 * a required {@code transaction_id} plus optional {@code amount} or
 * {@code precise_amount}.
 */
public final class BulkCommitInflightItem {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private BulkCommitInflightItem() {}

  public static BulkCommitInflightItem create() {
    return new BulkCommitInflightItem();
  }

  public BulkCommitInflightItem transactionId(String transactionId) {
    fields.put("transaction_id", transactionId);
    return this;
  }

  public BulkCommitInflightItem amount(double amount) {
    fields.put("amount", amount);
    return this;
  }

  public BulkCommitInflightItem preciseAmount(double preciseAmount) {
    fields.put("precise_amount", preciseAmount);
    return this;
  }

  public BulkCommitInflightItem preciseAmount(String preciseAmount) {
    fields.put("precise_amount", preciseAmount);
    return this;
  }

  /**
   * Sets an arbitrary extra field. Unknown per-item keys pass validation and
   * are sent to the API unchanged.
   */
  public BulkCommitInflightItem putAdditional(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
