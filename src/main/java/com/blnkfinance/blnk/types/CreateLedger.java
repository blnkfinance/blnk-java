package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code POST ledgers}: a required {@code name} plus
 * optional {@code meta_data}.
 *
 * <p>Unset fields are omitted from the serialized body entirely; wire names
 * are snake_case.
 */
public final class CreateLedger {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private CreateLedger() {}

  public static CreateLedger create() {
    return new CreateLedger();
  }

  public CreateLedger name(String name) {
    fields.put("name", name);
    return this;
  }

  public CreateLedger metaData(Map<String, Object> metaData) {
    fields.put("meta_data", metaData);
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
