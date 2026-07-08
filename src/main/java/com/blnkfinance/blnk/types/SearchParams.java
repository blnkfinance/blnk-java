package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code POST search/{collection}}.
 *
 * <p>Unset fields are omitted from the serialized body entirely; wire names
 * are snake_case. Numeric fields are {@link Number} so non-integer values
 * remain representable.
 */
public final class SearchParams {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private SearchParams() {}

  public static SearchParams create() {
    return new SearchParams();
  }

  public SearchParams q(String q) {
    fields.put("q", q);
    return this;
  }

  public SearchParams queryBy(String queryBy) {
    fields.put("query_by", queryBy);
    return this;
  }

  public SearchParams filterBy(String filterBy) {
    fields.put("filter_by", filterBy);
    return this;
  }

  public SearchParams sortBy(String sortBy) {
    fields.put("sort_by", sortBy);
    return this;
  }

  public SearchParams page(Number page) {
    fields.put("page", page);
    return this;
  }

  public SearchParams perPage(Number perPage) {
    fields.put("per_page", perPage);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body — forwarded unmodified by {@code Search.search}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
