package com.blnkfinance.blnk;

import com.blnkfinance.blnk.types.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * The request function services depend on. Services receive this seam (never
 * the Blnk instance itself) so tests can inject mocks.
 *
 * <p>{@code data} is one of: {@code null} (no body), an {@link
 * com.fasterxml.jackson.databind.node.ObjectNode} (JSON body), or a {@link
 * com.blnkfinance.blnk.util.MultipartBody} (multipart form upload).
 * {@code method} is {@code "POST" | "GET" | "PUT" | "DELETE"}.
 * {@code headerOptions} may be null.
 *
 * <p>Never throws for request failures — always returns an ApiResponse value.
 */
@FunctionalInterface
public interface BlnkRequest {

  ApiResponse<JsonNode> call(
      String endpoint, Object data, String method, Map<String, String> headerOptions);
}
