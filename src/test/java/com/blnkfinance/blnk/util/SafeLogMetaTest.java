package com.blnkfinance.blnk.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link SafeLogMeta}: redaction of sensitive strings, keys, and exceptions. */
class SafeLogMetaTest {

  @Test
  @DisplayName("redacts auth header values in strings")
  void redactsAuthHeaderValuesInStrings() {
    String input = "Request failed with X-Blnk-Key: blnk_local_dev_secret";
    assertEquals(
        "Request failed with X-Blnk-Key: [REDACTED]",
        SafeLogMeta.redactSensitiveString(input));
  }

  @Test
  @DisplayName("redacts sensitive object keys")
  void redactsSensitiveObjectKeys() {
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("X-Blnk-Key", "blnk_secret");
    headers.put("content-type", "application/json");
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("endpoint", "health");
    meta.put("apiKey", "blnk_secret");
    meta.put("headers", headers);

    Map<String, Object> redacted = SafeLogMeta.redactSensitiveLogMeta(meta);

    assertEquals("[REDACTED]", redacted.get("apiKey"));
    Map<String, Object> expectedHeaders = new LinkedHashMap<>();
    expectedHeaders.put("X-Blnk-Key", "[REDACTED]");
    expectedHeaders.put("content-type", "application/json");
    assertEquals(expectedHeaders, redacted.get("headers"));
  }

  @Test
  @DisplayName("redacts Error objects to name and message only")
  void redactsErrorObjects() {
    Object redacted = SafeLogMeta.redactSensitiveLogValue(
        new RuntimeException("request failed with X-Blnk-Key: blnk_secret"));

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("name", "RuntimeException"); // name carries the exception's simple class name
    expected.put("message", "request failed with X-Blnk-Key: [REDACTED]");
    assertEquals(expected, redacted);
  }

  @Test
  @DisplayName("safeLogMeta sanitizes mixed metadata")
  void safeLogMetaSanitizesMixedMetadata() {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("endpoint", "transactions");
    meta.put("error", new RuntimeException("network error"));
    meta.put("authorization", "Bearer abc123");

    Object errorMeta = SafeLogMeta.safeLogMeta(meta)[0];

    Map<String, Object> expectedError = new LinkedHashMap<>();
    expectedError.put("name", "RuntimeException");
    expectedError.put("message", "network error");
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("endpoint", "transactions");
    expected.put("error", expectedError);
    // Key-level redaction wins before the string regexes run.
    expected.put("authorization", "[REDACTED]");
    assertEquals(expected, errorMeta);
  }
}
