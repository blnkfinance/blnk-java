package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.StringUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Validators for the search endpoints. Each validator receives the request
 * payload as a raw {@code Map<String,Object>} and returns {@code null} when
 * the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class SearchValidators {

  private SearchValidators() {}

  // Arrays.asList (not List.of) so membership checks stay null-safe — a
  // validator must never throw.
  private static final List<String> SEARCH_COLLECTIONS =
      Arrays.asList("ledgers", "transactions", "balances", "identities");

  private static final int MAX_PER_PAGE = 250;

  private static final List<String> FILTER_OPERATORS =
      Arrays.asList(
          "eq", "ne", "gt", "gte", "lt", "lte", "in", "between", "like", "ilike", "isnull",
          "isnotnull");

  private static final List<String> VALUELESS_OPERATORS = Arrays.asList("isnull", "isnotnull");
  private static final List<String> VALUES_ARRAY_OPERATORS = Arrays.asList("in", "between");

  private static final int MAX_FILTER_LIMIT = 100;

  /**
   * Validates a search collection name — strict, case-sensitive membership
   * against the four literal collection names. Note: the parameter is named
   * {@code service} but carries the collection name.
   */
  public static String validateSearchCollection(String service) {
    if (!SEARCH_COLLECTIONS.contains(service)) {
      return "collection must be ledgers, transactions, balances, or identities";
    }
    return null;
  }

  /**
   * Validates search parameters. Checks run in a fixed order — {@code q},
   * {@code page}, {@code per_page}, {@code query_by}, {@code filter_by},
   * {@code sort_by} — so the first failing field's message wins.
   */
  public static String validateSearchParams(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Search params must be a valid object";
    }

    // One message covers a missing, empty, and whitespace-only q.
    Object q = data.get("q");
    if (!StringUtils.isValidString(q) || ((String) q).strip().isEmpty()) {
      return "Field \"q\" must be filled";
    }

    // For each optional field, an absent key is treated as "not provided"; a
    // key present with a null value fails the check.
    if (data.containsKey("page")) {
      Object page = data.get("page");
      if (!numberIsInteger(page) || ((Number) page).doubleValue() < 1) {
        return "page must be a positive integer if provided";
      }
    }

    if (data.containsKey("per_page")) {
      Object perPage = data.get("per_page");
      if (!numberIsInteger(perPage)
          || ((Number) perPage).doubleValue() < 1
          || ((Number) perPage).doubleValue() > MAX_PER_PAGE) {
        return "per_page must be an integer between 1 and 250 if provided";
      }
    }

    if (data.containsKey("query_by") && !StringUtils.isValidString(data.get("query_by"))) {
      return "query_by must be a string if provided";
    }

    if (data.containsKey("filter_by") && !StringUtils.isValidString(data.get("filter_by"))) {
      return "filter_by must be a string if provided";
    }

    if (data.containsKey("sort_by") && !StringUtils.isValidString(data.get("sort_by"))) {
      return "sort_by must be a string if provided";
    }

    return null;
  }

  /** Validates filter parameters. */
  public static String validateFilterParams(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Filter params must be a valid object";
    }

    // An empty filters array is valid.
    Object filters = data.get("filters");
    if (!StringUtils.isValidArray(filters)) {
      return "filters must be an array";
    }

    // Per-element validation, in order — element errors win over ALL
    // top-level optional-field errors below.
    int length = arrayLength(filters);
    for (int index = 0; index < length; index++) {
      String filterError = validateFilterCondition(arrayGet(filters, index), index);
      if (filterError != null) {
        return filterError;
      }
    }

    if (data.containsKey("logical_operator")) {
      Object logicalOperator = data.get("logical_operator");
      if (!(logicalOperator instanceof String s) || !isFilterLogicalOperator(s)) {
        return "logical_operator must be \"and\" or \"or\" if provided";
      }
    }

    if (data.containsKey("sort_by") && !StringUtils.isValidString(data.get("sort_by"))) {
      return "sort_by must be a string if provided";
    }

    if (data.containsKey("sort_order")) {
      Object sortOrder = data.get("sort_order");
      if (!(sortOrder instanceof String s) || !isFilterSortOrder(s)) {
        return "sort_order must be \"asc\" or \"desc\" if provided";
      }
    }

    if (data.containsKey("include_count") && !(data.get("include_count") instanceof Boolean)) {
      return "include_count must be a boolean if provided";
    }

    if (data.containsKey("limit")) {
      Object limit = data.get("limit");
      if (!numberIsInteger(limit)
          || ((Number) limit).doubleValue() < 1
          || ((Number) limit).doubleValue() > MAX_FILTER_LIMIT) {
        return "limit must be an integer between 1 and 100 if provided";
      }
    }

    // offset: 0 is valid — non-negative, not positive.
    if (data.containsKey("offset")) {
      Object offset = data.get("offset");
      if (!numberIsInteger(offset) || ((Number) offset).doubleValue() < 0) {
        return "offset must be a non-negative integer if provided";
      }
    }

    return null;
  }

  /**
   * Validates reindex options. Note the caller-side gate:
   * {@code Search.startReindex} only runs this when options were provided.
   */
  public static String validateStartReindexRequest(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Reindex options must be a valid object";
    }

    if (data.containsKey("batch_size")) {
      Object batchSize = data.get("batch_size");
      if (!numberIsInteger(batchSize) || ((Number) batchSize).doubleValue() < 1) {
        return "batch_size must be a positive integer if provided";
      }
    }

    return null;
  }

  /**
   * Validates one filter condition; {@code index} appears in messages as a
   * plain integer.
   */
  private static String validateFilterCondition(Object filterObject, int index) {
    // A null or non-map element is rejected.
    if (!(filterObject instanceof Map<?, ?> filter)) {
      return "filters[" + index + "] must be a valid object";
    }

    Object field = filter.get("field");
    if (!StringUtils.isValidString(field) || ((String) field).strip().isEmpty()) {
      return "filters[" + index + "].field must be a non-empty string";
    }

    Object operatorValue = filter.get("operator");
    if (!(operatorValue instanceof String operator) || !FILTER_OPERATORS.contains(operator)) {
      return "filters[" + index + "].operator must be a supported filter operator";
    }

    // `in` and `between` share one rule — a non-empty values array. `between`
    // does not require exactly two values, and `value` is never inspected for
    // these operators (unconditional return after the check).
    if (VALUES_ARRAY_OPERATORS.contains(operator)) {
      Object values = filter.get("values");
      if (!StringUtils.isValidArray(values) || arrayLength(values) == 0) {
        return "filters["
            + index
            + "].values must be a non-empty array for operator \""
            + operator
            + "\"";
      }
      return null;
    }

    if (VALUELESS_OPERATORS.contains(operator)) {
      return null;
    }

    // Presence check, not truthiness: 0, "", false, and NaN are all accepted
    // values. An absent key and an explicit null both read as null here and
    // are rejected.
    if (filter.get("value") == null) {
      return "filters[" + index + "].value is required for operator \"" + operator + "\"";
    }

    return null;
  }

  private static boolean isFilterLogicalOperator(String value) {
    return value.equals("and") || value.equals("or");
  }

  private static boolean isFilterSortOrder(String value) {
    return value.equals("asc") || value.equals("desc");
  }

  /**
   * True only for finite numbers with no fractional part; any non-Number
   * value fails.
   */
  private static boolean numberIsInteger(Object value) {
    if (!(value instanceof Number number)) {
      return false;
    }
    double d = number.doubleValue();
    return Double.isFinite(d) && d == Math.rint(d);
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
