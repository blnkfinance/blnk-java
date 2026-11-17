package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Options for {@code GET balances/{balance_id}}. Never sent as a body:
 * {@code from_source}, {@code with_queued} and {@code include} are query
 * parameters only.
 */
public final class GetBalanceRequest {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private GetBalanceRequest() {}

  public static GetBalanceRequest create() {
    return new GetBalanceRequest();
  }

  /** {@code include=identity}: expands the balance's {@code identity} object. */
  public static final String INCLUDE_IDENTITY = "identity";

  /** {@code include=ledger}: expands the balance's {@code ledger} object. */
  public static final String INCLUDE_LEDGER = "ledger";

  /** Every {@code include} value Core understands on this route. */
  public static final List<String> INCLUDE_VALUES = List.of(INCLUDE_IDENTITY, INCLUDE_LEDGER);

  /**
   * Related objects to expand in the response, sent as repeated
   * {@code include} query parameters ({@code ?include=identity&include=ledger}).
   * Use {@link #INCLUDE_IDENTITY} and {@link #INCLUDE_LEDGER}; any other value
   * is rejected with a {@code 400} before a request is sent.
   */
  public GetBalanceRequest include(String... include) {
    fields.put("include", include == null ? null : new ArrayList<>(Arrays.asList(include)));
    return this;
  }

  /**
   * Optional boolean flag; typed {@code Object} so non-boolean values remain representable.
   *
   * @deprecated Core's {@code GET balances/{id}} ignores {@code from_source};
   *     it only applies to the historical balance route, which
   *     {@code LedgerBalances.getAt} already covers. The flag is still sent
   *     so existing behaviour is unchanged.
   */
  @Deprecated
  public GetBalanceRequest fromSource(Object fromSource) {
    fields.put("from_source", fromSource);
    return this;
  }

  /** Optional boolean flag; typed {@code Object} so non-boolean values remain representable. */
  public GetBalanceRequest withQueued(Object withQueued) {
    fields.put("with_queued", withQueued);
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
