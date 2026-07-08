package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code POST balances}.
 *
 * <p>Unset fields are omitted from the serialized body entirely; wire names
 * are snake_case. {@code trackFundLineage} and {@code metaData} are typed
 * {@code Object} so invalid-type input can reach the validator's rejection
 * paths.
 */
public final class CreateLedgerBalance {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private CreateLedgerBalance() {}

  public static CreateLedgerBalance create() {
    return new CreateLedgerBalance();
  }

  public CreateLedgerBalance ledgerId(String ledgerId) {
    fields.put("ledger_id", ledgerId);
    return this;
  }

  public CreateLedgerBalance identityId(String identityId) {
    fields.put("identity_id", identityId);
    return this;
  }

  public CreateLedgerBalance currency(String currency) {
    fields.put("currency", currency);
    return this;
  }

  /** Optional boolean flag; typed {@code Object} so non-boolean input reaches the validator. */
  public CreateLedgerBalance trackFundLineage(Object trackFundLineage) {
    fields.put("track_fund_lineage", trackFundLineage);
    return this;
  }

  /** One of {@code FIFO}, {@code LIFO}, {@code PROPORTIONAL}. */
  public CreateLedgerBalance allocationStrategy(String allocationStrategy) {
    fields.put("allocation_strategy", allocationStrategy);
    return this;
  }

  /** Optional metadata; typed {@code Object} so non-object input reaches the validator. */
  public CreateLedgerBalance metaData(Object metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
