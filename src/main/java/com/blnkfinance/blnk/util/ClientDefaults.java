package com.blnkfinance.blnk.util;

/** Default values applied when the corresponding client option is absent or invalid. */
public final class ClientDefaults {

  private ClientDefaults() {}

  /** Default HTTP timeout in ms. */
  public static final double DEFAULT_TIMEOUT_MS = 10000;

  /** Total request attempts including the first. */
  public static final int DEFAULT_RETRY_COUNT = 1;

  /** Base delay between retry attempts in ms. */
  public static final double DEFAULT_RETRY_DELAY_MS = 2000;
}
