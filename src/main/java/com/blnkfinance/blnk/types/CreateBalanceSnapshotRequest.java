package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional options for {@code POST balances-snapshots}. {@code batch_size}
 * travels ONLY in the query string, never in a request body.
 */
public final class CreateBalanceSnapshotRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private CreateBalanceSnapshotRequest() {}

  public static CreateBalanceSnapshotRequest create() {
    return new CreateBalanceSnapshotRequest();
  }

  /**
   * Balances processed per batch. Omit or zero uses the server default (1000).
   * Typed {@link Number} so any numeric value can be supplied.
   */
  public CreateBalanceSnapshotRequest batchSize(Number batchSize) {
    fields.put("batch_size", batchSize);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** JSON view (unused on the wire — batch_size travels in the query string only). */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
