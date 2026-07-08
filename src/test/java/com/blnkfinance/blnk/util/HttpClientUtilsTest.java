package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for {@link HttpClientUtils#readResponseJsonBody}: empty and non-empty bodies. */
class HttpClientUtilsTest {

  @Test
  @DisplayName("returns null for empty bodies")
  void returnsNullForEmptyBodies() {
    TransportResponse response = TransportResponse.builder().text(() -> "").build();

    assertNull(HttpClientUtils.readResponseJsonBody(response));
  }

  @Test
  @DisplayName("parses non-empty JSON bodies")
  void parsesNonEmptyJsonBodies() {
    TransportResponse response =
        TransportResponse.builder().text(() -> "{\"message\":\"deleted\"}").build();

    assertEquals(
        BlnkJson.parse("{\"message\":\"deleted\"}"),
        HttpClientUtils.readResponseJsonBody(response));
  }
}
