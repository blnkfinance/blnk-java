package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional filter for {@code Hooks.list}. {@code type} is a plain String (see
 * {@link HookType}) — interpolated RAW into {@code hooks?type=...} without
 * URL encoding.
 */
public final class ListHooksOptions {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private ListHooksOptions() {}

  public static ListHooksOptions create() {
    return new ListHooksOptions();
  }

  /** {@code "PRE_TRANSACTION"} or {@code "POST_TRANSACTION"} — see {@link HookType}. */
  public ListHooksOptions type(String type) {
    fields.put("type", type);
    return this;
  }

  /** Field map as passed to the validator; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
