package com.blnkfinance.blnk.util;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small string and value-shape helpers shared by the request validators. */
public final class StringUtils {

  private StringUtils() {}

  private static final Pattern SNAKE_SEGMENT = Pattern.compile("_([a-z])");

  private static final Set<String> OPERATOR_TYPES = Set.of("!=", "<", "<=", "=", ">", ">=");

  /** Regex {@code _([a-z])} → uppercase group 1 (only lowercase latin after {@code _}). */
  public static String toCamelCase(String str) {
    Matcher matcher = SNAKE_SEGMENT.matcher(str);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /** True when the value is a String. */
  public static boolean isValidString(Object val) {
    return val instanceof String;
  }

  /**
   * True for any {@link Number}. Note: {@code Double.NaN} deliberately passes
   * this check — validators only require a numeric type, not a finite value.
   */
  public static boolean isValidNumber(Object val) {
    return val instanceof Number;
  }

  /** True for a List, a Java array, or a Jackson ArrayNode. */
  public static boolean isValidArray(Object val) {
    return val instanceof List || val instanceof ArrayNode
        || (val != null && val.getClass().isArray());
  }

  /** {@code val ∈ {"!=", "<", "<=", "=", ">", ">="}}. */
  public static boolean isValidOperator(Object val) {
    return val instanceof String s && OPERATOR_TYPES.contains(s);
  }
}
