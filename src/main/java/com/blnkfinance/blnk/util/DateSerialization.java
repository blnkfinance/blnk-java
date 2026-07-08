package com.blnkfinance.blnk.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Core date helpers shared by the transaction and identity serializers plus
 * the hooks/api-keys validators.
 *
 * <p>Date objects always serialize as UTC ISO-8601 without fractional
 * seconds: {@code YYYY-MM-DDTHH:mm:ssZ} — the timestamp layout Blnk Core
 * expects. Sub-second precision is ALWAYS STRIPPED (truncated, not rounded).
 */
public final class DateSerialization {

  private DateSerialization() {}

  /**
   * RFC 3339 date-time without fractional seconds (matches Blnk Core's Go
   * {@code time.Parse} layout). Accepts {@code Z}, {@code ±HH:MM}, and
   * {@code ±HHMM} offsets.
   */
  public static final Pattern RFC3339_DATETIME =
      Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}(?::\\d{2}|\\d{2}))$");

  private static final DateTimeFormatter STRIPPED_MILLIS_UTC =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  /**
   * Validates a transaction date input: date objects are always valid;
   * strings are trimmed, must be non-empty, match {@link #RFC3339_DATETIME},
   * and parse to a real instant; everything else is invalid.
   */
  public static boolean isValidTransactionDateInput(Object value) {
    if (isDateObject(value)) {
      return true; // Java temporals always hold a valid instant.
    }
    if (value instanceof String s) {
      String trimmed = s.trim();
      if (trimmed.isEmpty()) {
        return false;
      }
      if (!RFC3339_DATETIME.matcher(trimmed).matches()) {
        return false;
      }
      return parseRfc3339(trimmed) != null;
    }
    return false;
  }

  /**
   * Serializes a transaction date: null → null; date object → UTC ISO string
   * with sub-second precision stripped; string → returned as-is (NOT
   * validated here); other values returned via String.valueOf (unreachable
   * from typed callers).
   */
  public static String serializeTransactionDate(Object value) {
    if (value == null) {
      return null;
    }
    Instant instant = toInstant(value);
    if (instant != null) {
      return formatStrippedMillisUtc(instant);
    }
    if (value instanceof String s) {
      return s;
    }
    return String.valueOf(value);
  }

  /** Formats as {@code YYYY-MM-DDTHH:mm:ssZ}; sub-second precision is truncated, not rounded. */
  public static String formatStrippedMillisUtc(Instant instant) {
    return STRIPPED_MILLIS_UTC.format(instant.truncatedTo(ChronoUnit.SECONDS));
  }

  /** True for the supported date/time types: Date, Instant, OffsetDateTime, ZonedDateTime. */
  public static boolean isDateObject(Object value) {
    return value instanceof Date || value instanceof Instant || value instanceof OffsetDateTime
        || value instanceof ZonedDateTime;
  }

  /** Converts supported date objects to an Instant; null for anything else. */
  public static Instant toInstant(Object value) {
    if (value instanceof Date d) {
      return d.toInstant();
    }
    if (value instanceof Instant i) {
      return i;
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof ZonedDateTime zdt) {
      return zdt.toInstant();
    }
    return null;
  }

  /**
   * Parses a string that already matches {@link #RFC3339_DATETIME} to an
   * {@link Instant} (supports the no-colon offset form {@code +0000} the
   * pattern allows); null when it is not a real calendar timestamp.
   */
  public static Instant parseRfc3339(String value) {
    String normalized = value;
    // "+HHMM" / "-HHMM" (no colon) → "+HH:MM" for ISO_OFFSET_DATE_TIME.
    if (normalized.length() >= 5) {
      char signChar = normalized.charAt(normalized.length() - 5);
      if ((signChar == '+' || signChar == '-')
          && normalized.indexOf(':', normalized.length() - 5) < 0) {
        normalized = normalized.substring(0, normalized.length() - 2) + ":"
            + normalized.substring(normalized.length() - 2);
      }
    }
    try {
      return OffsetDateTime.parse(normalized).toInstant();
    } catch (RuntimeException e) {
      return null;
    }
  }
}
