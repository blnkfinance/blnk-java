package com.blnkfinance.blnk.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value helpers shared across the SDK: number-to-string interpolation for
 * messages and log output, string-to-number coercion for amount inputs, and
 * the loose truthiness check backing the client's presence tests.
 */
public final class ValueFormat {

  private ValueFormat() {}

  // ---------------------------------------------------------------------
  // Number → string interpolation
  // ---------------------------------------------------------------------

  /**
   * Formats a number for message interpolation: integral finite doubles print
   * without a decimal point ({@code "100"} not {@code "100.0"}); NaN and
   * infinities print as {@code NaN}/{@code Infinity}/{@code -Infinity}; very
   * large (≥1e21) or very small (<1e-6) magnitudes use exponent notation
   * ({@code 1e+21}).
   */
  public static String formatNumber(double value) {
    if (Double.isNaN(value)) {
      return "NaN";
    }
    if (Double.isInfinite(value)) {
      return value > 0 ? "Infinity" : "-Infinity";
    }
    if (value == 0) {
      return "0"; // Negative zero prints as "0".
    }
    boolean negative = value < 0;
    double abs = Math.abs(value);

    // Shortest round-trip decimal digits from Double.toString, normalized to
    // (digits, pointPos) where abs == 0.digits * 10^pointPos.
    String repr = Double.toString(abs);
    String digits;
    int pointPos;
    int e = repr.indexOf('E');
    if (e >= 0) {
      String mantissa = repr.substring(0, e);
      int exp = Integer.parseInt(repr.substring(e + 1));
      int dot = mantissa.indexOf('.');
      String mantissaDigits = mantissa.replace(".", "");
      int intDigits = dot < 0 ? mantissa.length() : dot;
      digits = mantissaDigits;
      pointPos = intDigits + exp;
    } else {
      int dot = repr.indexOf('.');
      digits = repr.replace(".", "");
      pointPos = dot < 0 ? repr.length() : dot;
    }
    // Strip leading zeros (adjusting the point position).
    int lead = 0;
    while (lead < digits.length() - 1 && digits.charAt(lead) == '0') {
      lead++;
      pointPos--;
    }
    digits = digits.substring(lead);
    // Strip trailing zeros.
    int end = digits.length();
    while (end > 1 && digits.charAt(end - 1) == '0') {
      end--;
    }
    digits = digits.substring(0, end);

    int k = digits.length();
    int n = pointPos; // value = digits * 10^(n - k)
    StringBuilder sb = new StringBuilder();
    if (negative) {
      sb.append('-');
    }
    if (k <= n && n <= 21) {
      sb.append(digits);
      sb.append("0".repeat(n - k));
    } else if (0 < n && n <= 21) {
      sb.append(digits, 0, n).append('.').append(digits, n, k);
    } else if (-6 < n && n <= 0) {
      sb.append("0.").append("0".repeat(-n)).append(digits);
    } else {
      // Exponential form.
      sb.append(digits.charAt(0));
      if (k > 1) {
        sb.append('.').append(digits, 1, k);
      }
      int exp = n - 1;
      sb.append('e').append(exp >= 0 ? "+" : "-").append(Math.abs(exp));
    }
    return sb.toString();
  }

  /** Convenience overload for boxed numbers. */
  public static String formatNumber(Number value) {
    if (value instanceof Integer || value instanceof Long || value instanceof Short
        || value instanceof Byte) {
      return String.valueOf(value.longValue());
    }
    return formatNumber(value.doubleValue());
  }

  // ---------------------------------------------------------------------
  // String → number coercion
  // ---------------------------------------------------------------------

  /** Whitespace and line terminators trimmed before coercion (Unicode spaces and BOM included). */
  private static final String TRIMMABLE_WHITESPACE =
      "[\\s\\u000B\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";

  private static final Pattern DECIMAL_LITERAL =
      Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
  private static final Pattern NUMBER_FULL =
      Pattern.compile("[+-]?(Infinity|\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
  private static final Pattern PARSE_FLOAT_PREFIX =
      Pattern.compile("^[+-]?(Infinity|(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?)");

  /**
   * Coerces a whole string to a number: whitespace-trimmed; null or empty →
   * 0; supports {@code 0x/0o/0b} literals and {@code ±Infinity}; anything
   * that is not entirely a numeric literal → NaN ({@code "12px"} → NaN).
   */
  public static double coerceNumber(String str) {
    if (str == null) {
      return 0; // A null string coerces to 0.
    }
    String trimmed =
        str.replaceAll("^" + TRIMMABLE_WHITESPACE + "+|" + TRIMMABLE_WHITESPACE + "+$", "");
    if (trimmed.isEmpty()) {
      return 0;
    }
    try {
      if (trimmed.matches("0[xX][0-9a-fA-F]+")) {
        return Long.parseLong(trimmed.substring(2), 16);
      }
      if (trimmed.matches("0[oO][0-7]+")) {
        return Long.parseLong(trimmed.substring(2), 8);
      }
      if (trimmed.matches("0[bB][01]+")) {
        return Long.parseLong(trimmed.substring(2), 2);
      }
      if (trimmed.equals("Infinity") || trimmed.equals("+Infinity")) {
        return Double.POSITIVE_INFINITY;
      }
      if (trimmed.equals("-Infinity")) {
        return Double.NEGATIVE_INFINITY;
      }
      if (DECIMAL_LITERAL.matcher(trimmed).matches()) {
        return Double.parseDouble(trimmed);
      }
      if (NUMBER_FULL.matcher(trimmed).matches()) {
        return Double.parseDouble(trimmed);
      }
    } catch (NumberFormatException ignored) {
      return Double.NaN;
    }
    return Double.NaN;
  }

  /**
   * Parses the longest numeric prefix of a string after skipping leading
   * whitespace ({@code "12px"} → 12); no numeric prefix → NaN.
   */
  public static double parseLeadingNumber(String str) {
    if (str == null) {
      return Double.NaN;
    }
    String trimmed = str.replaceAll("^" + TRIMMABLE_WHITESPACE + "+", "");
    Matcher matcher = PARSE_FLOAT_PREFIX.matcher(trimmed);
    if (!matcher.find()) {
      return Double.NaN;
    }
    String prefix = matcher.group();
    if (prefix.endsWith("Infinity")) {
      return prefix.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }
    // Trim a dangling exponent marker ("1e", "1e+") that the regex excludes by
    // construction; Double.parseDouble handles the rest.
    return Double.parseDouble(prefix);
  }

  // ---------------------------------------------------------------------
  // Truthiness
  // ---------------------------------------------------------------------

  /**
   * The loose truthiness check backing the SDK's presence tests. Falsy: null,
   * false, empty string, numeric 0, NaN, JSON null/missing nodes, and
   * JsonNode equivalents of the above. Everything else (including empty
   * objects and empty arrays) is truthy.
   */
  public static boolean isTruthy(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof String s) {
      return !s.isEmpty();
    }
    if (value instanceof Number n) {
      double d = n.doubleValue();
      return d != 0 && !Double.isNaN(d);
    }
    if (value instanceof JsonNode node) {
      if (node.isNull() || node.isMissingNode()) {
        return false;
      }
      if (node.isBoolean()) {
        return node.booleanValue();
      }
      if (node.isTextual()) {
        return !node.asText().isEmpty();
      }
      if (node.isNumber()) {
        double d = node.doubleValue();
        return d != 0 && !Double.isNaN(d);
      }
      return true;
    }
    if (value instanceof Map || value instanceof List) {
      return true;
    }
    return true;
  }

  public static boolean isFalsy(Object value) {
    return !isTruthy(value);
  }
}
