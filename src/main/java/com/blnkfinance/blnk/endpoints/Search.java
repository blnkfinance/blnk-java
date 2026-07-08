package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.FilterParams;
import com.blnkfinance.blnk.types.SearchParams;
import com.blnkfinance.blnk.types.StartReindexRequest;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.SearchValidators;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint service for search: typed searches, filters, and reindexing. The
 * collection parameter is a plain {@link String} (see
 * {@link com.blnkfinance.blnk.types.SearchCollections} for the known values)
 * so an unknown collection is rejected by runtime validation rather than the
 * compiler.
 */
public class Search {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Search(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Searches a collection — the collection is validated FIRST, then the
   * params; the params object goes to the wire unmodified via
   * {@code POST search/{service}}.
   */
  public ApiResponse<JsonNode> search(SearchParams data, String service) {
    try {
      String collectionError = SearchValidators.validateSearchCollection(service);
      if (collectionError != null) {
        return formatResponse.format(400, collectionError, null, null);
      }

      String paramsError =
          SearchValidators.validateSearchParams(data == null ? null : data.toMap());
      if (paramsError != null) {
        return formatResponse.format(400, paramsError, null, null);
      }

      return request.call("search/" + service, data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "search");
    }
  }

  /**
   * Filters a collection — the collection is validated FIRST, then the
   * params; {@code POST {collection}/filter}.
   */
  public ApiResponse<JsonNode> filter(FilterParams data, String collection) {
    try {
      String collectionError = SearchValidators.validateSearchCollection(collection);
      if (collectionError != null) {
        return formatResponse.format(400, collectionError, null, null);
      }

      String paramsError =
          SearchValidators.validateFilterParams(data == null ? null : data.toMap());
      if (paramsError != null) {
        return formatResponse.format(400, paramsError, null, null);
      }

      return request.call(collection + "/filter", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "filter");
    }
  }

  /** Starts a reindex with no options — validation is skipped and the body is {@code {}}. */
  public ApiResponse<JsonNode> startReindex() {
    return startReindexInternal(null);
  }

  /**
   * Starts a reindex — {@code POST search/reindex}. A {@code null} argument
   * is treated as "no options" and behaves like {@link #startReindex()}. The
   * wire body is REBUILT rather than forwarded: only {@code batch_size} is
   * ever copied into the payload; any other fields are dropped.
   */
  public ApiResponse<JsonNode> startReindex(StartReindexRequest options) {
    return startReindexInternal(options);
  }

  private ApiResponse<JsonNode> startReindexInternal(StartReindexRequest options) {
    try {
      if (options != null) {
        String paramsError = SearchValidators.validateStartReindexRequest(options.toMap());
        if (paramsError != null) {
          return formatResponse.format(400, paramsError, null, null);
        }
      }

      Map<String, Object> body = new LinkedHashMap<>();
      if (options != null && options.toMap().containsKey("batch_size")) {
        body.put("batch_size", options.toMap().get("batch_size"));
      }

      return request.call("search/reindex", BlnkJson.toObjectNode(body), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "startReindex");
    }
  }

  /** Retrieves reindex status — {@code GET search/reindex}, no body. */
  public ApiResponse<JsonNode> getReindexStatus() {
    try {
      return request.call("search/reindex", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getReindexStatus");
    }
  }
}
