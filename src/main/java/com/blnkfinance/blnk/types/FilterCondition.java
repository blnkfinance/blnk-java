package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single filter condition in a DB filter request.
 *
 * <p>{@code value} is any JSON value and {@code values} any JSON array —
 * serialized as-is. For scalar operators, an absent {@code value} and an
 * explicit {@code null} both fail validation, while values like {@code 0},
 * {@code ""} and {@code false} are accepted.
 */
public final class FilterCondition {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private FilterCondition() {}

  public static FilterCondition create() {
    return new FilterCondition();
  }

  public FilterCondition field(String field) {
    fields.put("field", field);
    return this;
  }

  /** Operator literal, e.g. {@code "eq"}, {@code "in"}, {@code "isnull"}. */
  public FilterCondition operator(String operator) {
    fields.put("operator", operator);
    return this;
  }

  public FilterCondition value(Object value) {
    fields.put("value", value);
    return this;
  }

  public FilterCondition values(java.util.List<?> values) {
    fields.put("values", values);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
