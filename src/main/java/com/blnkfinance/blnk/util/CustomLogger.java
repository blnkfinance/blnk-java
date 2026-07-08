package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.BlnkLogger;

/**
 * The default logger injected by {@code Blnk.init}. Prefixes are exact:
 * {@code INFO: } to stdout, {@code ERROR: } to stderr, {@code DEBUG: } to
 * stdout.
 */
public final class CustomLogger implements BlnkLogger {

  @Override
  public void info(String message, Object... meta) {
    System.out.println("INFO: " + message + Loggers.renderMeta(meta));
  }

  @Override
  public void error(String message, Object... meta) {
    System.err.println("ERROR: " + message + Loggers.renderMeta(meta));
  }

  @Override
  public void debug(String message, Object... meta) {
    System.out.println("DEBUG: " + message + Loggers.renderMeta(meta));
  }
}
