package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.types.ListOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for {@link ListValidators#validateListOptions}. */
class ListValidatorsTest {

  @Test
  @DisplayName("accepts empty options")
  void acceptsEmptyOptions() {
    assertNull(ListValidators.validateListOptions(Map.of()));
  }

  @Test
  @DisplayName("accepts limit 1 and offset 0, the smallest values Core allows")
  void acceptsBoundaryValues() {
    assertNull(ListValidators.validateListOptions(ListOptions.create().limit(1).offset(0).toMap()));
  }

  @Test
  @DisplayName("rejects null payload")
  void rejectsNullPayload() {
    assertEquals(
        "Data must be a valid object of type ListOptions", ListValidators.validateListOptions(null));
  }

  @Test
  @DisplayName("rejects limit below 1")
  void rejectsLimitBelowOne() {
    assertEquals(
        "limit must be at least 1",
        ListValidators.validateListOptions(ListOptions.create().limit(0).toMap()));
  }

  @Test
  @DisplayName("rejects non-integer limit")
  void rejectsNonIntegerLimit() {
    Map<String, Object> decimal = new java.util.LinkedHashMap<>();
    decimal.put("limit", 2.5);
    assertEquals("limit must be an integer if provided", ListValidators.validateListOptions(decimal));

    Map<String, Object> missing = new java.util.LinkedHashMap<>();
    missing.put("limit", null);
    assertEquals("limit must be an integer if provided", ListValidators.validateListOptions(missing));
  }

  @Test
  @DisplayName("rejects negative offset")
  void rejectsNegativeOffset() {
    assertEquals(
        "offset must be at least 0",
        ListValidators.validateListOptions(ListOptions.create().offset(-5).toMap()));
  }

  @Test
  @DisplayName("rejects non-integer offset")
  void rejectsNonIntegerOffset() {
    Map<String, Object> asString = new java.util.LinkedHashMap<>();
    asString.put("offset", "0");
    assertEquals(
        "offset must be an integer if provided", ListValidators.validateListOptions(asString));
  }

  @Test
  @DisplayName("limit is checked before offset")
  void limitCheckedFirst() {
    assertEquals(
        "limit must be at least 1",
        ListValidators.validateListOptions(ListOptions.create().limit(0).offset(-1).toMap()));
  }

  @Test
  @DisplayName("rejects unknown query keys instead of dropping them")
  void rejectsUnknownQueryKeys() {
    Map<String, Object> misspelled = new java.util.LinkedHashMap<>();
    misspelled.put("limt", 50);
    assertEquals("unsupported list option: limt", ListValidators.validateListOptions(misspelled));

    Map<String, Object> page = new java.util.LinkedHashMap<>();
    page.put("limit", 10);
    page.put("page", 2);
    assertEquals("unsupported list option: page", ListValidators.validateListOptions(page));
  }
}
