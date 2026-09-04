package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Before/after balance projection on a dry-run preview. Amounts are
 * minor-unit strings.
 */
public final class DryRunBalanceProjection {

  private final JsonNode json;

  private DryRunBalanceProjection(JsonNode json) {
    this.json = json;
  }

  public static DryRunBalanceProjection fromJson(JsonNode json) {
    return json != null && json.isObject() ? new DryRunBalanceProjection(json) : null;
  }

  public String balanceId() {
    return text("balance_id");
  }

  public String role() {
    return text("role");
  }

  public String currency() {
    return text("currency");
  }

  public Boolean virtual() {
    JsonNode node = json.get("virtual");
    return node != null && node.isBoolean() ? node.booleanValue() : null;
  }

  public String currentBalance() {
    return text("current_balance");
  }

  public String currentAvailable() {
    return text("current_available");
  }

  public String currentCreditBalance() {
    return text("current_credit_balance");
  }

  public String currentDebitBalance() {
    return text("current_debit_balance");
  }

  public String currentInflightDebitBalance() {
    return text("current_inflight_debit_balance");
  }

  public String currentInflightCreditBalance() {
    return text("current_inflight_credit_balance");
  }

  public String resultingBalance() {
    return text("resulting_balance");
  }

  public String resultingAvailable() {
    return text("resulting_available");
  }

  public String resultingCreditBalance() {
    return text("resulting_credit_balance");
  }

  public String resultingDebitBalance() {
    return text("resulting_debit_balance");
  }

  public String resultingInflightDebitBalance() {
    return text("resulting_inflight_debit_balance");
  }

  public String resultingInflightCreditBalance() {
    return text("resulting_inflight_credit_balance");
  }

  private String text(String field) {
    JsonNode node = json.get(field);
    return node == null || node.isNull() ? null : node.asText();
  }
}
