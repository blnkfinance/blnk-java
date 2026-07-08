package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code POST hooks} and {@code PUT hooks/{id}} — the same
 * shape serves both create and update. {@code type} is a plain String (see
 * {@link HookType}) and {@code timeout}/{@code retryCount} are {@link Number}
 * so the validator's type checks stay reachable.
 */
public final class CreateHookData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private CreateHookData() {}

  public static CreateHookData create() {
    return new CreateHookData();
  }

  public CreateHookData name(String name) {
    fields.put("name", name);
    return this;
  }

  public CreateHookData url(String url) {
    fields.put("url", url);
    return this;
  }

  /** {@code "PRE_TRANSACTION"} or {@code "POST_TRANSACTION"} — see {@link HookType}. */
  public CreateHookData type(String type) {
    fields.put("type", type);
    return this;
  }

  public CreateHookData active(Boolean active) {
    fields.put("active", active);
    return this;
  }

  public CreateHookData timeout(Number timeout) {
    fields.put("timeout", timeout);
    return this;
  }

  public CreateHookData retryCount(Number retryCount) {
    fields.put("retry_count", retryCount);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body — forwarded unmodified by {@code Hooks.create}/{@code update}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
