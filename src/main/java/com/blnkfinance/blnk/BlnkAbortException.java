package com.blnkfinance.blnk;

/**
 * Signals that a transport call was aborted or timed out. Any transport call
 * that throws this (or is timed out by the client) takes the synthetic-408
 * path and is reported as a timeout. Timeouts are never retried.
 */
public class BlnkAbortException extends RuntimeException {

  public BlnkAbortException(String message) {
    super(message);
  }

  public BlnkAbortException(String message, Throwable cause) {
    super(message, cause);
  }
}
