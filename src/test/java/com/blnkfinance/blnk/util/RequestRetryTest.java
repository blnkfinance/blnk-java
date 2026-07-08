package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.BlnkAbortException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link RequestRetry}: retryability rules, backoff, and option clamping. */
class RequestRetryTest {

  @Test
  @DisplayName("isRetryableHttpMethod")
  void isRetryableHttpMethod() {
    assertTrue(RequestRetry.isRetryableHttpMethod("GET"));
    assertFalse(RequestRetry.isRetryableHttpMethod("POST"));
    assertFalse(RequestRetry.isRetryableHttpMethod("PUT"));
    assertFalse(RequestRetry.isRetryableHttpMethod("DELETE"));
  }

  @Test
  @DisplayName("isRetryableHttpStatus")
  void isRetryableHttpStatus() {
    assertFalse(RequestRetry.isRetryableHttpStatus(404));
    assertTrue(RequestRetry.isRetryableHttpStatus(500));
    assertTrue(RequestRetry.isRetryableHttpStatus(503));
  }

  @Test
  @DisplayName("isRetryableTransportError")
  void isRetryableTransportError() {
    // A caller-initiated abort is never retried.
    assertFalse(RequestRetry.isRetryableTransportError(new BlnkAbortException("aborted")));
    // A plain transport failure is retryable.
    assertTrue(RequestRetry.isRetryableTransportError(new RuntimeException("connection reset")));
    // Values that are not exceptions are never retryable.
    assertFalse(RequestRetry.isRetryableTransportError("connection reset"));
  }

  @Test
  @DisplayName("retryDelayForAttempt uses linear backoff")
  void retryDelayForAttemptLinearBackoff() {
    assertEquals(2000, RequestRetry.retryDelayForAttempt(1, 2000));
    assertEquals(4000, RequestRetry.retryDelayForAttempt(2, 2000));
  }

  @Test
  @DisplayName("normalizeRetryCount clamps invalid values to 1")
  void normalizeRetryCountClamps() {
    assertEquals(1, RequestRetry.normalizeRetryCount(null));
    assertEquals(1, RequestRetry.normalizeRetryCount(0.0));
    assertEquals(1, RequestRetry.normalizeRetryCount(-1.0));
    assertEquals(1, RequestRetry.normalizeRetryCount(Double.NaN));
    assertEquals(1, RequestRetry.normalizeRetryCount(Double.POSITIVE_INFINITY));
    assertEquals(1, RequestRetry.normalizeRetryCount(Double.NEGATIVE_INFINITY));
    assertEquals(2, RequestRetry.normalizeRetryCount(2.9));
    assertEquals(3, RequestRetry.normalizeRetryCount(3.0));
  }

  @Test
  @DisplayName("normalizeRetryDelayMs clamps invalid values to default")
  void normalizeRetryDelayMsClamps() {
    assertEquals(2000, RequestRetry.normalizeRetryDelayMs(null));
    assertEquals(2000, RequestRetry.normalizeRetryDelayMs(-1.0));
    assertEquals(2000, RequestRetry.normalizeRetryDelayMs(Double.NaN));
    assertEquals(2000, RequestRetry.normalizeRetryDelayMs(Double.POSITIVE_INFINITY));
    assertEquals(2000, RequestRetry.normalizeRetryDelayMs(Double.NEGATIVE_INFINITY));
    assertEquals(500, RequestRetry.normalizeRetryDelayMs(500.0));
  }
}
