package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options for {@code GET balances/{balance_id}}. Never sent as a body:
 * {@code from_source} only toggles the {@code ?from_source=true} query flag.
 */
public final class GetBalanceRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private GetBalanceRequest() {}

  public static GetBalanceRequest create() {
    return new GetBalanceRequest();
  }

  /** Optional boolean flag; typed {@code Object} so non-boolean values remain representable. */
  public GetBalanceRequest fromSource(Object fromSource) {
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
