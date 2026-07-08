package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.ValueFormat;
import com.blnkfinance.blnk.util.StringUtils;

import java.util.Map;

/**
 * Validators for the balance-monitor endpoints. Each validator receives the
 * request payload as a raw {@code Map<String,Object>} and returns {@code null}
 * when the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class BalanceMonitorValidators {

  private BalanceMonitorValidators() {}

  /**
   * Validates a monitor id — used only by {@code BalanceMonitor.delete}
   * ({@code get} and {@code update} intentionally do not pre-validate ids).
   * Only a non-string value or the exact empty string fails; a whitespace-only
   * id is valid.
   */
  public static String validateMonitorId(String id) {
    if (!StringUtils.isValidString(id) || id.isEmpty()) {
      return "monitor id is required";
    }

    return null;
  }

  /**
   * Validates the monitor payload — used by both {@code create} and
   * {@code update}. Empty strings satisfy the string checks here. For
   * {@code description} and {@code call_back_url}, an absent key is treated as
   * "not provided" while a key explicitly present with a null value fails the
   * check.
   */
  public static String validateMonitorData(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type MonitorData";
    }

    // Type check only — an empty string passes.
    if (!StringUtils.isValidString(data.get("balance_id"))) {
      return "balance_id must be a valid string";
    }

    // Every condition sub-failure yields this single message.
    if (!isValidCondition(data.get("condition"))) {
      return "condition must be a valid MonitorCondition object";
    }

    // Absent key: skipped; a key present with a null value fails the check.
    if (data.containsKey("description") && !StringUtils.isValidString(data.get("description"))) {
      return "description must be a valid string if provided";
    }

    if (data.containsKey("call_back_url")
        && !StringUtils.isValidString(data.get("call_back_url"))) {
      return "call_back_url must be a valid string if provided";
    }

    // If all validations pass, return null
    return null;
  }

  /**
   * A condition is valid when it is a non-null map with a string
   * {@code field}, one of the six supported {@code operator} symbols, and
   * numeric {@code value} and {@code precision}. Any {@link Number} instance
   * is accepted, including NaN and infinities.
   */
  private static boolean isValidCondition(Object condition) {
    if (ValueFormat.isFalsy(condition)) {
      return false;
    }
    if (!(condition instanceof Map<?, ?> map)) {
      return false;
    }
    return StringUtils.isValidString(map.get("field"))
        && StringUtils.isValidOperator(map.get("operator"))
        && StringUtils.isValidNumber(map.get("value"))
        && StringUtils.isValidNumber(map.get("precision"));
  }
}
