package com.blnkfinance.blnk.testsupport;

import java.nio.charset.StandardCharsets;

/**
 * Decodes a captured request body into a UTF-8 string so multipart tests can
 * match against the payload.
 */
public final class StreamTestUtils {

  private StreamTestUtils() {}

  /** Decodes the body bytes as UTF-8; a null body decodes to "". */
  public static String readBody(byte[] body) {
    return body == null ? "" : new String(body, StandardCharsets.UTF_8);
  }
}
