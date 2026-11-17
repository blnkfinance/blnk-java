package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Condition attached to a balance monitor: {@code field}, {@code operator},
 * {@code value} and {@code precision}.
 *
 * <p>Unset fields are absent keys; wire names are snake_case. {@code value}
 * also accepts a raw {@code Object} so wrong-typed input (e.g. a string) can
 * reach the validator.
 *
 * <p>{@code value} is in major units: Core stores {@code precise_value =
 * value × precision}. A $100 threshold on a USD balance is {@code value(100)}
 * with {@code precision(100)}, not {@code value(10000)}. Monitors read back
 * from Core also carry the computed {@code precise_value}.
 *
 * <p>The same builder is used by {@link UpdateMonitorData}, where every field
 * is optional and only the ones set are sent.
 */
public final class MonitorCondition {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private MonitorCondition() {}

  public static MonitorCondition create() {
    return new MonitorCondition();
  }

  public MonitorCondition field(String field) {
    fields.put("field", field);
    return this;
  }

  /** One of {@code "!=" "<" "<=" "=" ">" ">="}; the validator compares raw strings. */
  public MonitorCondition operator(String operator) {
    fields.put("operator", operator);
    return this;
  }

  public MonitorCondition value(double value) {
    fields.put("value", value);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-number input reach the validator. */
  public MonitorCondition value(Object value) {
    fields.put("value", value);
    return this;
  }

  public MonitorCondition precision(double precision) {
    fields.put("precision", precision);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body fragment. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
