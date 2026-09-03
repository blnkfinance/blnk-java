package com.blnkfinance.blnk.types;

/**
 * Stable Core {@code error_detail.code} values the SDK callers should
 * recognize. The HTTP layer still surfaces whatever Core returns via
 * {@link BlnkApiErrorDetail#code()}.
 */
public final class BlnkErrorCodes {

  private BlnkErrorCodes() {}

  /**
   * Negative {@code amount}, {@code precise_amount}, or {@code precision}.
   * Core 0.15.3 currently reports those as {@link #TXN_VALIDATION_ERROR};
   * keep this code so callers can match either.
   */
  public static final String TXN_INVALID_AMOUNT = "TXN_INVALID_AMOUNT";

  /** Duplicate internal-balance indicator + currency. */
  public static final String GEN_CONFLICT = "GEN_CONFLICT";

  /** Payload failed Core request validation (for example missing amount). */
  public static final String TXN_VALIDATION_ERROR = "TXN_VALIDATION_ERROR";
}
