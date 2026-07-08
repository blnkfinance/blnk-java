package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateApiKeyData;
import com.blnkfinance.blnk.types.DeleteApiKeyOptions;
import com.blnkfinance.blnk.types.ListApiKeysOptions;
import com.blnkfinance.blnk.util.UriEncoding;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.ApiKeyValidators;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Endpoint service for API key management. Note the URL-encoding asymmetry
 * with {@link Hooks}: here BOTH the id path segment and the owner query value
 * are URL-encoded before being placed in the endpoint.
 */
public class ApiKeys {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public ApiKeys(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /** Creates an API key — {@code POST api-keys}; the payload is forwarded untouched. */
  public ApiResponse<JsonNode> create(CreateApiKeyData data) {
    try {
      String validatorResponse =
          ApiKeyValidators.validateCreateApiKeyData(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call("api-keys", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /** Lists API keys without filters — {@code GET api-keys}. */
  public ApiResponse<JsonNode> list() {
    return listInternal(null);
  }

  /**
   * Lists API keys, optionally filtered by owner. A {@code null} argument is
   * treated as "no options" and behaves like {@link #list()}. The owner query
   * value is URL-encoded.
   */
  public ApiResponse<JsonNode> list(ListApiKeysOptions options) {
    return listInternal(options);
  }

  private ApiResponse<JsonNode> listInternal(ListApiKeysOptions options) {
    try {
      String validatorResponse =
          ApiKeyValidators.validateListApiKeysOptions(options == null ? null : options.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      String endpoint =
          options != null && options.toMap().containsKey("owner")
              ? "api-keys?owner="
                  + UriEncoding.encodePathSegment((String) options.toMap().get("owner"))
              : "api-keys";

      return request.call(endpoint, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "list");
    }
  }

  /** Deletes an API key by id with no owner filter. */
  public ApiResponse<JsonNode> delete(String id) {
    return deleteInternal(id, null);
  }

  /**
   * Deletes an API key — {@code DELETE api-keys/{id}}. The id check runs
   * FIRST, before options validation; both the id and the owner query value
   * are URL-encoded. A successful delete resolves with {@code null} data.
   */
  public ApiResponse<JsonNode> delete(String id, DeleteApiKeyOptions options) {
    return deleteInternal(id, options);
  }

  private ApiResponse<JsonNode> deleteInternal(String id, DeleteApiKeyOptions options) {
    try {
      if (id == null || id.isEmpty()) {
        return formatResponse.format(400, "api key id is required", null, null);
      }

      String validatorResponse =
          ApiKeyValidators.validateDeleteApiKeyOptions(
              options == null ? null : options.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      String endpoint = "api-keys/" + UriEncoding.encodePathSegment(id);
      if (options != null && options.toMap().containsKey("owner")) {
        endpoint +=
            "?owner=" + UriEncoding.encodePathSegment((String) options.toMap().get("owner"));
      }

      return request.call(endpoint, null, "DELETE", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "delete");
    }
  }
}
