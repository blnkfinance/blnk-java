package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link ApiKeyValidators}: create-key payload validation plus
 * the optional list and delete option checks.
 */
class ApiKeyValidatorsTest {

  /** Builds a fully valid create-API-key payload. */
  private static Map<String, Object> validData() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("name", "Service Account");
    data.put("owner", "merchant_a");
    data.put("scopes", List.of("ledgers:read"));
    data.put("expires_at", "2026-03-11T00:00:00Z");
    return data;
  }

  private static Map<String, Object> with(String key, Object value) {
    Map<String, Object> data = validData();
    data.put(key, value);
    return data;
  }

  @Test
  @DisplayName("ValidateCreateApiKeyData")
  void validateCreateApiKeyData() {
    assertNull(ApiKeyValidators.validateCreateApiKeyData(validData()));
    assertEquals(
        "name is required", ApiKeyValidators.validateCreateApiKeyData(with("name", "")));
    assertEquals(
        "owner is required", ApiKeyValidators.validateCreateApiKeyData(with("owner", "")));
    assertEquals(
        "at least one scope must be specified",
        ApiKeyValidators.validateCreateApiKeyData(with("scopes", List.of())));
    assertEquals(
        "each scope must be a non-empty string",
        ApiKeyValidators.validateCreateApiKeyData(
            with("scopes", Arrays.asList("ledgers:read", ""))));
    assertEquals(
        "expires_at must be a valid ISO 8601 datetime string",
        ApiKeyValidators.validateCreateApiKeyData(with("expires_at", "not-a-date")));
  }

  @Test
  @DisplayName("ValidateListApiKeysOptions")
  void validateListApiKeysOptions() {
    assertNull(ApiKeyValidators.validateListApiKeysOptions(null));
    assertNull(ApiKeyValidators.validateListApiKeysOptions(new LinkedHashMap<>()));
    Map<String, Object> owner = new LinkedHashMap<>();
    owner.put("owner", "merchant_a");
    assertNull(ApiKeyValidators.validateListApiKeysOptions(owner));
    Map<String, Object> emptyOwner = new LinkedHashMap<>();
    emptyOwner.put("owner", "");
    assertEquals(
        "owner must be a non-empty string",
        ApiKeyValidators.validateListApiKeysOptions(emptyOwner));
  }

  @Test
  @DisplayName("ValidateDeleteApiKeyOptions")
  void validateDeleteApiKeyOptions() {
    assertNull(ApiKeyValidators.validateDeleteApiKeyOptions(null));
    Map<String, Object> owner = new LinkedHashMap<>();
    owner.put("owner", "merchant_a");
    assertNull(ApiKeyValidators.validateDeleteApiKeyOptions(owner));
    Map<String, Object> emptyOwner = new LinkedHashMap<>();
    emptyOwner.put("owner", "");
    assertEquals(
        "owner must be a non-empty string",
        ApiKeyValidators.validateDeleteApiKeyOptions(emptyOwner));
  }
}
