package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.util.Loggers;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * System operations for Blnk Core (health checks, etc.). The class is named
 * {@code BlnkSystem} to avoid clashing with {@code java.lang.System}; it
 * fronts Blnk's "System" service.
 */
public class BlnkSystem {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public BlnkSystem(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Checks whether Blnk Core is running and reachable — {@code GET health}
   * (Core responds with {@code { status: "UP" }} on {@code data}). No body,
   * no validation. Never throws: runtime errors are converted to an error
   * response, reported under the name {@code "health"}.
   */
  public ApiResponse<JsonNode> health() {
    try {
      return request.call("health", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "health");
    }
  }
}
