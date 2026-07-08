package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Response from {@code GET balances/{balance_id}/lineage}.
 * {@code total_with_lineage} may arrive as either a string or a number, so it
 * is typed {@code Object} holding a {@link String} or a {@link Number}.
 * Optional response convenience — endpoint methods still return the raw
 * parsed {@code ApiResponse<JsonNode>}.
 */
public record BalanceLineageResponse(
    String balanceId,
    String aggregateBalanceId,
    Object totalWithLineage,
    List<LineageProviderBreakdown> providers) {

  /** Tolerant parse: missing keys → null/empty fields; extra keys ignored. */
  public static BalanceLineageResponse fromJson(JsonNode node) {
    List<LineageProviderBreakdown> providers = new ArrayList<>();
    JsonNode providersNode = node.get("providers");
    if (providersNode != null && providersNode.isArray()) {
      for (JsonNode provider : providersNode) {
        providers.add(LineageProviderBreakdown.fromJson(provider));
      }
    }
    return new BalanceLineageResponse(
        text(node.get("balance_id")),
        text(node.get("aggregate_balance_id")),
        LineageProviderBreakdown.stringOrNumber(node.get("total_with_lineage")),
        providers);
  }

  private static String text(JsonNode node) {
    return node == null || node.isNull() ? null : node.asText();
  }
}
