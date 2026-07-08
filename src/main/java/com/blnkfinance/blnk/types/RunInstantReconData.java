package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload for {@code POST reconciliation/start-instant}. {@code dry_run} and
 * {@code grouping_criteria} are optional — unset keys are absent on the wire.
 */
public final class RunInstantReconData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private RunInstantReconData() {}

  public static RunInstantReconData create() {
    return new RunInstantReconData();
  }

  public RunInstantReconData externalTransactions(List<ExternalTransaction> externalTransactions) {
    fields.put("external_transactions", externalTransactions);
    return this;
  }

  /** One of {@code one_to_one, one_to_many, many_to_one}. */
  public RunInstantReconData strategy(String strategy) {
    fields.put("strategy", strategy);
    return this;
  }

  public RunInstantReconData dryRun(Boolean dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  public RunInstantReconData matchingRuleIds(List<String> matchingRuleIds) {
    fields.put("matching_rule_ids", matchingRuleIds);
    return this;
  }

  public RunInstantReconData groupingCriteria(String groupingCriteria) {
    fields.put("grouping_criteria", groupingCriteria);
    return this;
  }

  /**
   * Field map as passed to validators. {@link ExternalTransaction} elements
   * are converted to their own map views so the validator sees plain maps
   * throughout.
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>(fields);
    Object transactions = map.get("external_transactions");
    if (transactions instanceof List<?> list) {
      List<Object> converted = new ArrayList<>();
      for (Object item : list) {
        converted.add(
            item instanceof ExternalTransaction transaction ? transaction.toMap() : item);
      }
      map.put("external_transactions", converted);
    }
    return map;
  }

  /** Wire body — forwarded unmodified by {@code Reconciliation.runInstant}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
