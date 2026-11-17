package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.types.UpdateMonitorData;
import com.blnkfinance.blnk.util.PathSegments;
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
   * validated and URL-encoded; an empty id returns {@code 400}; no body. Never
   * throws: runtime errors are converted to an error response, reported under
   * the name {@code "get"}.
   */
  public ApiResponse<JsonNode> get(String id) {
    try {
      return request.call("balance-monitors/" + PathSegments.segment(id), null, "GET", null);
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
          "balance-monitors/balances/" + PathSegments.segment(balanceId),
          null,
          "GET",
          null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "listByBalanceId");
    }
  }

  /**
   * Updates a balance monitor — {@code PUT balance-monitors/{id}}. Validates
   * the data, then validates and URL-encodes the id, then forwards
   * {@code data} unmodified. Never throws: runtime errors are converted to an
   * error response, reported under the name {@code "update"}.
   *
   * @deprecated This requires a full {@link MonitorData}, so every change
   *     resends the whole monitor, and Core ignores the {@code call_back_url}
   *     it carries. Core 0.15.9+ keeps any field a {@code PUT} leaves out: use
   *     {@link #updatePartial(String, UpdateMonitorData)}, which sends only the
   *     fields you set. This method keeps working.
   */
  @Deprecated
  public ApiResponse<JsonNode> update(String id, MonitorData data) {
    try {
      String validatorResponse =
          BalanceMonitorValidators.validateMonitorData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call(
          "balance-monitors/" + PathSegments.segment(id), data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "update");
    }
  }

  /**
   * Partially updates a balance monitor — {@code PUT balance-monitors/{id}}
   * with only the fields set on {@code data} (Core 0.15.9+). Fields left
   * unset keep their stored values. Validates the id and the body, then
   * sends the body unmodified.
   *
   * <p>Core replies with {@code {"message": ...}}, plus
   * {@code "precision_unchanged": true} when a precision was sent without a
   * value and Core kept the stored precision.
   *
   * <p>Never throws: runtime errors are converted to an error response,
   * reported under the name {@code "updatePartial"}.
   */
  public ApiResponse<JsonNode> updatePartial(String id, UpdateMonitorData data) {
    try {
      String idError = BalanceMonitorValidators.validateMonitorId(id);
      if (idError != null) {
        return formatResponse.format(400, idError, null, null);
      }
      String validatorResponse =
          BalanceMonitorValidators.validateUpdateMonitorData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call(
          "balance-monitors/" + PathSegments.segment(id), data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "updatePartial");
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
          "balance-monitors/" + PathSegments.segment(id), null, "DELETE", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "delete");
    }
  }
}
