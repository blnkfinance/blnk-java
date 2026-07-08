package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST {collection}/filter}.
 *
 * <p>{@code filters} is required but may be EMPTY. Unset optional fields are
 * absent keys; wire names are snake_case. Numeric fields are {@link Number}
 * so non-integer values remain representable.
 */
public final class FilterParams {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private FilterParams() {}

  public static FilterParams create() {
    return new FilterParams();
  }

  public FilterParams filters(List<FilterCondition> filters) {
    fields.put("filters", filters);
    return this;
  }

  /** {@code "and"} or {@code "or"}. */
  public FilterParams logicalOperator(String logicalOperator) {
    fields.put("logical_operator", logicalOperator);
    return this;
  }

  public FilterParams sortBy(String sortBy) {
    fields.put("sort_by", sortBy);
    return this;
  }

  /** {@code "asc"} or {@code "desc"}. */
  public FilterParams sortOrder(String sortOrder) {
    fields.put("sort_order", sortOrder);
    return this;
  }

  public FilterParams includeCount(Boolean includeCount) {
    fields.put("include_count", includeCount);
    return this;
  }

  public FilterParams limit(Number limit) {
    fields.put("limit", limit);
    return this;
  }

  public FilterParams offset(Number offset) {
    fields.put("offset", offset);
    return this;
  }

  /**
   * Field map as passed to validators. {@link FilterCondition} elements are
   * converted to their own map views so the validator sees plain maps
   * throughout.
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>(fields);
    Object filters = map.get("filters");
    if (filters instanceof List<?> list) {
      List<Object> converted = new ArrayList<>();
      for (Object item : list) {
        converted.add(item instanceof FilterCondition condition ? condition.toMap() : item);
      }
      map.put("filters", converted);
    }
    return map;
  }

  /** Wire body — forwarded unmodified by {@code Search.filter}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
