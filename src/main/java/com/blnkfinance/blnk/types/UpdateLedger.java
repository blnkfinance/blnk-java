package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code PUT ledgers/{id}}: the new ledger {@code name}.
 */
public final class UpdateLedger {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private UpdateLedger() {}

  public static UpdateLedger create() {
    return new UpdateLedger();
  }

  public UpdateLedger name(String name) {
    fields.put("name", name);
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
