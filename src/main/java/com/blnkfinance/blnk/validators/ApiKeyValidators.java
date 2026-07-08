package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.DateSerialization;
import com.blnkfinance.blnk.util.StringUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.Map;

/**
 * Validators for the API-key endpoints. Each validator receives the request
 * payload as a raw {@code Map<String,Object>} and returns {@code null} when
 * the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class ApiKeyValidators {

  private ApiKeyValidators() {}

  /** Validates the payload for creating an API key. */
  public static String validateCreateApiKeyData(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type CreateApiKeyData";
    }

    // Emptiness is an exact empty-string comparison (no trim) — whitespace-only
    // values pass.
    Object name = data.get("name");
    if (!StringUtils.isValidString(name) || name.equals("")) {
      return "name is required";
    }

    Object owner = data.get("owner");
    if (!StringUtils.isValidString(owner) || owner.equals("")) {
      return "owner is required";
    }

    Object scopes = data.get("scopes");
    if (!StringUtils.isValidArray(scopes) || arrayLength(scopes) == 0) {
      return "at least one scope must be specified";
    }

    // Each scope is checked in order — the first offending element wins.
    int length = arrayLength(scopes);
    for (int index = 0; index < length; index++) {
      Object scope = arrayGet(scopes, index);
      if (!StringUtils.isValidString(scope) || scope.equals("")) {
        return "each scope must be a non-empty string";
      }
    }

    // Note: the message says "ISO 8601" but the accepted format is RFC 3339
    // without fractional seconds; callers depend on this exact message text.
    Object expiresAt = data.get("expires_at");
    if (!StringUtils.isValidString(expiresAt)
        || !DateSerialization.isValidTransactionDateInput(expiresAt)) {
      return "expires_at must be a valid ISO 8601 datetime string";
    }

    return null;
  }

  /**
   * Validates optional listing options. A {@code null} map means no options
   * were provided and is valid.
   */
  public static String validateListApiKeysOptions(Map<String, Object> options) {
    if (options == null) {
      return null;
    }

    // An absent key is treated as "not provided"; a key present with a null
    // value fails the check.
    if (options.containsKey("owner")) {
      Object owner = options.get("owner");
      if (!StringUtils.isValidString(owner) || owner.equals("")) {
        return "owner must be a non-empty string";
      }
    }

    return null;
  }

  /**
   * Validates optional delete options — intentionally the same rules as
   * {@link #validateListApiKeysOptions}.
   */
  public static String validateDeleteApiKeyOptions(Map<String, Object> options) {
    return validateListApiKeysOptions(options);
  }

  private static int arrayLength(Object value) {
    if (value instanceof List<?> list) {
      return list.size();
    }
    if (value instanceof ArrayNode arrayNode) {
      return arrayNode.size();
    }
    if (value != null && value.getClass().isArray()) {
      return java.lang.reflect.Array.getLength(value);
    }
    return 0;
  }

  private static Object arrayGet(Object value, int index) {
    if (value instanceof List<?> list) {
      return list.get(index);
    }
    if (value instanceof ArrayNode arrayNode) {
      return arrayNode.get(index);
    }
    if (value != null && value.getClass().isArray()) {
      return java.lang.reflect.Array.get(value, index);
    }
    return null;
  }
}
