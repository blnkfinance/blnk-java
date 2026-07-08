package com.blnkfinance.blnk.types;

/**
 * Batch-size limits enforced by the transaction validators.
 */
public final class TransactionConstants {

  private TransactionConstants() {}

  /** Maximum items per bulk commit or bulk void inflight request. */
  public static final int MAX_BULK_INFLIGHT_ITEMS = 100;

  /** Maximum items per {@code POST transactions/bulk} request (Core 0.15.0). */
  public static final int MAX_BULK_CREATE_ITEMS = 10000;
}
