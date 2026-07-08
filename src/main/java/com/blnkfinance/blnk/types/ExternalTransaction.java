package com.blnkfinance.blnk.types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inline external transaction for instant reconciliation. {@code date} is a
 * plain STRING checked only for being a string — never parsed or reformatted.
 */
public final class ExternalTransaction {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private ExternalTransaction() {}

  public static ExternalTransaction create() {
    return new ExternalTransaction();
  }

  public ExternalTransaction id(String id) {
    fields.put("id", id);
    return this;
  }

  public ExternalTransaction amount(Number amount) {
    fields.put("amount", amount);
    return this;
  }

  public ExternalTransaction reference(String reference) {
    fields.put("reference", reference);
    return this;
  }

  public ExternalTransaction currency(String currency) {
    fields.put("currency", currency);
    return this;
  }

  public ExternalTransaction description(String description) {
    fields.put("description", description);
    return this;
  }

  /** Plain string date — sent to the wire verbatim. */
  public ExternalTransaction date(String date) {
    fields.put("date", date);
    return this;
  }

  public ExternalTransaction source(String source) {
    fields.put("source", source);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }
}
