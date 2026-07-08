package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.IdentitySerialization;
import com.blnkfinance.blnk.util.StringUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validators for the identity endpoints. Each validator receives the request
 * payload as a raw {@code Map<String,Object>} and returns {@code null} when
 * the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins.
 */
public final class IdentityValidators {

  private IdentityValidators() {}

  /**
   * Matching is case-insensitive: uppercase {@code IDT_} prefixes and
   * uppercase hex digits validate.
   */
  public static final Pattern IDENTITY_ID_PATTERN =
      Pattern.compile(
          "^idt_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
          Pattern.CASE_INSENSITIVE);

  public static final String[] VALID_GENDERS = {"male", "female", "other"};

  /**
   * Validates an identity payload — used by {@code create} and {@code update}.
   * Optional-field gates test key presence: an absent key is treated as "not
   * provided", while a key explicitly present with a null value fails its
   * check. A payload containing only {@code identity_type} is fully valid.
   * Note: a null map is not handled here — the resulting
   * {@link NullPointerException} propagates to the calling endpoint's catch
   * block and surfaces as a 500-shaped error response.
   */
  public static String validateIdentity(Map<String, Object> data) {
    // Strict, case-sensitive equality; an absent identity_type also fails here.
    Object identityType = data.get("identity_type");
    if (!"individual".equals(identityType) && !"organization".equals(identityType)) {
      return "identity_type must be individual or organization";
    }

    // The value is coerced to a string before the regex test (null reads as
    // the literal "null" and fails to match).
    if (data.containsKey("identity_id")
        && !IDENTITY_ID_PATTERN.matcher(jsString(data.get("identity_id"))).matches()) {
      return "identity_id must start with idt_ followed by a valid UUID";
    }

    if (data.containsKey("dob")
        && !IdentitySerialization.isValidIdentityDateInput(data.get("dob"))) {
      return "dob must be a valid ISO 8601 date string or Date";
    }

    // Exact membership — type-based and case-sensitive.
    if (data.containsKey("gender")
        && !Arrays.asList(VALID_GENDERS).contains(data.get("gender"))) {
      return "gender must be male, female, or other if provided";
    }

    // Lists pass the metadata check; an explicit null fails.
    if (data.containsKey("meta_data")
        && !LedgerBalanceValidators.isValidMetaData(data.get("meta_data"))) {
      return "meta_data must be a valid object if provided";
    }

    return null;
  }

  /**
   * Validates an identity id path parameter — used by {@code delete},
   * {@code getTokenizedFields} and as step 1 of the tokenize/detokenize
   * validators. Only a null id or the exact empty string fails —
   * whitespace-only ids pass, and the {@code idt_} + UUID pattern is
   * intentionally not applied to path ids.
   */
  public static String validateIdentityId(String id) {
    if (!StringUtils.isValidString(id) || id.isEmpty()) {
      return "identity id is required";
    }

    return null;
  }

  /**
   * Validates the id/field pair — used by {@code tokenizeField} and
   * {@code detokenizeField}. The field name is intentionally not restricted to
   * the tokenizable field set, and whitespace-only field names pass.
   */
  public static String validateTokenizeIdentityField(String id, String field) {
    String idError = validateIdentityId(id);
    if (idError != null) {
      return idError;
    }

    if (!StringUtils.isValidString(field) || field.isEmpty()) {
      return "field name is required";
    }

    return null;
  }

  /**
   * Validates the payload for {@code detokenize}. An empty fields array is
   * allowed (detokenize-all semantics). A non-array {@code fields} value
   * yields the distinct message {@code fields must be an array} — tokenize has
   * no such branch.
   */
  public static String validateDetokenizeIdentityData(String id, Map<String, Object> data) {
    String idError = validateIdentityId(id);
    if (idError != null) {
      return idError;
    }

    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type DetokenizeIdentityData";
    }

    if (!StringUtils.isValidArray(data.get("fields"))) {
      return "fields must be an array";
    }

    for (Object field : asList(data.get("fields"))) {
      if (!StringUtils.isValidString(field) || ((String) field).isEmpty()) {
        return "each field must be a non-empty string";
      }
    }

    return null;
  }

  /**
   * Validates the payload for {@code tokenize}. Note: a non-array
   * {@code fields} value also yields {@code at least one field must be
   * specified} — there is no separate "fields must be an array" branch here,
   * and callers depend on this asymmetry with detokenize.
   */
  public static String validateTokenizeIdentityData(String id, Map<String, Object> data) {
    String idError = validateIdentityId(id);
    if (idError != null) {
      return idError;
    }

    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type TokenizeIdentityData";
    }

    Object fields = data.get("fields");
    if (!StringUtils.isValidArray(fields) || asList(fields).isEmpty()) {
      return "at least one field must be specified";
    }

    for (Object field : asList(fields)) {
      if (!StringUtils.isValidString(field) || ((String) field).isEmpty()) {
        return "each field must be a non-empty string";
      }
    }

    return null;
  }

  /**
   * String coercion used by the regex test: a null value reads as the literal
   * {@code "null"}.
   */
  private static String jsString(Object value) {
    return value == null ? "null" : String.valueOf(value);
  }

  /** Returns the value as a List for each array shape {@code StringUtils.isValidArray} accepts. */
  private static List<Object> asList(Object value) {
    if (value instanceof List<?> list) {
      return new ArrayList<>(list);
    }
    if (value instanceof ArrayNode arrayNode) {
      List<Object> items = new ArrayList<>(arrayNode.size());
      arrayNode.forEach(items::add);
      return items;
    }
    if (value != null && value.getClass().isArray()) {
      int length = Array.getLength(value);
      List<Object> items = new ArrayList<>(length);
      for (int i = 0; i < length; i++) {
        items.add(Array.get(value, i));
      }
      return items;
    }
    return new ArrayList<>();
  }
}
