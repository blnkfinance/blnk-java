package com.blnkfinance.blnk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;

import java.util.regex.Pattern;

/**
 * Transaction payload serialization. The date-semantics members are aliases
 * of {@link DateSerialization} so both serializers share one policy.
 */
public final class TransactionSerialization {

  private TransactionSerialization() {}

  /**
   * RFC3339 without fractional seconds — matches Blnk Core
   * {@code time.Parse("2006-01-02T15:04:05Z07:00", …)}. Accepts {@code Z},
   * {@code ±HH:MM} and {@code ±HHMM}; rejects fractional seconds and
   * date-only strings.
   */
  public static final Pattern RFC3339_DATETIME = DateSerialization.RFC3339_DATETIME;

  /** The four keys serializeCreateTransaction overwrites, in the order they are applied. */
  private static final String[] DATE_FIELDS = {
    "inflight_expiry_date", "scheduled_for", "effective_date", "inflight_commit_date"
  };

  /**
   * Validates a date input: date objects are always valid; strings are
   * trimmed, must be non-empty, match {@link #RFC3339_DATETIME} and parse;
   * anything else is invalid.
   */
  public static boolean isValidTransactionDateInput(Object value) {
    return DateSerialization.isValidTransactionDateInput(value);
  }

  /**
   * Serializes a date input: null → null; date object → UTC RFC 3339 with
   * sub-second precision STRIPPED (truncated) — {@code YYYY-MM-DDTHH:mm:ssZ};
   * string → returned unchanged, deliberately including any surrounding
   * whitespace.
   */
  public static String serializeTransactionDate(Object value) {
    return DateSerialization.serializeTransactionDate(value);
  }

  /**
   * Returns a copy of the payload with exactly four keys overwritten by
   * {@code serializeTransactionDate}: {@code inflight_expiry_date},
   * {@code scheduled_for}, {@code effective_date},
   * {@code inflight_commit_date}. All other fields pass through untouched.
   *
   * <p>Raw date objects arrive POJONode-wrapped (see
   * {@code CreateTransactions.toJson()}); string dates (TextNodes) pass
   * through byte-identically. Absent keys stay absent — no date key is ever
   * added to the wire body.
   */
  public static ObjectNode serializeCreateTransaction(ObjectNode payload) {
    ObjectNode out = payload.deepCopy();
    for (String field : DATE_FIELDS) {
      JsonNode node = out.get(field);
      if (node == null) {
        continue;
      }
      if (node instanceof POJONode pojoNode && DateSerialization.isDateObject(pojoNode.getPojo())) {
        out.put(field, DateSerialization.serializeTransactionDate(pojoNode.getPojo()));
      }
      // TextNode (string input) and any other value pass through unchanged;
      // only date objects are re-serialized.
    }
    return out;
  }
}
