package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options for {@code GET balances/{balance_id}/at}. {@code timestamp} is a
 * plain STRING taken verbatim and URL-encoded — no date parsing, no
 * reformatting. Never sent as a request body.
 */
public final class GetBalanceAtRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private GetBalanceAtRequest() {}

  public static GetBalanceAtRequest create() {
    return new GetBalanceAtRequest();
  }

  /** ISO 8601 timestamp (RFC3339), e.g. {@code 2025-02-24T08:55:26Z}. */
  public GetBalanceAtRequest timestamp(String timestamp) {
    fields.put("timestamp", timestamp);
    return this;
  }

  /** Optional boolean flag; typed {@code Object} and never validated by this endpoint. */
  public GetBalanceAtRequest fromSource(Object fromSource) {
    fields.put("from_source", fromSource);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** JSON view (unused on the wire — options travel in the query string only). */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
