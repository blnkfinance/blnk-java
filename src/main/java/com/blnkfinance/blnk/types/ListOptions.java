package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pagination for Core GET list routes. Sent as query parameters; unset fields
 * are omitted so Core can apply its per-route defaults ({@code limit=10} for
 * ledgers and balances, {@code limit=20} for transactions; {@code offset=0}
 * on both).
 *
 * <p>Only {@code limit} and {@code offset} setters exist — unknown query keys
 * cannot be introduced through this builder. {@link
 * com.blnkfinance.blnk.validators.ListValidators} still rejects extra keys
 * when it is given a map.
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
