package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Validators for the reconciliation endpoints. Each validator receives the
 * request payload as a raw {@code Map<String,Object>} and returns {@code null}
 * when the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class ReconciliationValidators {

  private ReconciliationValidators() {}

  /** Maximum number of external transactions accepted by instant reconciliation. */
  private static final int MAX_INSTANT_RECON_ITEMS = 10000;

  private static final List<String> CRITERIA_FIELDS =
      Arrays.asList("amount", "currency", "reference", "description", "date");

  private static final List<String> OPERATORS =
      Arrays.asList("equals", "greater_than", "less_than", "contains");

  private static final List<String> STRATEGIES =
      Arrays.asList("one_to_one", "one_to_many", "many_to_one");

  private static boolean isValidCriteriaField(Object field) {
    return CRITERIA_FIELDS.contains(field);
  }

  private static boolean isValidOperator(Object operator) {
    return OPERATORS.contains(operator);
  }

  private static boolean isValidStrategy(Object strategy) {
    return STRATEGIES.contains(strategy);
  }

  /**
   * A criterion is valid when it is a map with a supported field and operator.
   * {@code allowable_drift} is optional: an absent key passes, a key present
   * with a null value fails, and any {@link Number} instance (including NaN)
   * passes.
   */
  private static boolean isValidCriteria(Object criterion) {
    if (!(criterion instanceof Map<?, ?> map)) {
      return false;
    }
    return isValidCriteriaField(map.get("field"))
        && isValidOperator(map.get("operator"))
        && (!map.containsKey("allowable_drift")
            || StringUtils.isValidNumber(map.get("allowable_drift")));
  }

  /** Validates a matching-rule payload — used by both create and update. */
  public static String validateMatcher(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type Matcher";
    }

    if (!StringUtils.isValidString(data.get("name"))) {
      return "name must be a valid string";
    }

    if (!StringUtils.isValidString(data.get("description"))) {
      return "description must be a valid string";
    }

    if (!(data.get("criteria") instanceof List<?> criteria)) {
      return "criteria must be a valid array";
    }

    // An empty criteria array passes — the loop never runs.
    for (Object criterion : criteria) {
      if (!isValidCriteria(criterion)) {
        return "Each criterion must be a valid object of type Criteria with valid field,"
            + " operator, and optional allowable_drift";
      }
    }

    return null;
  }

  /**
   * An external transaction must be a map with a numeric {@code amount} and
   * string {@code id}, {@code reference}, {@code currency},
   * {@code description}, {@code date}, and {@code source}.
   */
  private static boolean isValidExternalTransaction(Object txn) {
    if (!(txn instanceof Map<?, ?> map)) {
      return false;
    }
    return StringUtils.isValidString(map.get("id"))
        && StringUtils.isValidNumber(map.get("amount"))
        && StringUtils.isValidString(map.get("reference"))
        && StringUtils.isValidString(map.get("currency"))
        && StringUtils.isValidString(map.get("description"))
        && StringUtils.isValidString(map.get("date"))
        && StringUtils.isValidString(map.get("source"));
  }

  /** Validates the payload for instant reconciliation — used only by runInstant. */
  public static String validateRunInstantReconData(Map<String, Object> data) {
    if (data == null) {
      return "Data must be a valid object of type RunInstantReconData";
    }

    if (!(data.get("external_transactions") instanceof List<?> externalTransactions)
        || externalTransactions.isEmpty()) {
      return "external_transactions must be a non-empty array";
    }

    // Strict comparison — exactly 10000 items pass.
    if (externalTransactions.size() > MAX_INSTANT_RECON_ITEMS) {
      return "too many external_transactions; max is " + MAX_INSTANT_RECON_ITEMS;
    }

    // Transactions are validated before strategy — the ordering determines
    // which message wins when both are invalid.
    for (Object txn : externalTransactions) {
      if (!isValidExternalTransaction(txn)) {
        return "Each external transaction must include id, amount, reference, currency,"
            + " description, date, and source";
      }
    }

    // One shared message covers both a non-string and an unsupported strategy.
    Object strategy = data.get("strategy");
    if (!StringUtils.isValidString(strategy) || !isValidStrategy(strategy)) {
      return "strategy must be one of: one_to_one, one_to_many, many_to_one";
    }

    if (!(data.get("matching_rule_ids") instanceof List<?> matchingRuleIds)
        || matchingRuleIds.isEmpty()) {
      return "matching_rule_ids must be a non-empty array";
    }

    for (Object ruleId : matchingRuleIds) {
      if (!StringUtils.isValidString(ruleId)) {
        return "Each matching_rule_id must be a valid string";
      }
    }

    // An absent key is treated as "not provided"; a key present with a null
    // value fails the check.
    if (data.containsKey("dry_run") && !(data.get("dry_run") instanceof Boolean)) {
      return "dry_run must be a boolean";
    }

    if (data.containsKey("grouping_criteria")
        && !StringUtils.isValidString(data.get("grouping_criteria"))) {
      return "grouping_criteria must be a valid string";
    }

    return null;
  }
}
