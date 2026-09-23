package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.util.UriEncoding;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.BalanceMonitorValidators;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Endpoint service for balance monitors: create, retrieve, list, update, and
 * delete monitors on Blnk Core.
 *
 * <p>Note: {@code get} and {@code update} interpolate the id raw (no
 * validation, no encoding; an empty id is allowed). {@code delete} and
 * {@code listByBalanceId} validate and percent-encode their path ids.
 */
public class BalanceMonitor {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public BalanceMonitor(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Creates a balance monitor — {@code POST balance-monitors}. Validates,
   * then forwards {@code data} unmodified. Never throws: runtime errors are
   * converted to an error response, reported under the name {@code "create"}.
   */
  public ApiResponse<JsonNode> create(MonitorData data) {
    try {
      String validatorResponse =
          BalanceMonitorValidators.validateMonitorData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call("balance-monitors", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /**
   * Retrieves a balance monitor — {@code GET balance-monitors/{id}}. The id is
   * interpolated raw, with no validation and no URL-encoding; no body. Never
   * throws: runtime errors are converted to an error response, reported under
   * the name {@code "get"}.
   */
  public ApiResponse<JsonNode> get(String id) {
    try {
      return request.call("balance-monitors/" + id, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get");
    }
  }

  /**
   * Lists balance monitors — {@code GET balance-monitors}, no body. Note:
   * runtime errors here are reported under the name {@code "get"}, not
   * {@code "list"}; callers depend on this exact value.
   */
  public ApiResponse<JsonNode> list() {
    try {
      return request.call("balance-monitors", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get"); // intentionally "get"
    }
  }

  /**
   * Lists monitors for one balance — {@code GET
   * balance-monitors/balances/{balance_id}}. Distinct from {@link #list()},
   * which still returns every monitor. The balance id is percent-encoded so
   * {@code /}, {@code ?}, and {@code #} cannot alter the path. Empty or
   * whitespace-only ids return {@code 400} without a request. This method
   * takes no query options, so unknown list keys are N/A.
   */
  public ApiResponse<JsonNode> listByBalanceId(String balanceId) {
    try {
      if (balanceId == null || balanceId.isBlank()) {
        return formatResponse.format(400, "balance id is required", null, null);
      }
      return request.call(
          "balance-monitors/balances/" + UriEncoding.encodePathSegment(balanceId),
          null,
          "GET",
          null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "listByBalanceId");
    }
  }

  /**
   * Updates a balance monitor — {@code PUT balance-monitors/{id}}. Validates
   * the data only (the id is neither validated nor URL-encoded), then forwards
   * {@code data} unmodified. Never throws: runtime errors are converted to an
   * error response, reported under the name {@code "update"}.
   */
  public ApiResponse<JsonNode> update(String id, MonitorData data) {
    try {
      String validatorResponse =
          BalanceMonitorValidators.validateMonitorData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call("balance-monitors/" + id, data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "update");
    }
  }

  /**
   * Deletes a balance monitor — {@code DELETE balance-monitors/{id}} with the
   * id URL-encoded. Validates its id ({@code monitor id is required}) and
   * encodes it. No body. Never throws: runtime errors are converted to an
   * error response, reported under the name {@code "delete"}.
   */
  public ApiResponse<JsonNode> delete(String id) {
    try {
      String validatorResponse = BalanceMonitorValidators.validateMonitorId(id);
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(
          "balance-monitors/" + UriEncoding.encodePathSegment(id), null, "DELETE", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "delete");
    }
  }
}
