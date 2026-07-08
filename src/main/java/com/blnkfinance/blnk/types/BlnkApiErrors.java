package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extracts structured error details from Blnk API error bodies.
 */
public final class BlnkApiErrors {

  private BlnkApiErrors() {}

  /**
   * Extracts structured Blnk API error details from a parsed JSON error body.
   *
   * <p>Check order:
   * <ol>
   *   <li>null nodes, JSON null, strings, numbers and booleans yield null;
   *       objects AND arrays proceed to the field checks;</li>
   *   <li>an {@code error_detail} object with string {@code code} AND string
   *       {@code message} yields {@code {code, message, details}};</li>
   *   <li>a non-empty string {@code error} yields
   *       {@code {code: "UNKNOWN", message: error}};</li>
   *   <li>otherwise null.</li>
   * </ol>
   */
  public static BlnkApiErrorDetail parseBlnkApiErrorBody(JsonNode body) {
    if (body == null || body.isNull() || body.isMissingNode()
        || !(body.isObject() || body.isArray())) {
      return null;
    }

    JsonNode errorDetail = body.get("error_detail");
    if (errorDetail != null && !errorDetail.isNull() && errorDetail.isObject()) {
      JsonNode code = errorDetail.get("code");
      JsonNode message = errorDetail.get("message");
      if (code != null && code.isTextual() && message != null && message.isTextual()) {
        JsonNode details = errorDetail.get("details");
        return new BlnkApiErrorDetail(code.asText(), message.asText(), details);
      }
    }

    JsonNode error = body.get("error");
    if (error != null && error.isTextual() && !error.asText().isEmpty()) {
      return new BlnkApiErrorDetail("UNKNOWN", error.asText());
    }

    return null;
  }
}
