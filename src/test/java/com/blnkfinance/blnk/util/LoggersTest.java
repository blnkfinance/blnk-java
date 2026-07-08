package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.testsupport.RecordingLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link Loggers#handleError}: sensitive values are redacted before logging. */
class LoggersTest {

  @Test
  @DisplayName("redacts sensitive values before logging")
  void redactsSensitiveValuesBeforeLogging() {
    RecordingLogger logger = new RecordingLogger();

    Loggers.handleError(
        new RuntimeException("failed with X-Blnk-Key: blnk_secret"),
        logger,
        HttpClientUtils.FORMAT_RESPONSE,
        "create");

    RecordingLogger.LogEntry first = logger.byLevel("error").get(0);
    assertEquals("create", first.message());
    Map<String, Object> expectedMeta = new LinkedHashMap<>();
    expectedMeta.put("name", "RuntimeException");
    expectedMeta.put("message", "failed with X-Blnk-Key: [REDACTED]");
    assertEquals(List.of(expectedMeta), first.meta());
  }
}
