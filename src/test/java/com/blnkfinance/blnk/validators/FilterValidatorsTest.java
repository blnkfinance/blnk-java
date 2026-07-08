package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for filter-parameter validation in {@link SearchValidators}. */
@DisplayName("Filter validators")
class FilterValidatorsTest {

  private static Map<String, Object> condition(String field, String operator) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("field", field);
    map.put("operator", operator);
    return map;
  }

  @Test
  @DisplayName("ValidateFilterParams accepts API-valid payload")
  void validateFilterParamsAcceptsApiValidPayload() {
    Map<String, Object> first = condition("status", "eq");
    first.put("value", "APPLIED");
    Map<String, Object> second = condition("currency", "in");
    second.put("values", List.of("USD", "EUR"));

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("filters", List.of(first, second));
    data.put("logical_operator", "and");
    data.put("sort_by", "created_at");
    data.put("sort_order", "desc");
    data.put("include_count", true);
    data.put("limit", 20);
    data.put("offset", 0);

    assertNull(SearchValidators.validateFilterParams(data));
  }

  @Test
  @DisplayName("ValidateFilterParams accepts valueless operators")
  void validateFilterParamsAcceptsValuelessOperators() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("filters", List.of(condition("identity_id", "isnull")));

    assertNull(SearchValidators.validateFilterParams(data));
  }

  @Test
  @DisplayName("ValidateFilterParams rejects missing values for in operator")
  void validateFilterParamsRejectsMissingValuesForInOperator() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("filters", List.of(condition("currency", "in")));

    assertEquals(
        "filters[0].values must be a non-empty array for operator \"in\"",
        SearchValidators.validateFilterParams(data));
  }

  @Test
  @DisplayName("ValidateFilterParams rejects invalid logical_operator")
  void validateFilterParamsRejectsInvalidLogicalOperator() {
    Map<String, Object> filter = condition("status", "eq");
    filter.put("value", "APPLIED");
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("filters", List.of(filter));
    data.put("logical_operator", "xor");

    assertEquals(
        "logical_operator must be \"and\" or \"or\" if provided",
        SearchValidators.validateFilterParams(data));
  }
}
