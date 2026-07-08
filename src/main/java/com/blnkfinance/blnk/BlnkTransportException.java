package com.blnkfinance.blnk;

/**
 * Transport-level failure (network error, DNS, connection reset, …) raised
 * before any HTTP response was received. Retryable for GET requests.
 */
public class BlnkTransportException extends RuntimeException {

  public BlnkTransportException(String message) {
    super(message);
  }

  public BlnkTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
