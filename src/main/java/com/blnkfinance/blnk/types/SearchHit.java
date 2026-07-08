package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Single Typesense search hit. Parameterize explicitly with the document type
 * of the collection being searched. Optional response convenience.
 */
public final class SearchHit<TDocument> {

  private TDocument document;
  private List<Object> highlights;
  private Map<String, Object> highlight;
  private Number textMatch;

  private SearchHit() {}

  public static <T> SearchHit<T> create() {
    return new SearchHit<>();
  }

  public SearchHit<TDocument> document(TDocument document) {
    this.document = document;
    return this;
  }

  public SearchHit<TDocument> highlights(List<Object> highlights) {
    this.highlights = highlights;
    return this;
  }

  public SearchHit<TDocument> highlight(Map<String, Object> highlight) {
    this.highlight = highlight;
    return this;
  }

  public SearchHit<TDocument> textMatch(Number textMatch) {
    this.textMatch = textMatch;
    return this;
  }

  public TDocument document() {
    return document;
  }

  public List<Object> highlights() {
    return highlights;
  }

  public Map<String, Object> highlight() {
    return highlight;
  }

  public Number textMatch() {
    return textMatch;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static <T> SearchHit<T> fromJson(JsonNode node, Function<JsonNode, T> documentFromJson) {
    SearchHit<T> hit = new SearchHit<>();
    if (node == null || !node.isObject()) {
      return hit;
    }
    JsonNode document = node.get("document");
    if (document != null && !document.isNull()) {
      hit.document = documentFromJson.apply(document);
    }
    JsonNode highlights = node.get("highlights");
    if (highlights != null && highlights.isArray()) {
      List<Object> parsed = new ArrayList<>();
      for (JsonNode item : highlights) {
        parsed.add(item);
      }
      hit.highlights = parsed;
    }
    JsonNode highlight = node.get("highlight");
    if (highlight != null && highlight.isObject()) {
      hit.highlight =
          BlnkJson.mapper().convertValue(highlight, new TypeReference<Map<String, Object>>() {});
    }
    JsonNode textMatch = node.get("text_match");
    if (textMatch != null && textMatch.isNumber()) {
      hit.textMatch = textMatch.numberValue();
    }
    return hit;
  }
}
