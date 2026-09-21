package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pagination for {@code GET ledgers} and {@code GET balances}. Sent as query
 * parameters; Core defaults to {@code limit=10}, {@code offset=0}.
 */
public final class ListOptions {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private ListOptions() {}

  public static ListOptions create() {
    return new ListOptions();
  }

  /** Page size, at least 1. */
  public ListOptions limit(int limit) {
    fields.put("limit", limit);
    return this;
  }

  /** Untyped variant; non-integers are rejected by validation. */
  public ListOptions limit(Object limit) {
    fields.put("limit", limit);
    return this;
  }

  /** Rows to skip, at least 0. */
  public ListOptions offset(int offset) {
    fields.put("offset", offset);
    return this;
  }

  /** Untyped variant; non-integers are rejected by validation. */
  public ListOptions offset(Object offset) {
    fields.put("offset", offset);
    return this;
  }

  /** Unset fields have no key. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** {@code "?limit=20&offset=40"} in set order, or {@code ""} when empty. */
  public String toQueryString() {
    if (fields.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("?");
    fields.forEach((key, value) -> {
      if (sb.length() > 1) {
        sb.append('&');
      }
      sb.append(key).append('=').append(value);
    });
    return sb.toString();
  }
}
