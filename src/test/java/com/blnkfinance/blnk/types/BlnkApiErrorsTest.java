package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for {@link BlnkApiErrors#parseBlnkApiErrorBody}: structured, legacy, and non-object error bodies. */
class BlnkApiErrorsTest {

  @Test
  @DisplayName("parses error_detail from Core API responses")
  void parsesErrorDetail() {
    BlnkApiErrorDetail parsed = BlnkApiErrors.parseBlnkApiErrorBody(BlnkJson.parse(
        "{\"error\":\"ledger not found\",\"error_detail\":{\"code\":\"LGR_NOT_FOUND\","
            + "\"message\":\"ledger not found\",\"details\":{\"ledger_id\":\"ldg_missing\"}}}"));

    assertEquals(
        new BlnkApiErrorDetail(
            "LGR_NOT_FOUND", "ledger not found", BlnkJson.parse("{\"ledger_id\":\"ldg_missing\"}")),
        parsed);
  }

  @Test
  @DisplayName("falls back to legacy error string")
  void fallsBackToLegacyErrorString() {
    BlnkApiErrorDetail parsed =
        BlnkApiErrors.parseBlnkApiErrorBody(BlnkJson.parse("{\"error\":\"invalid request\"}"));

    assertEquals(new BlnkApiErrorDetail("UNKNOWN", "invalid request"), parsed);
  }

  @Test
  @DisplayName("surfaces TXN_INVALID_AMOUNT from Core error_detail")
  void surfacesTxnInvalidAmount() {
    BlnkApiErrorDetail parsed =
        BlnkApiErrors.parseBlnkApiErrorBody(
            BlnkJson.parse(
                "{\"error\":\"amount must be positive\",\"error_detail\":{\"code\":\""
                    + BlnkErrorCodes.TXN_INVALID_AMOUNT
                    + "\",\"message\":\"amount must be positive\"}}"));

    assertEquals(BlnkErrorCodes.TXN_INVALID_AMOUNT, parsed.code());
    assertEquals("amount must be positive", parsed.message());
  }

  @Test
  @DisplayName("surfaces GEN_CONFLICT from Core error_detail")
  void surfacesGenConflict() {
    BlnkApiErrorDetail parsed =
        BlnkApiErrors.parseBlnkApiErrorBody(
            BlnkJson.parse(
                "{\"error\":\"indicator already exists\",\"error_detail\":{\"code\":\""
                    + BlnkErrorCodes.GEN_CONFLICT
                    + "\",\"message\":\"indicator already exists\"}}"));

    assertEquals(BlnkErrorCodes.GEN_CONFLICT, parsed.code());
  }

  @Test
  @DisplayName("returns null for non-object bodies")
  void returnsNullForNonObjectBodies() {
    assertNull(BlnkApiErrors.parseBlnkApiErrorBody(null));
    assertNull(BlnkApiErrors.parseBlnkApiErrorBody(TextNode.valueOf("oops")));
  }
}
