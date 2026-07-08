package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST api-keys}. {@code expires_at} is a
 * caller-supplied STRING — validated (RFC3339 without fractional seconds) and
 * sent VERBATIM, never reformatted.
 */
public final class CreateApiKeyData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private CreateApiKeyData() {}

  public static CreateApiKeyData create() {
    return new CreateApiKeyData();
  }

  public CreateApiKeyData name(String name) {
    fields.put("name", name);
    return this;
  }

  public CreateApiKeyData owner(String owner) {
    fields.put("owner", owner);
    return this;
  }

  public CreateApiKeyData scopes(List<String> scopes) {
    fields.put("scopes", scopes);
    return this;
  }

  /** ISO 8601 / RFC3339 datetime string, e.g. {@code 2026-03-11T00:00:00Z}. */
  public CreateApiKeyData expiresAt(String expiresAt) {
    fields.put("expires_at", expiresAt);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body — forwarded unmodified by {@code ApiKeys.create}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
