package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One split leg of a transaction: {@code identifier} plus optional
 * {@code distribution}, {@code precise_distribution} and {@code narration}.
 *
 * <p>Unset fields are absent keys; wire names are snake_case.
 * {@code distribution} is a percentage ({@code "20%"}), fixed amount
 * ({@code "240.23"}) or {@code "left"}.
 */
public final class MultipleSourcesT {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private MultipleSourcesT() {}

  public static MultipleSourcesT create() {
    return new MultipleSourcesT();
  }

  public MultipleSourcesT identifier(String identifier) {
    fields.put("identifier", identifier);
    return this;
  }

  public MultipleSourcesT distribution(String distribution) {
    fields.put("distribution", distribution);
    return this;
  }

  public MultipleSourcesT preciseDistribution(double preciseDistribution) {
    fields.put("precise_distribution", preciseDistribution);
    return this;
  }

  public MultipleSourcesT preciseDistribution(String preciseDistribution) {
    fields.put("precise_distribution", preciseDistribution);
    return this;
  }

  public MultipleSourcesT narration(String narration) {
    fields.put("narration", narration);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public MultipleSourcesT putAdditional(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
