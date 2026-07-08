package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.BlnkTransport;
import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Canned transport stubs. Both stubs echo the request headers back as the
 * response headers so header-propagation tests can assert on them.
 */
public final class TransportMocks {

  private TransportMocks() {}

  /** Always succeeds: 200 / "OK" with body {@code {"message":"Success"}}. */
  public static BlnkTransport successTransport() {
    return (url, request) -> TransportResponse.builder()
        .ok(true)
        .status(200)
        .statusText("OK")
        .headers(request.headers())
        .text(() -> "{\"message\":\"Success\"}")
        .json(() -> {
          ObjectNode body = BlnkJson.objectNode();
          body.put("message", "Success");
          return body;
        })
        .build();
  }

  /**
   * Always fails: 500 with statusText "Failed" — deliberately not the
   * canonical reason phrase, and tests assert on this exact text — and body
   * {@code {"message":"Failed"}}. The body carries no error/error_detail, so
   * no structured error is parsed.
   */
  public static BlnkTransport failTransport() {
    return (url, request) -> TransportResponse.builder()
        .ok(false)
        .status(500)
        .statusText("Failed")
        .headers(request.headers())
        .text(() -> "{\"message\":\"Failed\"}")
        .json(() -> {
          ObjectNode body = BlnkJson.objectNode();
          body.put("message", "Failed");
          return body;
        })
        .build();
  }
}
