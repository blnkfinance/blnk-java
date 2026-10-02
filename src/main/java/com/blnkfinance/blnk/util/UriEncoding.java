package com.blnkfinance.blnk.util;

import java.nio.charset.StandardCharsets;

/**
 * Percent-encoding for URI path segments: percent-encodes everything outside
 * {@code A-Z a-z 0-9 - _ . ! ~ * ' ( )}. Each byte of the UTF-8 encoding
 * outside that set becomes {@code %XX}. This is the canonical shared helper
 * for path-segment encoding — do not re-implement it elsewhere.
 */
public final class UriEncoding {

  private UriEncoding() {}

  private static final String UNRESERVED = "-_.!~*'()";

  public static String encodePathSegment(String value) {
    StringBuilder sb = new StringBuilder(value.length());
    for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
      int c = b & 0xFF;
      if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
          || UNRESERVED.indexOf(c) >= 0) {
        sb.append((char) c);
      } else {
        sb.append('%').append(String.format("%02X", c));
      }
    }
    return sb.toString();
  }

  /**
   * Appends {@code name=value} to {@code url}. Uses {@code ?} when the URL has
   * no query string yet, otherwise {@code &}. {@code value} is percent-encoded
   * the same way as a path segment. A null or empty {@code value} leaves
   * {@code url} unchanged so self-hosted Core calls are not given a Cloud
   * routing param.
   */
  public static String appendQueryParam(String url, String name, String value) {
    if (url == null || name == null || name.isEmpty() || value == null || value.isEmpty()) {
      return url;
    }
    char separator = url.indexOf('?') >= 0 ? '&' : '?';
    return url + separator + name + '=' + encodePathSegment(value);
  }
}
