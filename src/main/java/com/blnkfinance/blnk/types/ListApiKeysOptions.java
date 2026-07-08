package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional filter for {@code ApiKeys.list}. {@code owner} IS URL-encoded when
 * placed in the query string.
 */
public final class ListApiKeysOptions {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private ListApiKeysOptions() {}

  public static ListApiKeysOptions create() {
    return new ListApiKeysOptions();
  }

  public ListApiKeysOptions owner(String owner) {
    fields.put("owner", owner);
    return this;
  }

  /** Field map as passed to the validator; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
