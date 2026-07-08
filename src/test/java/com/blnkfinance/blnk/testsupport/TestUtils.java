package com.blnkfinance.blnk.testsupport;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared helpers for the integration and end-to-end suites.
 */
public final class TestUtils {

  private TestUtils() {}

  /**
   * Base URL of the live Blnk Core instance used by the integration/e2e suites
   * ({@code BLNK_BASE_URL} env override; defaults to a local instance).
   */
  public static final String BASE_URL =
      System.getenv("BLNK_BASE_URL") != null
          ? System.getenv("BLNK_BASE_URL")
          : "http://localhost:5001/";

  /** Integration API key ({@code BLNK_API_KEY} env override; defaults to the local dev secret). */
  public static final String BLNK_API_KEY =
      System.getenv("BLNK_API_KEY") != null
          ? System.getenv("BLNK_API_KEY")
          : "blnk-local-dev-secret-change-me";

  /** Returns {@code prefix}, a dash, then {@code count} random digits. */
  public static String generateRandomNumbersWithPrefix(String prefix, int count) {
    StringBuilder digits = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      digits.append(ThreadLocalRandom.current().nextInt(10));
    }
    return prefix + "-" + digits;
  }

  /** Sleeps for the given number of seconds. */
  public static void sleepSeconds(double seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("sleep interrupted", e);
    }
  }
}
