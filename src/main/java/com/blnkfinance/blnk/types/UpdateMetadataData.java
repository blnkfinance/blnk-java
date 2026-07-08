package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for metadata updates: {@code { meta_data: {...} }}.
 *
 * <p>An unset field is an absent key; explicitly setting {@code null} keeps
 * the key with a null value, which fails validation with
 * {@code meta_data must be a valid object}.
 */
public final class UpdateMetadataData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private UpdateMetadataData() {}

  public static UpdateMetadataData create() {
    return new UpdateMetadataData();
  }

  public UpdateMetadataData metaData(Map<String, Object> metaData) {
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
