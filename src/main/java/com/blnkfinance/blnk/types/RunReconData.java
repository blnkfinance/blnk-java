package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload for {@code POST reconciliation/start}. NOTE:
 * {@code Reconciliation.run} performs ZERO validation — the object goes to
 * the wire exactly as built.
 */
public final class RunReconData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private RunReconData() {}

  public static RunReconData create() {
    return new RunReconData();
  }

  public RunReconData uploadId(String uploadId) {
    fields.put("upload_id", uploadId);
    return this;
  }

  public RunReconData matchingRuleIds(List<String> matchingRuleIds) {
    fields.put("matching_rule_ids", matchingRuleIds);
    return this;
  }

  public RunReconData dryRun(Boolean dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /** One of {@code one_to_one, one_to_many, many_to_one}. */
  public RunReconData strategy(String strategy) {
    fields.put("strategy", strategy);
    return this;
  }

  public RunReconData groupingCriteria(String groupingCriteria) {
    fields.put("grouping_criteria", groupingCriteria);
    return this;
  }

  /** Field map view; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /** Wire body — forwarded unmodified by {@code Reconciliation.run}. */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
