package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.types.MonitorTrigger;
import com.blnkfinance.blnk.util.ValueFormat;
import com.blnkfinance.blnk.util.StringUtils;

import java.util.List;
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

    String triggerError = validateTrigger(data);
    if (triggerError != null) {
      return triggerError;
    }

    // If all validations pass, return null
    return null;
  }

  /**
   * Validates the body of a partial monitor update ({@code
   * BalanceMonitor.updatePartial}). Every field is optional, but each one that
   * is present must be well-formed. {@code call_back_url} is rejected because
   * Core ignores it on update.
   */
  public static String validateUpdateMonitorData(Map<String, Object> data) {
    if (data == null) {
      return "Data must be a valid object of type UpdateMonitorData";
    }

    if (data.containsKey("balance_id") && !StringUtils.isValidString(data.get("balance_id"))) {
      return "balance_id must be a valid string if provided";
    }

    if (data.containsKey("description") && !StringUtils.isValidString(data.get("description"))) {
      return "description must be a valid string if provided";
    }

    String triggerError = validateTrigger(data);
    if (triggerError != null) {
      return triggerError;
    }

    if (data.containsKey("condition")) {
      if (!(data.get("condition") instanceof Map<?, ?> condition)) {
        return "condition must be a valid MonitorCondition object if provided";
      }
      if (condition.containsKey("field") && !StringUtils.isValidString(condition.get("field"))) {
        return "condition.field must be a valid string if provided";
      }
      if (condition.containsKey("operator")
          && !StringUtils.isValidOperator(condition.get("operator"))) {
        return "condition.operator must be one of != < <= = > >= if provided";
      }
      if (condition.containsKey("value") && !StringUtils.isValidNumber(condition.get("value"))) {
        return "condition.value must be a number if provided";
      }
      if (condition.containsKey("precision")
          && !StringUtils.isValidNumber(condition.get("precision"))) {
        return "condition.precision must be a number if provided";
      }
    }

    for (String key : data.keySet()) {
      if (!UPDATE_FIELDS.contains(key)) {
        return "Invalid field: " + key;
      }
    }

    return null;
  }

  private static final List<String> UPDATE_FIELDS =
      List.of("balance_id", "description", "trigger", "condition");

  /**
   * Mirrors Core's {@code NormalizeTrigger}: an absent or empty trigger means
   * "not set" (edge on create, the stored mode on update); anything else must
   * be exactly {@code edge} or {@code level}, case-sensitive.
   */
  private static String validateTrigger(Map<String, Object> data) {
    if (!data.containsKey("trigger") || "".equals(data.get("trigger"))) {
      return null;
    }
    if (!MonitorTrigger.VALUES.contains(data.get("trigger"))) {
      return "trigger must be either \"edge\" or \"level\" if provided";
    }
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
