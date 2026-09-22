package com.blnkfinance.blnk.validators;

import java.util.Map;
import java.util.Set;

/**
 * Validates {@code ListOptions}. Same rules Core applies ({@code limit >= 1},
 * {@code offset >= 0}), checked before the request is sent. Unknown query keys
 * are rejected rather than dropped so a misspelled option cannot silently
 * become Core's default page.
 *
 * <p>{@link com.blnkfinance.blnk.types.ListOptions} only exposes {@code limit}
 * and {@code offset} setters, so a typed builder cannot inject extra keys.
 * This check still applies when the payload is a map (tests, and any future
 * map-shaped caller).
 */
public final class ListValidators {

  private static final Set<String> LIST_QUERY_KEYS = Set.of("limit", "offset");

  private ListValidators() {}

  /** Returns {@code null} when valid, otherwise the failure message. */
  public static String validateListOptions(Map<String, Object> data) {
    if (data == null) {
      return "Data must be a valid object of type ListOptions";
    }

    if (data.containsKey("limit")) {
      if (!(data.get("limit") instanceof Integer limit)) {
        return "limit must be an integer if provided";
      }
      if (limit < 1) {
        return "limit must be at least 1";
      }
    }

    if (data.containsKey("offset")) {
      if (!(data.get("offset") instanceof Integer offset)) {
        return "offset must be an integer if provided";
      }
      if (offset < 0) {
        return "offset must be at least 0";
      }
    }

    for (String key : data.keySet()) {
      if (!LIST_QUERY_KEYS.contains(key)) {
        return "unsupported list option: " + key;
      }
    }

    return null;
  }
}
