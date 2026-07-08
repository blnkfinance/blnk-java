package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Ledger document shape returned by {@code POST search/ledgers}. Typesense
 * timestamps are Unix-seconds NUMBERS — never converted to date objects.
 * Optional response convenience.
 */
public final class SearchLedgerDocument {

  private String id;
  private String ledgerId;
  private String name;
  private Long createdAt;
  private Map<String, Object> metaData;

  private SearchLedgerDocument() {}

  public static SearchLedgerDocument create() {
    return new SearchLedgerDocument();
  }

  public SearchLedgerDocument id(String id) {
    this.id = id;
    return this;
  }

  public SearchLedgerDocument ledgerId(String ledgerId) {
    this.ledgerId = ledgerId;
    return this;
  }

  public SearchLedgerDocument name(String name) {
    this.name = name;
    return this;
  }

  /** Typesense-indexed creation time (Unix timestamp seconds). */
  public SearchLedgerDocument createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public SearchLedgerDocument metaData(Map<String, Object> metaData) {
    this.metaData = metaData;
    return this;
  }

  public String id() {
    return id;
  }

  public String ledgerId() {
    return ledgerId;
  }

  public String name() {
    return name;
  }

  public Long createdAt() {
    return createdAt;
  }

  public Map<String, Object> metaData() {
    return metaData;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static SearchLedgerDocument fromJson(JsonNode node) {
    SearchLedgerDocument document = new SearchLedgerDocument();
    if (node == null || !node.isObject()) {
      return document;
    }
    document.id = text(node.get("id"));
    document.ledgerId = text(node.get("ledger_id"));
    document.name = text(node.get("name"));
    document.createdAt = longValue(node.get("created_at"));
    document.metaData = map(node.get("meta_data"));
    return document;
  }

  private static String text(JsonNode node) {
    return node == null || !node.isTextual() ? null : node.asText();
  }

  private static Long longValue(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.longValue();
  }

  private static Map<String, Object> map(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }
    return BlnkJson.mapper().convertValue(node, new TypeReference<Map<String, Object>>() {});
  }
}
