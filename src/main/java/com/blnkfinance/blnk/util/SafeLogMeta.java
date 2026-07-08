package com.blnkfinance.blnk.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Redaction helpers that mask API keys, bearer tokens, and other secrets in
 * log metadata before it reaches any logger. Always returns NEW structures,
 * never mutates the input.
 *
 * <p>The redaction patterns use the default ASCII-only {@code \s}; secrets
 * are expected to be delimited by ordinary whitespace.
 */
public final class SafeLogMeta {

  private SafeLogMeta() {}

  private static final Set<String> SENSITIVE_KEYS =
      Set.of("apikey", "api_key", "x-blnk-key", "authorization", "cookie", "password", "secret",
          "token");

  private static final Pattern BLNK_KEY_PATTERN =
      Pattern.compile("X-Blnk-Key:\\s*[^\\s,;]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern BEARER_PATTERN =
      Pattern.compile("Bearer\\s+[^\\s,;]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern API_KEY_PATTERN =
      Pattern.compile("(api[_-]?key[\"']?\\s*[:=]\\s*[\"']?)[^\"'\\s,}]+",
          Pattern.CASE_INSENSITIVE);

  public static String redactSensitiveString(String value) {
    String result = BLNK_KEY_PATTERN.matcher(value).replaceAll("X-Blnk-Key: [REDACTED]");
    result = BEARER_PATTERN.matcher(result).replaceAll("Bearer [REDACTED]");
    result = API_KEY_PATTERN.matcher(result).replaceAll("$1[REDACTED]");
    return result;
  }

  /**
   * null → as-is; String → redactSensitiveString; Throwable →
   * {@code {name, message}} map (name = the exception's simple class name;
   * the stack trace and everything else are dropped); List/array →
   * element-wise recurse; Map → redactSensitiveLogMeta; other values as-is.
   */
  public static Object redactSensitiveLogValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String s) {
      return redactSensitiveString(s);
    }
    if (value instanceof Throwable t) {
      Map<String, Object> redacted = new LinkedHashMap<>();
      redacted.put("name", t.getClass().getSimpleName());
      redacted.put("message", t.getMessage() == null ? null : redactSensitiveString(t.getMessage()));
      return redacted;
    }
    if (value instanceof List<?> list) {
      List<Object> redacted = new ArrayList<>(list.size());
      for (Object item : list) {
        redacted.add(redactSensitiveLogValue(item));
      }
      return redacted;
    }
    if (value instanceof Object[] array) {
      List<Object> redacted = new ArrayList<>(array.length);
      for (Object item : array) {
        redacted.add(redactSensitiveLogValue(item));
      }
      return redacted;
    }
    if (value instanceof Map<?, ?> map) {
      return redactSensitiveLogMeta(map);
    }
    return value;
  }

  /**
   * Per entry: sensitive key (lowercased compare) → {@code [REDACTED]};
   * key {@code headers} (case-insensitive) → recurse as meta map; else
   * redactSensitiveLogValue. New map, key order preserved.
   */
  public static Map<String, Object> redactSensitiveLogMeta(Map<?, ?> meta) {
    Map<String, Object> redacted = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : meta.entrySet()) {
      String key = String.valueOf(entry.getKey());
      String lower = key.toLowerCase();
      if (SENSITIVE_KEYS.contains(lower)) {
        redacted.put(key, "[REDACTED]");
        continue;
      }
      if (lower.equals("headers") && entry.getValue() instanceof Map<?, ?> headers) {
        redacted.put(key, redactSensitiveLogMeta(headers));
        continue;
      }
      redacted.put(key, redactSensitiveLogValue(entry.getValue()));
    }
    return redacted;
  }

  /** Redacts each element via {@link #redactSensitiveLogValue}; returns a new array. */
  public static Object[] safeLogMeta(Object... meta) {
    Object[] redacted = new Object[meta.length];
    for (int i = 0; i < meta.length; i++) {
      redacted[i] = redactSensitiveLogValue(meta[i]);
    }
    return redacted;
  }
}
