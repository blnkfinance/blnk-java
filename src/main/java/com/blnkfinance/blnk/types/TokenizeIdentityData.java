package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST identities/{id}/tokenize}:
 * {@code {"fields": [...]}}.
 *
 * <p>Field names are the PascalCase Core field names (see
 * {@link TokenizableIdentityFields}), NOT the snake_case IdentityData JSON
 * keys. The validator requires at least one field; it does NOT restrict
 * values to the known list — any non-empty string passes.
 */
public final class TokenizeIdentityData {

  private final Map<String, Object> data = new LinkedHashMap<>();

  private TokenizeIdentityData() {}

  public static TokenizeIdentityData create() {
    return new TokenizeIdentityData();
  }

  public TokenizeIdentityData fields(List<String> fields) {
    data.put("fields", fields == null ? null : new ArrayList<>(fields));
    return this;
  }

  public TokenizeIdentityData fields(String... fields) {
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
