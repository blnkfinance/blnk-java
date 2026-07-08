package com.blnkfinance.blnk;

import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkApiErrorDetail;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Formats a terminal outcome (status, message, data, optional structured
 * error) into an {@link ApiResponse}. The canonical implementation is
 * {@link com.blnkfinance.blnk.util.HttpClientUtils#FORMAT_RESPONSE}.
 */
@FunctionalInterface
public interface FormatResponseFn {

  ApiResponse<JsonNode> format(int status, String message, JsonNode data, BlnkApiErrorDetail error);

  default ApiResponse<JsonNode> format(int status, String message, JsonNode data) {
    return format(status, message, data, null);
  }
}
