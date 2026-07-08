package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.BlnkAbortException;

/**
 * Retry policy helpers: which methods, statuses, and errors are retryable,
 * plus normalization of the retry-related client options.
 */
public final class RequestRetry {

  private RequestRetry() {}

  /** Blocks for the given delay; non-positive delays return immediately. */
  public static void sleep(long ms) {
    if (ms <= 0) {
      return;
    }
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("sleep interrupted", e);
    }
  }

  /** Only GET requests are retryable. */
  public static boolean isRetryableHttpMethod(String method) {
    return "GET".equals(method);
  }

  /** {@code status >= 500}. */
  public static boolean isRetryableHttpStatus(int status) {
    return status >= 500;
  }

  /**
   * Absent (null), non-finite (NaN/±Infinity), or {@code < 1} → 1
   * (DEFAULT_RETRY_COUNT); otherwise the value floored (2.9 → 2).
   */
  public static int normalizeRetryCount(Double retryCount) {
    if (retryCount == null || !Double.isFinite(retryCount) || retryCount < 1) {
      return ClientDefaults.DEFAULT_RETRY_COUNT;
    }
    return (int) Math.floor(retryCount);
  }

  /**
   * Absent (null), non-finite, or {@code < 0} → 2000
   * (DEFAULT_RETRY_DELAY_MS); otherwise the value unchanged — fractional
   * delays are kept and a delay of 0 is deliberately allowed.
   */
  public static double normalizeRetryDelayMs(Double retryDelayMs) {
    if (retryDelayMs == null || !Double.isFinite(retryDelayMs) || retryDelayMs < 0) {
      return ClientDefaults.DEFAULT_RETRY_DELAY_MS;
    }
    return retryDelayMs;
  }

  /**
   * False when the value is not a Throwable; false for
   * {@link BlnkAbortException} (timeouts are never retried); true for every
   * other Throwable.
   */
  public static boolean isRetryableTransportError(Object error) {
    if (!(error instanceof Throwable)) {
      return false;
    }
    return !(error instanceof BlnkAbortException);
  }

  /** {@code baseDelayMs * attempt} — linear backoff, deliberately with no jitter and no cap. */
  public static double retryDelayForAttempt(int attempt, double baseDelayMs) {
    return baseDelayMs * attempt;
  }
}
