package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One entry per split on a dry-run preview that used {@code sources} or
 * {@code destinations}. The balance is reported as {@code identifier} because a
 * leg can name an internal {@code @} balance that does not exist yet.
 */
public final class DryRunLegProjection {

  private final JsonNode json;

  private DryRunLegProjection(JsonNode json) {
    this.json = json;
  }

  public static DryRunLegProjection fromJson(JsonNode json) {
    return json != null && json.isObject() ? new DryRunLegProjection(json) : null;
  }

  public String identifier() {
    JsonNode node = json.get("identifier");
    return node != null && node.isTextual() ? node.asText() : null;
  }

  /** {@code source} or {@code destination}. */
  public String role() {
    JsonNode node = json.get("role");
    return node != null && node.isTextual() ? node.asText() : null;
  }

  /** Leg amount in minor units, kept as a string so nothing is rounded. */
  public String preciseAmount() {
    JsonNode node = json.get("precise_amount");
    return node == null || node.isNull() ? null : node.asText();
  }

  public Double amount() {
    JsonNode node = json.get("amount");
    return node != null && node.isNumber() ? node.doubleValue() : null;
  }
}
