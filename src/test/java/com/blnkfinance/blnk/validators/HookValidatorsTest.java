package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for {@link HookValidators}: create, list, and update option checks. */
class HookValidatorsTest {

  /** Builds a fully valid create-hook payload shared across cases. */
  private static Map<String, Object> validData() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("name", "Pre-transaction validation");
    data.put("url", "https://api.example.com/validate");
    data.put("type", "PRE_TRANSACTION");
    data.put("active", true);
    data.put("timeout", 30);
    data.put("retry_count", 3);
    return data;
  }

  @Nested
  @DisplayName("ValidateCreateHookData")
  class ValidateCreateHookData {

    @Test
    @DisplayName("accepts valid payload")
    void acceptsValidPayload() {
      assertNull(HookValidators.validateCreateHookData(validData()));
    }

    @Test
    @DisplayName("rejects empty name")
    void rejectsEmptyName() {
      Map<String, Object> data = validData();
      data.put("name", "");
      assertEquals("name is required", HookValidators.validateCreateHookData(data));
    }

    @Test
    @DisplayName("rejects empty url")
    void rejectsEmptyUrl() {
      Map<String, Object> data = validData();
      data.put("url", "");
      assertEquals("url is required", HookValidators.validateCreateHookData(data));
    }

    @Test
    @DisplayName("rejects invalid type")
    void rejectsInvalidType() {
      Map<String, Object> data = validData();
      data.put("type", "INVALID");
      assertEquals(
          "type must be PRE_TRANSACTION or POST_TRANSACTION",
          HookValidators.validateCreateHookData(data));
    }

    @Test
    @DisplayName("rejects non-boolean active")
    void rejectsNonBooleanActive() {
      Map<String, Object> data = validData();
      data.put("active", "true"); // string, not boolean
      assertEquals("active must be a boolean", HookValidators.validateCreateHookData(data));
    }

    @Test
    @DisplayName("rejects non-positive timeout")
    void rejectsNonPositiveTimeout() {
      Map<String, Object> data = validData();
      data.put("timeout", 0);
      assertEquals(
          "timeout must be a positive number", HookValidators.validateCreateHookData(data));
    }

    @Test
    @DisplayName("rejects negative retry_count")
    void rejectsNegativeRetryCount() {
      Map<String, Object> data = validData();
      data.put("retry_count", -1);
      assertEquals(
          "retry_count must be a non-negative number",
          HookValidators.validateCreateHookData(data));
    }
  }

  @Nested
  @DisplayName("ValidateListHooksOptions")
  class ValidateListHooksOptions {

    @Test
    @DisplayName("accepts undefined options")
    void acceptsUndefinedOptions() {
      assertNull(HookValidators.validateListHooksOptions(null));
    }

    @Test
    @DisplayName("accepts empty options")
    void acceptsEmptyOptions() {
      assertNull(HookValidators.validateListHooksOptions(new LinkedHashMap<>()));
    }

    @Test
    @DisplayName("accepts valid type")
    void acceptsValidType() {
      Map<String, Object> options = new LinkedHashMap<>();
      options.put("type", "PRE_TRANSACTION");
      assertNull(HookValidators.validateListHooksOptions(options));
    }

    @Test
    @DisplayName("rejects invalid type")
    void rejectsInvalidType() {
      Map<String, Object> options = new LinkedHashMap<>();
      options.put("type", "INVALID");
      assertEquals(
          "type must be PRE_TRANSACTION or POST_TRANSACTION",
          HookValidators.validateListHooksOptions(options));
    }
  }

  @Nested
  @DisplayName("ValidateUpdateHookData")
  class ValidateUpdateHookData {

    @Test
    @DisplayName("accepts valid payload")
    void acceptsValidPayload() {
      assertNull(HookValidators.validateUpdateHookData(validData()));
    }

    @Test
    @DisplayName("rejects invalid type")
    void rejectsInvalidType() {
      Map<String, Object> data = validData();
      data.put("type", "INVALID");
      assertEquals(
          "type must be PRE_TRANSACTION or POST_TRANSACTION",
          HookValidators.validateUpdateHookData(data));
    }
  }
}
