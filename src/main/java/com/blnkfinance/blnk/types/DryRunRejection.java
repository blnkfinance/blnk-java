package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Projected rejection on a dry-run preview when {@code would_apply} is
 * {@code false}. {@code code} is the same catalog code a real post would
 * return.
 */
public final class DryRunRejection {

  private final String code;
  private final String reason;
  private final String message;

  private DryRunRejection(String code, String reason, String message) {
    this.code = code;
    this.reason = reason;
    this.message = message;
  }

  public static DryRunRejection fromJson(JsonNode json) {
    if (json == null || !json.isObject()) {
      return null;
    }
    return new DryRunRejection(text(json, "code"), text(json, "reason"), text(json, "message"));
  }

  public String code() {
    return code;
  }

  public String reason() {
    return reason;
  }

  public String message() {
    return message;
  }

  private static String text(JsonNode json, String field) {
    JsonNode node = json.get(field);
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
