package com.blnkfinance.blnk.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The identity payload serializer, applied by {@code Identity.create} and
 * {@code Identity.update} ONLY. Date handling delegates to
 * {@link DateSerialization}.
 */
public final class IdentitySerialization {

  private IdentitySerialization() {}

  /**
   * A pure delegate to the shared transaction date-input check: date objects
   * are always valid; strings are trimmed, must be non-empty, match
   * {@code RFC3339_DATETIME} (no fractional seconds) and form a real calendar
   * timestamp; anything else is invalid.
   */
  public static boolean isValidIdentityDateInput(Object value) {
    return DateSerialization.isValidTransactionDateInput(value);
  }

  /**
   * Returns a shallow copy of the payload with {@code dob} — the ONLY
   * transformed field — replaced by its serialized form:
   * <ul>
   *   <li>absent or null dob → the key is omitted from the wire body
   *       entirely;</li>
   *   <li>date object → UTC {@code YYYY-MM-DDTHH:mm:ssZ}, sub-second
   *       precision ALWAYS STRIPPED (truncated, not rounded);</li>
   *   <li>string → passed through BYTE-FOR-BYTE, deliberately with no
   *       trimming and no re-formatting: a validated
   *       {@code " 1990-01-15T00:00:00Z "} goes out with the spaces, and
   *       offset forms are not normalized to Z.</li>
   * </ul>
   * Every other field (including {@code meta_data}) is copied through
   * untouched — no renames, no reordering.
   */
  public static Map<String, Object> serializeIdentityData(Map<String, Object> data) {
    Map<String, Object> payload = new LinkedHashMap<>(data);
    String dob = DateSerialization.serializeTransactionDate(data.get("dob"));
    if (dob == null) {
      payload.remove("dob");
    } else {
      payload.put("dob", dob);
    }
    return payload;
  }
}
