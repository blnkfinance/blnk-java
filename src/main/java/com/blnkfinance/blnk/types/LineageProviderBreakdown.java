package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Per-provider fund breakdown in {@link BalanceLineageResponse#providers()}.
 * The {@code amount}, {@code available} and {@code spent} fields may arrive
 * as either strings or numbers, so they are typed {@code Object} and hold a
 * {@link String} or a {@link Number}. Optional response convenience —
 * endpoint methods still return the raw parsed {@code ApiResponse<JsonNode>}.
 */
public record LineageProviderBreakdown(
    String provider, Object amount, Object available, Object spent, String shadowBalanceId) {

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static LineageProviderBreakdown fromJson(JsonNode node) {
    return new LineageProviderBreakdown(
        text(node.get("provider")),
        stringOrNumber(node.get("amount")),
        stringOrNumber(node.get("available")),
        stringOrNumber(node.get("spent")),
        text(node.get("shadow_balance_id")));
  }

  private static String text(JsonNode node) {
    return node == null || node.isNull() ? null : node.asText();
  }

  /** String-or-number field: returns a String or a Number, or null for absent/null nodes. */
  static Object stringOrNumber(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return node.isNumber() ? node.numberValue() : node.asText();
  }
}
