package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.UpdateMetadataData;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.validators.MetadataValidators;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Endpoint service for metadata updates on ledgers, transactions, balances,
 * and identities.
 */
public class Metadata {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Metadata(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Updates an entity's metadata — {@code POST {id}/metadata}. Note: the HTTP
   * method is POST (not PUT or PATCH), and the caller-supplied entity id
   * forms the leading path segment RAW — it is validated but NOT URL-encoded.
   * Validates, then forwards {@code data} unmodified. Never throws: runtime
   * errors are converted to an error response, reported under the name
   * {@code "update"}.
   */
  public ApiResponse<JsonNode> update(String id, UpdateMetadataData data) {
    try {
      String validatorResponse =
          MetadataValidators.validateUpdateMetadataData(id, data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      return request.call(id + "/metadata", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "update");
    }
  }
}
