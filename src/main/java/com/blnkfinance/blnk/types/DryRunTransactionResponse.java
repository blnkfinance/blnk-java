package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed view of a Core dry-run preview. This is not a recorded transaction:
 * there is no {@code transaction_id}, the reference is unused, and HTTP
 * {@code 200} with {@code would_apply: false} is a projected rejection, not
 * an error.
 *
 * <p>Optional convenience wrapper — endpoint methods still return
 * {@code ApiResponse<JsonNode>}.
 */
public final class DryRunTransactionResponse {

  private final ObjectNode json;

  private DryRunTransactionResponse(ObjectNode json) {
    this.json = json;
  }

  public static DryRunTransactionResponse fromJson(JsonNode json) {
    return new DryRunTransactionResponse(
        json instanceof ObjectNode objectNode ? objectNode.deepCopy() : BlnkJson.objectNode());
  }

  /** True when the body looks like a dry-run preview rather than a recorded transaction. */
  public static boolean isDryRun(JsonNode json) {
    JsonNode dryRun = json == null ? null : json.get("dry_run");
    return dryRun != null && dryRun.isBoolean() && dryRun.booleanValue();
  }

  public ObjectNode toJson() {
    return json.deepCopy();
  }

  public Boolean dryRun() {
    return bool("dry_run");
  }

  public Boolean wouldApply() {
    return bool("would_apply");
  }

  public DryRunRejection rejection() {
    return DryRunRejection.fromJson(json.get("rejection"));
  }

  public String status() {
    return text("status");
  }

  public String reference() {
    return text("reference");
  }

  public String currency() {
    return text("currency");
  }

  public Double amount() {
    return number("amount");
  }

  /** Minor-unit amount, typically a string so nothing is rounded. */
  public Object preciseAmount() {
    JsonNode node = json.get("precise_amount");
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isNumber()) {
      return node.asText();
    }
    return node;
  }

  public Double precision() {
    return number("precision");
  }

  /** Present on inflight previews only: {@code commit} or {@code void}. */
  public String operation() {
    return text("operation");
  }

  public List<DryRunBalanceProjection> balances() {
    JsonNode node = json.get("balances");
    if (node == null || !node.isArray()) {
      return Collections.emptyList();
    }
    List<DryRunBalanceProjection> balances = new ArrayList<>();
    for (JsonNode item : node) {
      DryRunBalanceProjection projection = DryRunBalanceProjection.fromJson(item);
      if (projection != null) {
        balances.add(projection);
      }
    }
    return Collections.unmodifiableList(balances);
  }

  /** Split-leg projections when the request used multiple sources or destinations. */
  public List<DryRunLegProjection> legs() {
    JsonNode node = json.get("legs");
    if (node == null || !node.isArray()) {
      return Collections.emptyList();
    }
    List<DryRunLegProjection> legs = new ArrayList<>();
    for (JsonNode item : node) {
      DryRunLegProjection leg = DryRunLegProjection.fromJson(item);
      if (leg != null) {
        legs.add(leg);
      }
    }
    return Collections.unmodifiableList(legs);
  }

  /**
   * Advisory messages that are not rejections, for example a currency mismatch
   * or legs being queued for independent async processing. A preview can carry
   * notes while {@code would_apply} is still {@code true}.
   */
  public List<String> notes() {
    return notesOf(json);
  }

  static List<String> notesOf(JsonNode json) {
    JsonNode node = json == null ? null : json.get("notes");
    if (node == null || !node.isArray()) {
      return Collections.emptyList();
    }
    List<String> notes = new ArrayList<>();
    for (JsonNode item : node) {
      if (item != null && !item.isNull()) {
        notes.add(item.asText());
      }
    }
    return Collections.unmodifiableList(notes);
  }

  private String text(String field) {
    JsonNode node = json.get(field);
    return node != null && node.isTextual() ? node.asText() : null;
  }

  private Double number(String field) {
    JsonNode node = json.get(field);
    return node != null && node.isNumber() ? node.doubleValue() : null;
  }

  private Boolean bool(String field) {
    JsonNode node = json.get(field);
    return node != null && node.isBoolean() ? node.booleanValue() : null;
  }
}
