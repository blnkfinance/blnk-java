package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * Validators for the ledger-balance endpoints, plus the shared
 * {@link #isValidMetaData} helper. Each validator receives the request payload
 * as a raw {@code Map<String,Object>} (or plain scalar parameters) and returns
 * {@code null} when the payload is valid, otherwise the exact failure message
 * surfaced to callers. The first failing check wins, and validators never
 * throw.
 */
public final class LedgerBalanceValidators {

  private LedgerBalanceValidators() {}

  /**
   * The supported allocation strategies, in order: {@code FIFO}, {@code LIFO},
   * {@code PROPORTIONAL}. Plain strings rather than an enum, so unsupported
   * values like {@code "INVALID"} are representable and rejected at runtime.
   */
  public static final String[] ALLOCATION_STRATEGIES = {"FIFO", "LIFO", "PROPORTIONAL"};

  /** Validates the payload for creating a ledger balance (called by {@code create}). */
  public static String validateCreateLedgerBalance(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type CreateLedgerBalance";
    }

    // Type check only — an empty string passes.
    if (!StringUtils.isValidString(data.get("ledger_id"))) {
      return "ledger_id must be a valid string";
    }

    // An absent key is treated as "not provided"; a key present with a null
    // value fails the string check. An empty string passes.
    if (data.containsKey("identity_id") && !StringUtils.isValidString(data.get("identity_id"))) {
      return "identity_id must be a valid string if provided";
    }

    // Note: the message names 'USD'/'NGN' but the check is string-only — any
    // string (even "EUR" or "") passes; callers depend on this exact message.
    if (!StringUtils.isValidString(data.get("currency"))) {
      return "currency must be either 'USD' or 'NGN'";
    }

    // Maps and lists pass; null and primitives fail.
    if (data.containsKey("meta_data") && !isValidMetaData(data.get("meta_data"))) {
      return "meta_data must be a valid object if provided";
    }

    if (data.containsKey("track_fund_lineage")
        && !(data.get("track_fund_lineage") instanceof Boolean)) {
      return "track_fund_lineage must be a boolean if provided";
    }

    // Exact, case-sensitive membership.
    if (data.containsKey("allocation_strategy")
        && !Arrays.asList(ALLOCATION_STRATEGIES).contains(data.get("allocation_strategy"))) {
      return "allocation_strategy must be one of FIFO, LIFO, or PROPORTIONAL";
    }

    // If all validations pass, return null.
    return null;
  }

  /**
   * Whether a metadata value is acceptable: any non-null, non-primitive value
   * passes — including lists — while null, strings, numbers, and booleans
   * fail. Shared across the validator classes.
   */
  public static boolean isValidMetaData(Object meta) {
    return meta != null
        && !(meta instanceof String)
        && !(meta instanceof Number)
        && !(meta instanceof Boolean);
  }

  /** Validates the indicator/currency pair for {@code getByIndicator}. */
  public static String validateGetByIndicator(String indicator, String currency) {
    // Only a null value or the exact empty string fails — whitespace-only
    // values pass.
    if (!StringUtils.isValidString(indicator) || "".equals(indicator)) {
      return "indicator is required";
    }

    if (!StringUtils.isValidString(currency) || "".equals(currency)) {
      return "currency is required";
    }

    return null;
  }

  /** Validates the payload for {@code updateIdentity} (runs after the balanceId pre-check). */
  public static String validateUpdateBalanceIdentity(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type UpdateBalanceIdentity";
    }

    // A missing key fails the string check, and an explicit "" is rejected —
    // stricter than create, where an empty identity_id passes.
    Object identityId = data.get("identity_id");
    if (!StringUtils.isValidString(identityId) || "".equals(identityId)) {
      return "identity_id is required";
    }

    return null;
  }

  /** Validates optional snapshot options ({@code createSnapshot} calls this
   * only when options are provided). */
  public static String validateCreateBalanceSnapshot(Map<String, Object> data) {
    // No options at all is valid.
    if (data == null) {
      return null;
    }

    // Relational check only: 0 passes, so "positive" effectively means
    // non-negative, and a non-numeric batch_size is skipped rather than
    // rejected. Callers depend on this exact message.
    if (data.containsKey("batch_size")
        && data.get("batch_size") instanceof Number batchSize
        && batchSize.doubleValue() < 0) {
      return "batch_size must be positive";
    }

    return null;
  }

  /** Validates optional balance-retrieval options ({@code get} calls this only
   * when options are provided). */
  public static String validateGetBalance(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type GetBalanceRequest";
    }

    // An absent key is treated as "not provided"; a key present with a null
    // value fails the check.
    if (data.containsKey("from_source") && !(data.get("from_source") instanceof Boolean)) {
      return "from_source must be a boolean if provided";
    }

    if (data.containsKey("with_queued") && !(data.get("with_queued") instanceof Boolean)) {
      return "with_queued must be a boolean if provided";
    }

    return null;
  }

  /** Validates the options for {@code getAt} (runs after the balanceId
   * pre-check; here the options are required). */
  public static String validateGetBalanceAt(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type GetBalanceAtRequest";
    }

    Object timestamp = data.get("timestamp");
    if (!StringUtils.isValidString(timestamp) || "".equals(timestamp)) {
      return "timestamp is required";
    }

    // Note: from_source is intentionally not validated here, unlike
    // validateGetBalance.
    return null;
  }
}
