package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Grouped Typesense search hit: a group key plus the hits in that group.
 * Optional response convenience.
 */
public final class SearchGroupedHit<TDocument> {

  private List<String> groupKey;
  private List<SearchHit<TDocument>> hits;

  private SearchGroupedHit() {}

  public static <T> SearchGroupedHit<T> create() {
    return new SearchGroupedHit<>();
  }

  public SearchGroupedHit<TDocument> groupKey(List<String> groupKey) {
    this.groupKey = groupKey;
    return this;
  }

  public SearchGroupedHit<TDocument> hits(List<SearchHit<TDocument>> hits) {
    this.hits = hits;
    return this;
  }

  public List<String> groupKey() {
    return groupKey;
  }

  public List<SearchHit<TDocument>> hits() {
    return hits;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static <T> SearchGroupedHit<T> fromJson(
      JsonNode node, Function<JsonNode, T> documentFromJson) {
    SearchGroupedHit<T> groupedHit = new SearchGroupedHit<>();
    if (node == null || !node.isObject()) {
      return groupedHit;
    }
    JsonNode groupKey = node.get("group_key");
    if (groupKey != null && groupKey.isArray()) {
      List<String> parsed = new ArrayList<>();
      for (JsonNode item : groupKey) {
        parsed.add(item.isNull() ? null : item.asText());
      }
      groupedHit.groupKey = parsed;
    }
    JsonNode hits = node.get("hits");
    if (hits != null && hits.isArray()) {
      List<SearchHit<T>> parsed = new ArrayList<>();
      for (JsonNode hit : hits) {
        parsed.add(SearchHit.fromJson(hit, documentFromJson));
      }
      groupedHit.hits = parsed;
    }
    return groupedHit;
  }
}
