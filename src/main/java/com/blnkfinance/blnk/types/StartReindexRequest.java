package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional options for {@code POST search/reindex}.
 *
 * <p>NOTE: {@code Search.startReindex} REBUILDS the wire body from this
 * object — only {@code batch_size} is ever copied; anything else would be
 * silently dropped. {@code batchSize} is {@link Number} so non-integer values
 * remain representable.
 */
public final class StartReindexRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private StartReindexRequest() {}

  public static StartReindexRequest create() {
    return new StartReindexRequest();
  }

  public StartReindexRequest batchSize(Number batchSize) {
    fields.put("batch_size", batchSize);
    return this;
  }

  /** Field map as passed to the validator; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
