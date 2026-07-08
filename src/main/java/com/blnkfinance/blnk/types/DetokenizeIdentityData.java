package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST identities/{id}/detokenize}:
 * {@code {"fields": [...]}}.
 *
 * <p>Field names are the PascalCase Core field names (see
 * {@link TokenizableIdentityFields}). An EMPTY {@code fields} array is allowed
 * and means "detokenize all currently tokenized fields".
 */
public final class DetokenizeIdentityData {

  private final Map<String, Object> data = new LinkedHashMap<>();

  private DetokenizeIdentityData() {}

  public static DetokenizeIdentityData create() {
    return new DetokenizeIdentityData();
  }

  public DetokenizeIdentityData fields(List<String> fields) {
    data.put("fields", fields == null ? null : new ArrayList<>(fields));
    return this;
  }

  public DetokenizeIdentityData fields(String... fields) {
    return fields(Arrays.asList(fields));
  }

  /** Field map as passed to validators. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(data);
  }

  /** Wire body — sent as-is (no copy/transform), exactly {@code {"fields": [...]}}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
