package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST transactions/inflight/bulk/commit}: an optional
 * {@code skip_queue} flag plus the list of {@code transactions}.
 *
 * <p>The body is forwarded to the wire exactly as built; no field is rewritten
 * before the request.
 */
public final class BulkCommitInflightRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private BulkCommitInflightRequest() {}

  public static BulkCommitInflightRequest create() {
    return new BulkCommitInflightRequest();
  }

  public BulkCommitInflightRequest skipQueue(boolean skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public BulkCommitInflightRequest skipQueue(Object skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  public BulkCommitInflightRequest transactions(List<BulkCommitInflightItem> transactions) {
    List<Map<String, Object>> items = new ArrayList<>();
    for (BulkCommitInflightItem item : transactions) {
      items.add(item.toMap());
    }
    fields.put("transactions", items);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public BulkCommitInflightRequest putAdditional(String key, Object value) {
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
