package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Logging helpers: the plain fallback logger and the shared error-to-response
 * handler.
 */
public final class Loggers {

  private Loggers() {}

  /**
   * The plain, prefix-free stdout/stderr logger used when a {@code Blnk} is
   * constructed directly with no logger.
   */
  public static final BlnkLogger CONSOLE = new BlnkLogger() {
    @Override
    public void info(String message, Object... meta) {
      System.out.println(message + renderMeta(meta));
    }

    @Override
    public void error(String message, Object... meta) {
      System.err.println(message + renderMeta(meta));
    }

    @Override
    public void debug(String message, Object... meta) {
      System.out.println(message + renderMeta(meta));
    }
  };

  /**
   * Converts a caught failure into a terminal 500 response:
   * <ol>
   *   <li>{@code logger.error(fnName, ...safeLogMeta(error))};</li>
   *   <li>Throwable → {@code (500, error message, null)};</li>
   *   <li>else → {@code (500, "An unknown error occurred.", null)}.</li>
   * </ol>
   * Never throws.
   */
  public static ApiResponse<JsonNode> handleError(
      Object error, BlnkLogger logger, FormatResponseFn formatResponse, String fnName) {
    logger.error(fnName, SafeLogMeta.safeLogMeta(error));
    if (error instanceof Throwable t) {
      return formatResponse.format(500, t.getMessage(), null, null);
    }
    return formatResponse.format(500, "An unknown error occurred.", null, null);
  }

  static String renderMeta(Object... meta) {
    if (meta == null || meta.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Object m : meta) {
      sb.append(' ').append(m);
    }
    return sb.toString();
  }
}
