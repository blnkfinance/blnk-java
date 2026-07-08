package com.blnkfinance.blnk;

/**
 * Logging hook used throughout the SDK. Implementations must supply
 * {@code info} and {@code error}; {@code debug} is optional and defaults to a
 * no-op.
 */
public interface BlnkLogger {

  void info(String message, Object... meta);

  void error(String message, Object... meta);

  default void debug(String message, Object... meta) {}
}
