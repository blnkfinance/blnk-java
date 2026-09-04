package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed view of a bulk dry-run preview. Core may return a single object with
 * a {@code transactions} (or {@code results}) array, or an array of per-item
 * previews.
 */
public final class DryRunBulkTransactionResponse {

  private final JsonNode json;
  private final List<DryRunTransactionResponse> transactions;

  private DryRunBulkTransactionResponse(JsonNode json, List<DryRunTransactionResponse> transactions) {
    this.json = json;
    this.transactions = transactions;
  }

  public static DryRunBulkTransactionResponse fromJson(JsonNode json) {
    if (json instanceof ArrayNode arrayNode) {
      return new DryRunBulkTransactionResponse(
          arrayNode.deepCopy(), parseArray(arrayNode));
    }
    if (!(json instanceof ObjectNode objectNode)) {
      return new DryRunBulkTransactionResponse(BlnkJson.objectNode(), List.of());
    }
    ObjectNode copy = objectNode.deepCopy();
    JsonNode items = copy.get("transactions");
    if (items == null || !items.isArray()) {
      items = copy.get("results");
    }
    if (items != null && items.isArray()) {
      return new DryRunBulkTransactionResponse(copy, parseArray(items));
    }
    if (DryRunTransactionResponse.isDryRun(copy) || copy.has("would_apply")) {
      return new DryRunBulkTransactionResponse(copy, List.of(DryRunTransactionResponse.fromJson(copy)));
    }
    return new DryRunBulkTransactionResponse(copy, List.of());
  }

  public JsonNode toJson() {
    return json.deepCopy();
  }

  public Boolean dryRun() {
    JsonNode node = json.isObject() ? json.get("dry_run") : null;
    return node != null && node.isBoolean() ? node.booleanValue() : Boolean.TRUE;
  }

  public Boolean wouldApply() {
    JsonNode node = json.isObject() ? json.get("would_apply") : null;
    return node != null && node.isBoolean() ? node.booleanValue() : null;
  }

  public Boolean cumulative() {
    JsonNode node = json.isObject() ? json.get("cumulative") : null;
    return node != null && node.isBoolean() ? node.booleanValue() : null;
  }

  public Boolean atomic() {
    JsonNode node = json.isObject() ? json.get("atomic") : null;
    return node != null && node.isBoolean() ? node.booleanValue() : null;
  }

  /**
   * Batch-level advisories, for example the warning that items are dispatched
   * concurrently and projected independently. Per-item notes stay on the
   * matching {@link DryRunTransactionResponse}.
   */
  public List<String> notes() {
    return DryRunTransactionResponse.notesOf(json.isObject() ? json : null);
  }

  public List<DryRunTransactionResponse> transactions() {
    return transactions;
  }

  private static List<DryRunTransactionResponse> parseArray(JsonNode items) {
    List<DryRunTransactionResponse> parsed = new ArrayList<>();
    for (JsonNode item : items) {
      parsed.add(DryRunTransactionResponse.fromJson(item));
    }
    return Collections.unmodifiableList(parsed);
  }
}
