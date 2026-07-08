package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for reindex-request validation in {@link SearchValidators}. */
@DisplayName("Start reindex validators")
class StartReindexValidatorsTest {

  @Test
  @DisplayName("ValidateStartReindexRequest accepts empty options")
  void validateStartReindexRequestAcceptsEmptyOptions() {
    assertNull(SearchValidators.validateStartReindexRequest(new LinkedHashMap<>()));
  }

  @Test
  @DisplayName("ValidateStartReindexRequest accepts positive batch_size")
  void validateStartReindexRequestAcceptsPositiveBatchSize() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("batch_size", 1000);
    assertNull(SearchValidators.validateStartReindexRequest(data));
  }

  @Test
  @DisplayName("ValidateStartReindexRequest rejects non-integer batch_size")
  void validateStartReindexRequestRejectsNonIntegerBatchSize() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("batch_size", 1.5);
    assertEquals(
        "batch_size must be a positive integer if provided",
        SearchValidators.validateStartReindexRequest(data));
  }
}
