package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional options for {@code ApiKeys.delete}. Validated by the SAME
 * validator as {@code ListApiKeysOptions}.
 */
public final class DeleteApiKeyOptions {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private DeleteApiKeyOptions() {}

  public static DeleteApiKeyOptions create() {
    return new DeleteApiKeyOptions();
  }

  public DeleteApiKeyOptions owner(String owner) {
    fields.put("owner", owner);
    return this;
  }

  /** Field map as passed to the validator; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
