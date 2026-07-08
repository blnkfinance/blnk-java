package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Balance monitor payload: a required {@code condition} and
 * {@code balance_id} plus optional {@code description} and
 * {@code call_back_url}.
 *
 * <p>Unset fields are omitted from the serialized body entirely; wire names
 * are snake_case. Note: {@code MonitorData} intentionally has NO
 * {@code meta_data} field; do not add one.
 */
public final class MonitorData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private MonitorData() {}

  public static MonitorData create() {
    return new MonitorData();
  }

  public MonitorData condition(MonitorCondition condition) {
    fields.put("condition", condition == null ? null : condition.toMap());
    return this;
  }

  public MonitorData description(String description) {
    fields.put("description", description);
    return this;
  }

  public MonitorData balanceId(String balanceId) {
    fields.put("balance_id", balanceId);
    return this;
  }

  public MonitorData callBackUrl(String callBackUrl) {
    fields.put("call_back_url", callBackUrl);
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
