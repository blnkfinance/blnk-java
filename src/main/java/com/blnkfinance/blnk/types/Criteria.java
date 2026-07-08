package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Matching-rule criterion for reconciliation. {@code field}/{@code operator}
 * are plain Strings so invalid values stay representable;
 * {@code allowable_drift} is optional — an unset key is absent on the wire.
 */
public final class Criteria {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private Criteria() {}

  public static Criteria create() {
    return new Criteria();
  }

  /** One of {@code amount, currency, reference, description, date}. */
  public Criteria field(String field) {
    fields.put("field", field);
    return this;
  }

  /** One of {@code equals, greater_than, less_than, contains}. */
  public Criteria operator(String operator) {
    fields.put("operator", operator);
    return this;
  }

  public Criteria allowableDrift(Number allowableDrift) {
    fields.put("allowable_drift", allowableDrift);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
