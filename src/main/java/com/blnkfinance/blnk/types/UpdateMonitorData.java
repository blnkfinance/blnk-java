package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Body for {@code BalanceMonitor.updatePartial}: a partial update of a
 * balance monitor (Core 0.15.9+). Core keeps every field the request leaves
 * out, so set only what should change. Unset fields are not sent.
 *
 * <ul>
 *   <li>{@code description("")} clears the description. Leaving it unset
 *       keeps the stored one.</li>
 *   <li>A condition with only {@code value(0)} moves the threshold to zero.
 *       Leaving {@code value} unset keeps the stored threshold.</li>
 *   <li>Core ignores a {@code precision} change unless {@code value} is sent
 *       with it, and then replies with {@code "precision_unchanged": true}.</li>
 * </ul>
 *
 * <p>There is no {@code call_back_url}: Core ignores it on update, so the
 * callback URL cannot be changed after creation.
 *
 * <p>Core replies with {@code {"message": ..., "precision_unchanged": true}}
 * (the flag only when it applies), not with the monitor.
 */
public final class UpdateMonitorData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private UpdateMonitorData() {}

  public static UpdateMonitorData create() {
    return new UpdateMonitorData();
  }

  /** Moves the monitor to a different balance. */
  public UpdateMonitorData balanceId(String balanceId) {
    fields.put("balance_id", balanceId);
    return this;
  }

  /** New description; {@code ""} clears it. */
  public UpdateMonitorData description(String description) {
    fields.put("description", description);
    return this;
  }

  /** {@link MonitorTrigger#EDGE} or {@link MonitorTrigger#LEVEL}. */
  public UpdateMonitorData trigger(String trigger) {
    fields.put("trigger", trigger);
    return this;
  }

  /**
   * Condition fields to change. Only the fields set on {@code condition} are
   * sent; for example {@code MonitorCondition.create().value(250)} moves the
   * threshold and keeps the stored field, operator and precision.
   */
  public UpdateMonitorData condition(MonitorCondition condition) {
    fields.put("condition", condition == null ? null : condition.toMap());
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
