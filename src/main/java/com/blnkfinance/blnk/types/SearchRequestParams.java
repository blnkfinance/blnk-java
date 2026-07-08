package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request params echoed back by Typesense (may include
 * {@code collection_name}). Optional response convenience — endpoint methods
 * still return the raw parsed {@code ApiResponse<JsonNode>}.
 */
public final class SearchRequestParams {

  private String q;
  private String queryBy;
  private String filterBy;
  private String sortBy;
  private Number page;
  private Number perPage;
  private String collectionName;

  private SearchRequestParams() {}

  public static SearchRequestParams create() {
    return new SearchRequestParams();
  }

  public SearchRequestParams q(String q) {
    this.q = q;
    return this;
  }

  public SearchRequestParams queryBy(String queryBy) {
    this.queryBy = queryBy;
    return this;
  }

  public SearchRequestParams filterBy(String filterBy) {
    this.filterBy = filterBy;
    return this;
  }

  public SearchRequestParams sortBy(String sortBy) {
    this.sortBy = sortBy;
    return this;
  }

  public SearchRequestParams page(Number page) {
    this.page = page;
    return this;
  }

  public SearchRequestParams perPage(Number perPage) {
    this.perPage = perPage;
    return this;
  }

  public SearchRequestParams collectionName(String collectionName) {
    this.collectionName = collectionName;
    return this;
  }

  public String q() {
    return q;
  }

  public String queryBy() {
    return queryBy;
  }

  public String filterBy() {
    return filterBy;
  }

  public String sortBy() {
    return sortBy;
  }

  public Number page() {
    return page;
  }

  public Number perPage() {
    return perPage;
  }

  public String collectionName() {
    return collectionName;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static SearchRequestParams fromJson(JsonNode node) {
    SearchRequestParams params = new SearchRequestParams();
    if (node == null || !node.isObject()) {
      return params;
    }
    params.q = text(node.get("q"));
    params.queryBy = text(node.get("query_by"));
    params.filterBy = text(node.get("filter_by"));
    params.sortBy = text(node.get("sort_by"));
    params.page = number(node.get("page"));
    params.perPage = number(node.get("per_page"));
    params.collectionName = text(node.get("collection_name"));
    return params;
  }

  private static String text(JsonNode node) {
    return node == null || !node.isTextual() ? null : node.asText();
  }

  private static Number number(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.numberValue();
  }
}
