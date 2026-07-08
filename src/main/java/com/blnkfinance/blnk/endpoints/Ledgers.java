package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateLedger;
import com.blnkfinance.blnk.types.UpdateLedger;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.LedgerValidators;
import com.fasterxml.jackson.databind.JsonNode;

/** Endpoint service for ledgers: create, retrieve, and update. */
public class Ledgers {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Ledgers(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Creates a ledger — {@code POST ledgers}. Validates, then forwards
   * {@code data} unmodified. Never throws: runtime errors are converted to an
   * error response, reported under the name {@code "create"}.
   */
  public ApiResponse<JsonNode> create(CreateLedger data) {
    try {
      String error =
          LedgerValidators.validateCreateLedger(data == null ? null : data.toMap());
      if (error != null) {
        return formatResponse.format(400, error, null, null);
      }
      return request.call("ledgers", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /**
   * Retrieves a ledger — {@code GET ledgers/{id}}, no body. Note: unlike the
   * other methods, there is no id validation and NO try/catch here — an error
   * thrown by the request layer PROPAGATES to the caller.
   */
  public ApiResponse<JsonNode> get(String id) {
    return request.call("ledgers/" + id, null, "GET", null);
  }

  /**
   * Updates a ledger — {@code PUT ledgers/{id}}. A null or empty id is
   * rejected first, then the data is validated and forwarded unmodified.
   * Never throws: runtime errors are converted to an error response, reported
   * under the name {@code "update"}.
   */
  public ApiResponse<JsonNode> update(String id, UpdateLedger data) {
    try {
      if (id == null || id.isEmpty()) {
        return formatResponse.format(400, "ledger id is required", null, null);
      }

      String error =
          LedgerValidators.validateUpdateLedger(data == null ? null : data.toMap());
      if (error != null) {
        return formatResponse.format(400, error, null, null);
      }

      return request.call("ledgers/" + id, data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "update");
    }
  }
}
