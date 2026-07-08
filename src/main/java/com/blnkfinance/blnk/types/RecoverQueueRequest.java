package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional query options for {@code POST transactions/recover}.
 *
 * <p>{@code threshold} is a duration string ({@code 5m}, {@code 1h}); it is
 * sent as a query-string parameter, never as a body.
 */
public final class RecoverQueueRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private RecoverQueueRequest() {}

  public static RecoverQueueRequest create() {
    return new RecoverQueueRequest();
  }

  public RecoverQueueRequest threshold(String threshold) {
    fields.put("threshold", threshold);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public RecoverQueueRequest putAdditional(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** JSON view (unused on the wire — threshold travels in the query string). */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
