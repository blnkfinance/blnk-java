package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generic Typesense search response. Parameterize with the document type of
 * the collection being searched (balance searches use
 * {@code SearchResponse<SearchBalanceDocument>}). Optional response
 * convenience — endpoint methods still return the raw parsed
 * {@code ApiResponse<JsonNode>}.
 */
public final class SearchResponse<TDocument> {

  private Number found;
  private Number outOf;
  private Number page;
  private SearchRequestParams requestParams;
  private Number searchTimeMs;
  private List<Object> facetCounts;
  private Boolean searchCutoff;
  private List<SearchHit<TDocument>> hits;
  private List<SearchGroupedHit<TDocument>> groupedHits;

  private SearchResponse() {}

  public static <T> SearchResponse<T> create() {
    return new SearchResponse<>();
  }

  public SearchResponse<TDocument> found(Number found) {
    this.found = found;
    return this;
  }

  public SearchResponse<TDocument> outOf(Number outOf) {
    this.outOf = outOf;
    return this;
  }

  public SearchResponse<TDocument> page(Number page) {
    this.page = page;
    return this;
  }

  public SearchResponse<TDocument> requestParams(SearchRequestParams requestParams) {
    this.requestParams = requestParams;
    return this;
  }

  public SearchResponse<TDocument> searchTimeMs(Number searchTimeMs) {
    this.searchTimeMs = searchTimeMs;
    return this;
  }

  public SearchResponse<TDocument> facetCounts(List<Object> facetCounts) {
    this.facetCounts = facetCounts;
    return this;
  }

  public SearchResponse<TDocument> searchCutoff(Boolean searchCutoff) {
    this.searchCutoff = searchCutoff;
    return this;
  }

  public SearchResponse<TDocument> hits(List<SearchHit<TDocument>> hits) {
    this.hits = hits;
    return this;
  }

  public SearchResponse<TDocument> groupedHits(List<SearchGroupedHit<TDocument>> groupedHits) {
    this.groupedHits = groupedHits;
    return this;
  }

  public Number found() {
    return found;
  }

  public Number outOf() {
    return outOf;
  }

  public Number page() {
    return page;
  }

  public SearchRequestParams requestParams() {
    return requestParams;
  }

  public Number searchTimeMs() {
    return searchTimeMs;
  }

  public List<Object> facetCounts() {
    return facetCounts;
  }

  public Boolean searchCutoff() {
    return searchCutoff;
  }

  public List<SearchHit<TDocument>> hits() {
    return hits;
  }

  public List<SearchGroupedHit<TDocument>> groupedHits() {
    return groupedHits;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static <T> SearchResponse<T> fromJson(
      JsonNode node, Function<JsonNode, T> documentFromJson) {
    SearchResponse<T> response = new SearchResponse<>();
    if (node == null || !node.isObject()) {
      return response;
    }
    response.found = number(node.get("found"));
    response.outOf = number(node.get("out_of"));
    response.page = number(node.get("page"));
    JsonNode requestParams = node.get("request_params");
    if (requestParams != null && requestParams.isObject()) {
      response.requestParams = SearchRequestParams.fromJson(requestParams);
    }
    response.searchTimeMs = number(node.get("search_time_ms"));
    JsonNode facetCounts = node.get("facet_counts");
    if (facetCounts != null && facetCounts.isArray()) {
      List<Object> parsed = new ArrayList<>();
      for (JsonNode item : facetCounts) {
        parsed.add(item);
      }
      response.facetCounts = parsed;
    }
    JsonNode searchCutoff = node.get("search_cutoff");
    if (searchCutoff != null && searchCutoff.isBoolean()) {
      response.searchCutoff = searchCutoff.booleanValue();
    }
    JsonNode hits = node.get("hits");
    if (hits != null && hits.isArray()) {
      List<SearchHit<T>> parsed = new ArrayList<>();
      for (JsonNode hit : hits) {
        parsed.add(SearchHit.fromJson(hit, documentFromJson));
      }
      response.hits = parsed;
    }
    JsonNode groupedHits = node.get("grouped_hits");
    if (groupedHits != null && groupedHits.isArray()) {
      List<SearchGroupedHit<T>> parsed = new ArrayList<>();
      for (JsonNode groupedHit : groupedHits) {
        parsed.add(SearchGroupedHit.fromJson(groupedHit, documentFromJson));
      }
      response.groupedHits = parsed;
    }
    return response;
  }

  private static Number number(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.numberValue();
  }
}
