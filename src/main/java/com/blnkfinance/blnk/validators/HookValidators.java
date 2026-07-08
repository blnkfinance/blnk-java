package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Validators for the hook endpoints. Each validator receives the request
 * payload as a raw {@code Map<String,Object>} and returns {@code null} when
 * the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class HookValidators {

  private HookValidators() {}

  private static final List<String> HOOK_TYPES =
      Arrays.asList("PRE_TRANSACTION", "POST_TRANSACTION");

  /** Exact, case-sensitive membership check against the two hook types. */
  private static boolean isValidHookType(Object type) {
    return HOOK_TYPES.contains(type);
  }

  /** Validates the payload for creating a hook. */
  public static String validateCreateHookData(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type CreateHookData";
    }

    // Emptiness is an exact empty-string comparison (no trim) — whitespace-only
    // names pass.
    Object name = data.get("name");
    if (!StringUtils.isValidString(name) || name.equals("")) {
      return "name is required";
    }

    Object url = data.get("url");
    if (!StringUtils.isValidString(url) || url.equals("")) {
      return "url is required";
    }

    Object type = data.get("type");
    if (!StringUtils.isValidString(type) || !isValidHookType(type)) {
      return "type must be PRE_TRANSACTION or POST_TRANSACTION";
    }

    if (!(data.get("active") instanceof Boolean)) {
      return "active must be a boolean";
    }

    // Note: because the comparison rejects (rather than accepts) a range,
    // NaN passes the positivity check.
    Object timeout = data.get("timeout");
    if (!StringUtils.isValidNumber(timeout) || ((Number) timeout).doubleValue() <= 0) {
      return "timeout must be a positive number";
    }

    Object retryCount = data.get("retry_count");
    if (!StringUtils.isValidNumber(retryCount) || ((Number) retryCount).doubleValue() < 0) {
      return "retry_count must be a non-negative number";
    }

    return null;
  }

  /**
   * Validates the update payload — the same rules as create. Note: an invalid
   * update payload yields the message naming {@code CreateHookData}; callers
   * depend on this exact string.
   */
  public static String validateUpdateHookData(Map<String, Object> data) {
    return validateCreateHookData(data);
  }

  /**
   * Validates optional listing options. A {@code null} map means no options
   * were provided and is valid.
   */
  public static String validateListHooksOptions(Map<String, Object> options) {
    if (options == null) {
      return null;
    }

    // An absent key is treated as "not provided"; a key present with a null
    // value fails the check.
    if (options.containsKey("type")) {
      Object type = options.get("type");
      if (!StringUtils.isValidString(type) || !isValidHookType(type)) {
        return "type must be PRE_TRANSACTION or POST_TRANSACTION";
      }
    }

    return null;
  }
}
