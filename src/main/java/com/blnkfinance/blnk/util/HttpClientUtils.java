package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkApiErrorDetail;
import com.blnkfinance.blnk.types.BlnkJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** Response-formatting and body-reading helpers shared by the client and services. */
public final class HttpClientUtils {

  private HttpClientUtils() {}

  /**
   * The canonical {@link FormatResponseFn} instance. Tests assert on this
   * exact reference, so it must remain a single shared constant.
   */
  public static final FormatResponseFn FORMAT_RESPONSE = HttpClientUtils::formatResponse;

  /**
   * Builds an {@link ApiResponse}. The {@code error} field is attached ONLY
   * when error is non-null; otherwise it is left unset.
   */
  public static ApiResponse<JsonNode> formatResponse(
      int status, String message, JsonNode data, BlnkApiErrorDetail error) {
    if (error != null) {
      return new ApiResponse<>(status, message, data, error);
    }
    return new ApiResponse<>(status, message, data);
  }

  /**
   * Reads the response body as JSON. The text body is consulted first (blank
   * → null, otherwise parsed — parse failures intentionally propagate
   * UNCHECKED to the caller); else the {@code json()} fallback; else null.
   */
  public static JsonNode readResponseJsonBody(TransportResponse response) {
    if (response.hasText()) {
      String text = response.text();
      if (text.trim().isEmpty()) {
        return null;
      }
      return BlnkJson.parse(text);
    }
    if (response.hasJson()) {
      return response.json();
    }
    return null;
  }

  /**
   * Copies {@code response}, renaming every key not listed in
   * {@code keepFields} via {@link StringUtils#toCamelCase}; values are copied
   * unchanged. Public utility — not used by the SDK runtime itself.
   */
  public static ObjectNode mapResponse(ObjectNode response, Collection<String> keepFields) {
    ObjectNode result = BlnkJson.objectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = response.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String newKey =
          keepFields.contains(field.getKey()) ? field.getKey()
              : StringUtils.toCamelCase(field.getKey());
      result.set(newKey, field.getValue());
    }
    return result;
  }

  /**
   * Canonical HTTP reason phrases used as the statusText fallback
   * ({@code java.net.http} exposes no reason phrase of its own). Unknown
   * status → {@code ""}.
   */
  public static String reasonPhrase(int status) {
    return switch (status) {
      case 100 -> "Continue";
      case 101 -> "Switching Protocols";
      case 200 -> "OK";
      case 201 -> "Created";
      case 202 -> "Accepted";
      case 203 -> "Non-Authoritative Information";
      case 204 -> "No Content";
      case 205 -> "Reset Content";
      case 206 -> "Partial Content";
      case 300 -> "Multiple Choices";
      case 301 -> "Moved Permanently";
      case 302 -> "Found";
      case 303 -> "See Other";
      case 304 -> "Not Modified";
      case 307 -> "Temporary Redirect";
      case 308 -> "Permanent Redirect";
      case 400 -> "Bad Request";
      case 401 -> "Unauthorized";
      case 402 -> "Payment Required";
      case 403 -> "Forbidden";
      case 404 -> "Not Found";
      case 405 -> "Method Not Allowed";
      case 406 -> "Not Acceptable";
      case 407 -> "Proxy Authentication Required";
      case 408 -> "Request Timeout";
      case 409 -> "Conflict";
      case 410 -> "Gone";
      case 411 -> "Length Required";
      case 412 -> "Precondition Failed";
      case 413 -> "Payload Too Large";
      case 414 -> "URI Too Long";
      case 415 -> "Unsupported Media Type";
      case 416 -> "Range Not Satisfiable";
      case 417 -> "Expectation Failed";
      case 418 -> "I'm a Teapot";
      case 422 -> "Unprocessable Entity";
      case 423 -> "Locked";
      case 425 -> "Too Early";
      case 426 -> "Upgrade Required";
      case 428 -> "Precondition Required";
      case 429 -> "Too Many Requests";
      case 431 -> "Request Header Fields Too Large";
      case 451 -> "Unavailable For Legal Reasons";
      case 500 -> "Internal Server Error";
      case 501 -> "Not Implemented";
      case 502 -> "Bad Gateway";
      case 503 -> "Service Unavailable";
      case 504 -> "Gateway Timeout";
      case 505 -> "HTTP Version Not Supported";
      case 506 -> "Variant Also Negotiates";
      case 507 -> "Insufficient Storage";
      case 508 -> "Loop Detected";
      case 510 -> "Not Extended";
      case 511 -> "Network Authentication Required";
      default -> "";
    };
  }
}
