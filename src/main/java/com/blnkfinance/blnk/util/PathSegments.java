package com.blnkfinance.blnk.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Validation and escaping for caller-supplied values that become part of a
 * request path, such as resource IDs. Every such value must go through
 * {@link #segment} (or {@link #reference} for free-form transaction
 * references), so an ID like {@code ../backup} or {@code %2e%2e/secret}
 * cannot change which endpoint the client's API key is sent to.
 */
public final class PathSegments {

  private PathSegments() {}

  /** Thrown when a value cannot be used as a request path segment. */
  public static final class InvalidPathSegmentException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InvalidPathSegmentException(String message) {
      super(message);
    }
  }

  /**
   * Returns {@code value} percent-encoded for use as a single path segment
   * (spaces become {@code %20}, never {@code +}). Throws
   * {@link InvalidPathSegmentException} when the value is null or empty, is
   * {@code .} or {@code ..}, or contains {@code / \ ? # %} or a control
   * character: characters that would add a segment, start a query or
   * fragment, or carry an encoding a server might decode into one of those.
   */
  public static String segment(String value) {
    rejectEmptyOrDot(value);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '/' || c == '\\' || c == '?' || c == '#' || c == '%') {
        throw new InvalidPathSegmentException(
            "invalid path segment " + quote(value) + ": must not contain / \\ ? # or %");
      }
      if (c < 0x20 || c == 0x7f) {
        throw new InvalidPathSegmentException(
            "invalid path segment " + quote(value) + ": must not contain control characters");
      }
    }
    return UriEncoding.encodePathSegment(value);
  }

  /**
   * Returns a free-form value, such as a transaction reference, encoded as a
   * single path segment. Unlike {@link #segment}, {@code /}, {@code %} and the
   * other reserved characters are escaped rather than rejected, because
   * references may legitimately contain them. Null, empty, {@code .} and
   * {@code ..} are still rejected, because escaping leaves dots unchanged.
   */
  public static String reference(String value) {
    rejectEmptyOrDot(value);
    return UriEncoding.encodePathSegment(value);
  }

  /**
   * True when the path part of {@code pathOrUrl} (everything before the
   * first {@code ?} or {@code #}) contains a {@code .} or {@code ..} segment
   * once percent-decoded, so {@code %2e%2e} counts the same as {@code ..}.
   */
  public static boolean hasDotSegment(String pathOrUrl) {
    if (pathOrUrl == null) {
      return false;
    }
    String path = pathOrUrl;
    int cut = indexOfAny(path, '?', '#');
    if (cut >= 0) {
      path = path.substring(0, cut);
    }
    String decoded = percentDecode(path);
    for (String part : decoded.split("[/\\\\]", -1)) {
      if (part.equals(".") || part.equals("..")) {
        return true;
      }
    }
    return false;
  }

  private static void rejectEmptyOrDot(String value) {
    if (value == null || value.isEmpty()) {
      throw new InvalidPathSegmentException("invalid path segment: value is empty");
    }
    if (value.equals(".") || value.equals("..")) {
      throw new InvalidPathSegmentException("invalid path segment " + quote(value));
    }
  }

  private static int indexOfAny(String s, char a, char b) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == a || c == b) {
        return i;
      }
    }
    return -1;
  }

  /** Lenient percent-decoding: malformed escapes are kept as-is, {@code +} is not a space. */
  private static String percentDecode(String s) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    for (int i = 0; i < bytes.length; i++) {
      byte b = bytes[i];
      if (b == '%' && i + 2 < bytes.length) {
        int hi = Character.digit(bytes[i + 1], 16);
        int lo = Character.digit(bytes[i + 2], 16);
        if (hi >= 0 && lo >= 0) {
          out.write((hi << 4) | lo);
          i += 2;
          continue;
        }
      }
      out.write(b);
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  private static String quote(String value) {
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '"' || c == '\\') {
        sb.append('\\').append(c);
      } else if (c < 0x20 || c == 0x7f) {
        sb.append(String.format("\\u%04x", (int) c));
      } else {
        sb.append(c);
      }
    }
    return sb.append('"').toString();
  }
}
