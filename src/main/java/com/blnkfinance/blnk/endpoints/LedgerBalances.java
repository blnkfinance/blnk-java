package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateBalanceSnapshotRequest;
import com.blnkfinance.blnk.types.CreateLedgerBalance;
import com.blnkfinance.blnk.types.GetBalanceAtRequest;
import com.blnkfinance.blnk.types.GetBalanceRequest;
import com.blnkfinance.blnk.types.UpdateBalanceIdentity;
import com.blnkfinance.blnk.util.UriEncoding;
import com.blnkfinance.blnk.util.ValueFormat;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.LedgerBalanceValidators;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Endpoint service for ledger balances: creation, lookups (by id, by
 * indicator/currency, or at a point in time), identity updates, snapshots,
 * and lineage.
 *
 * <p>Every method wraps its whole body in try/catch, so runtime errors are
 * converted to an error response reported under the calling method's own
 * name; validation failures return {@code (400, message, null)} without any
 * HTTP call; no method passes header options.
 */
public class LedgerBalances {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public LedgerBalances(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Creates a ledger balance — {@code POST balances}. Validates, then
   * forwards {@code data} unmodified. Never throws: runtime errors are
   * converted to an error response, reported under the name {@code "create"}.
   */
  public ApiResponse<JsonNode> create(CreateLedgerBalance data) {
    try {
      String error =
          LedgerBalanceValidators.validateCreateLedgerBalance(data == null ? null : data.toMap());
      if (error != null) {
        return formatResponse.format(400, error, null, null);
      }

      return request.call("balances", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /** Retrieves a balance — {@code GET balances/{id}} with no options. */
  public ApiResponse<JsonNode> get(String id) {
    return get(id, null);
  }

  /**
   * Retrieves a balance — {@code GET balances/{id}}, plus the literal
   * {@code ?from_source=true} when {@code options.from_source} is set. Note:
   * the {@code id} is never validated ({@code get("")} performs
   * {@code GET balances/}) and is spliced into the path with NO URL-encoding.
   * Options validation runs only when options are provided ({@code null}
   * means "no options"). No body.
   */
  public ApiResponse<JsonNode> get(String id, GetBalanceRequest options) {
    try {
      Map<String, Object> optionsMap = options == null ? null : options.toMap();
      if (options != null) {
        String error = LedgerBalanceValidators.validateGetBalance(optionsMap);
        if (error != null) {
          return formatResponse.format(400, error, null, null);
        }
      }

      String endpoint = "balances/" + id;
      // Only a true from_source reaches this point (the validator forces a
      // boolean); false appends nothing.
      if (optionsMap != null && ValueFormat.isTruthy(optionsMap.get("from_source"))) {
        endpoint += "?from_source=true";
      }

      return request.call(endpoint, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get");
    }
  }

  /**
   * Retrieves a balance by indicator and currency —
   * {@code GET balances/indicator/{indicator}/currency/{currency}}. Both path
   * segments are URL-encoded — the only encoded path segments in this class.
   * No body.
   */
  public ApiResponse<JsonNode> getByIndicator(String indicator, String currency) {
    try {
      String error = LedgerBalanceValidators.validateGetByIndicator(indicator, currency);
      if (error != null) {
        return formatResponse.format(400, error, null, null);
      }

      return request.call(
          "balances/indicator/" + UriEncoding.encodePathSegment(indicator)
              + "/currency/" + UriEncoding.encodePathSegment(currency),
          null,
          "GET",
          null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getByIndicator");
    }
  }

  /**
   * Attaches an identity to a balance —
   * {@code PUT balances/{balanceId}/identity}. A null or empty
   * {@code balanceId} is rejected before payload validation; the data is then
   * forwarded unmodified. The {@code balanceId} is NOT URL-encoded.
   */
  public ApiResponse<JsonNode> updateIdentity(String balanceId, UpdateBalanceIdentity data) {
    try {
      if (balanceId == null || balanceId.isEmpty()) {
        return formatResponse.format(400, "balance id is required", null, null);
      }

      String error =
          LedgerBalanceValidators.validateUpdateBalanceIdentity(
              data == null ? null : data.toMap());
      if (error != null) {
        return formatResponse.format(400, error, null, null);
      }

      return request.call("balances/" + balanceId + "/identity", data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "updateIdentity");
    }
  }

  /** Takes a balance snapshot — {@code POST balances-snapshots} with no options. */
  public ApiResponse<JsonNode> createSnapshot() {
    return createSnapshot(null);
  }

  /**
   * Takes a balance snapshot — {@code POST balances-snapshots}, or
   * {@code balances-snapshots?batch_size={batch_size}} when
   * {@code options.batch_size} is present, non-zero, AND {@code > 0} (the
   * relational guard is intentionally kept alongside the presence check). The
   * request has NO body — batch_size travels only in the query string, with
   * whole values rendered without a fractional part. Options validation runs
   * only when options are provided ({@code null} means "no options").
   */
  public ApiResponse<JsonNode> createSnapshot(CreateBalanceSnapshotRequest options) {
    try {
      Map<String, Object> optionsMap = options == null ? null : options.toMap();
      if (options != null) {
        String error = LedgerBalanceValidators.validateCreateBalanceSnapshot(optionsMap);
        if (error != null) {
          return formatResponse.format(400, error, null, null);
        }
      }

      // batch_size must be present and non-zero, then positive — the second
      // guard is intentionally redundant with the first.
      Object batchSize = optionsMap == null ? null : optionsMap.get("batch_size");
      String endpoint =
          ValueFormat.isTruthy(batchSize) && ((Number) batchSize).doubleValue() > 0
              ? "balances-snapshots?batch_size=" + ValueFormat.formatNumber((Number) batchSize)
              : "balances-snapshots";

      return request.call(endpoint, null, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "createSnapshot");
    }
  }

  /**
   * Retrieves a balance at a point in time —
   * {@code GET balances/{balanceId}/at?timestamp={timestamp}}. A null or
   * empty {@code balanceId} is rejected first, then the options are validated
   * (they are NOT optional here); the timestamp string is URL-encoded
   * verbatim, and the literal {@code &from_source=true} is appended when
   * {@code options.from_source} is set. Note: from_source is never validated
   * here — any value {@code ValueFormat.isTruthy} accepts (e.g. the string
   * {@code "true"}) appends the flag. The {@code balanceId} is NOT
   * URL-encoded. No body.
   */
  public ApiResponse<JsonNode> getAt(String balanceId, GetBalanceAtRequest options) {
    try {
      if (balanceId == null || balanceId.isEmpty()) {
        return formatResponse.format(400, "balance id is required", null, null);
      }

      Map<String, Object> optionsMap = options == null ? null : options.toMap();
      String error = LedgerBalanceValidators.validateGetBalanceAt(optionsMap);
      if (error != null) {
        return formatResponse.format(400, error, null, null);
      }

      String endpoint =
          "balances/" + balanceId + "/at?timestamp="
              + UriEncoding.encodePathSegment((String) optionsMap.get("timestamp"));
      if (ValueFormat.isTruthy(optionsMap.get("from_source"))) {
        endpoint += "&from_source=true";
      }

      return request.call(endpoint, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getAt");
    }
  }

  /**
   * Retrieves a balance's lineage — {@code GET balances/{balanceId}/lineage}.
   * A null or empty {@code balanceId} is rejected; no validator, no body; the
   * {@code balanceId} is NOT URL-encoded.
   */
  public ApiResponse<JsonNode> getLineage(String balanceId) {
    try {
      if (balanceId == null || balanceId.isEmpty()) {
        return formatResponse.format(400, "balance id is required", null, null);
      }

      return request.call("balances/" + balanceId + "/lineage", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getLineage");
    }
  }
}
