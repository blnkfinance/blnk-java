package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matching-rule payload for {@code POST/PUT reconciliation/matching-rules}.
 * Forwarded to the wire unmodified.
 */
public final class Matcher {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private Matcher() {}

  public static Matcher create() {
    return new Matcher();
  }

  public Matcher name(String name) {
    fields.put("name", name);
    return this;
  }

  public Matcher description(String description) {
    fields.put("description", description);
    return this;
  }

  public Matcher criteria(List<Criteria> criteria) {
    fields.put("criteria", criteria);
    return this;
  }

  /**
   * Field map as passed to validators. {@link Criteria} elements are
   * converted to their own map views so the validator sees plain maps
   * throughout.
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>(fields);
    Object criteria = map.get("criteria");
    if (criteria instanceof List<?> list) {
      List<Object> converted = new ArrayList<>();
      for (Object item : list) {
        converted.add(item instanceof Criteria criterion ? criterion.toMap() : item);
      }
      map.put("criteria", converted);
    }
    return map;
  }

  /** Wire body. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
