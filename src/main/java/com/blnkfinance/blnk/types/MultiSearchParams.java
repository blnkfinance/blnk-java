package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST multi-search}: several single-collection
 * searches run in one round trip. Wire shape is Typesense's
 * {@code {"searches":[{"collection":..., "q":..., ...}]}}; results come back in
 * the same order.
 */
public final class MultiSearchParams {

  private final List<Map<String, Object>> searches = new ArrayList<>();

  private MultiSearchParams() {}

  public static MultiSearchParams create() {
    return new MultiSearchParams();
  }

  /** Appends one search against {@code collection}; {@code params} fields are copied in beside it. */
  public MultiSearchParams add(String collection, SearchParams params) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("collection", collection);
    if (params != null) {
      entry.putAll(params.toMap());
    }
    searches.add(entry);
    return this;
  }

  /** Copy of the entries in insertion order. */
  public List<Map<String, Object>> searches() {
    List<Map<String, Object>> copy = new ArrayList<>();
    for (Map<String, Object> entry : searches) {
      copy.add(new LinkedHashMap<>(entry));
    }
    return copy;
  }

  /** {@code {"searches": [...]}} as passed to validators. */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("searches", searches());
    return map;
  }

  /** Wire body, forwarded unmodified by {@code Search.multiSearch}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
