package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateHookData;
import com.blnkfinance.blnk.types.ListHooksOptions;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.HookValidators;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Endpoint service for webhook management. Note the URL-encoding asymmetry
 * with {@link ApiKeys}: Hooks NEVER encodes ids or the type query value —
 * both are placed in the endpoint as-is.
 */
public class Hooks {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Hooks(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /** Creates a hook — {@code POST hooks}; the payload is forwarded untouched. */
  public ApiResponse<JsonNode> create(CreateHookData data) {
    try {
      String validatorResponse =
          HookValidators.validateCreateHookData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("hooks", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /**
   * Lists hooks without filters — {@code GET hooks}. Core 0.15.3+ returns
   * every hook (PRE and POST) when {@code type} is omitted.
   */
  public ApiResponse<JsonNode> list() {
    return listInternal(null);
  }

  /**
   * Lists hooks, optionally filtered by type. A {@code null} argument is
   * treated as "no options" and behaves like {@link #list()}. The type value
   * is interpolated into the query string raw, without URL-encoding.
   */
  public ApiResponse<JsonNode> list(ListHooksOptions options) {
    return listInternal(options);
  }

  private ApiResponse<JsonNode> listInternal(ListHooksOptions options) {
    try {
      String validatorResponse =
          HookValidators.validateListHooksOptions(options == null ? null : options.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      String endpoint =
          options != null && options.toMap().containsKey("type")
              ? "hooks?type=" + options.toMap().get("type")
              : "hooks";

      return request.call(endpoint, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "list");
    }
  }

  /** Retrieves a hook — {@code GET hooks/{id}}; the id is NOT URL-encoded. */
  public ApiResponse<JsonNode> get(String id) {
    try {
      if (id == null || id.isEmpty()) {
        return formatResponse.format(400, "hook id is required", null, null);
      }

      return request.call("hooks/" + id, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get");
    }
  }

  /**
   * Updates a hook — {@code PUT hooks/{id}}. The id check runs BEFORE payload
   * validation. Update payloads share the {@link CreateHookData} shape used
   * by {@link #create}; note that the invalid-payload validation message
   * names CreateHookData — callers depend on this exact string.
   */
  public ApiResponse<JsonNode> update(String id, CreateHookData data) {
    try {
      if (id == null || id.isEmpty()) {
        return formatResponse.format(400, "hook id is required", null, null);
      }

      String validatorResponse =
          HookValidators.validateUpdateHookData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("hooks/" + id, data.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "update");
    }
  }

  /** Deletes a hook — {@code DELETE hooks/{id}}; the id is NOT URL-encoded. */
  public ApiResponse<JsonNode> delete(String id) {
    try {
      if (id == null || id.isEmpty()) {
        return formatResponse.format(400, "hook id is required", null, null);
      }

      return request.call("hooks/" + id, null, "DELETE", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "delete");
    }
  }
}
