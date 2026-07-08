package com.blnkfinance.blnk.types;

import com.blnkfinance.blnk.util.DateSerialization;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST transactions}.
 *
 * <p>Date-input fields ({@code inflight_expiry_date}, {@code inflight_commit_date},
 * {@code scheduled_for}, {@code effective_date}) are stored RAW (String or
 * {@code Date}/{@code Instant}/{@code OffsetDateTime}/{@code ZonedDateTime}) so
 * validation runs on the raw value; {@link #toJson()} keeps raw date objects as
 * POJONodes which {@code TransactionSerialization.serializeCreateTransaction}
 * replaces with RFC3339 strings BEFORE the request call.
 *
 * <p>Unset fields are omitted from the serialized body entirely; wire names
 * are snake_case; field order in the body follows the setter call order.
 */
public final class CreateTransactions {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private CreateTransactions() {}

  public static CreateTransactions create() {
    return new CreateTransactions();
  }

  public CreateTransactions amount(double amount) {
    fields.put("amount", amount);
    return this;
  }

  public CreateTransactions preciseAmount(double preciseAmount) {
    fields.put("precise_amount", preciseAmount);
    return this;
  }

  public CreateTransactions preciseAmount(String preciseAmount) {
    fields.put("precise_amount", preciseAmount);
    return this;
  }

  public CreateTransactions precision(double precision) {
    fields.put("precision", precision);
    return this;
  }

  public CreateTransactions reference(String reference) {
    fields.put("reference", reference);
    return this;
  }

  public CreateTransactions description(String description) {
    fields.put("description", description);
    return this;
  }

  public CreateTransactions currency(String currency) {
    fields.put("currency", currency);
    return this;
  }

  public CreateTransactions rate(double rate) {
    fields.put("rate", rate);
    return this;
  }

  public CreateTransactions source(String source) {
    fields.put("source", source);
    return this;
  }

  public CreateTransactions sources(List<MultipleSourcesT> sources) {
    fields.put("sources", legsToMaps(sources));
    return this;
  }

  public CreateTransactions destinations(List<MultipleSourcesT> destinations) {
    fields.put("destinations", legsToMaps(destinations));
    return this;
  }

  public CreateTransactions destination(String destination) {
    fields.put("destination", destination);
    return this;
  }

  public CreateTransactions inflight(boolean inflight) {
    fields.put("inflight", inflight);
    return this;
  }

  /** Raw date input: String or Date/Instant/OffsetDateTime/ZonedDateTime. */
  public CreateTransactions inflightExpiryDate(Object inflightExpiryDate) {
    fields.put("inflight_expiry_date", inflightExpiryDate);
    return this;
  }

  /** Raw date input: String or Date/Instant/OffsetDateTime/ZonedDateTime. */
  public CreateTransactions inflightCommitDate(Object inflightCommitDate) {
    fields.put("inflight_commit_date", inflightCommitDate);
    return this;
  }

  /** Raw date input: String or Date/Instant/OffsetDateTime/ZonedDateTime. */
  public CreateTransactions scheduledFor(Object scheduledFor) {
    fields.put("scheduled_for", scheduledFor);
    return this;
  }

  /** Raw date input: String or Date/Instant/OffsetDateTime/ZonedDateTime. */
  public CreateTransactions effectiveDate(Object effectiveDate) {
    fields.put("effective_date", effectiveDate);
    return this;
  }

  public CreateTransactions skipQueue(boolean skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  public CreateTransactions atomic(boolean atomic) {
    fields.put("atomic", atomic);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public CreateTransactions atomic(Object atomic) {
    fields.put("atomic", atomic);
    return this;
  }

  public CreateTransactions allowOverdraft(boolean allowOverdraft) {
    fields.put("allow_overdraft", allowOverdraft);
    return this;
  }

  public CreateTransactions metaData(Map<String, Object> metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-object input reach the validator. */
  public CreateTransactions metaData(Object metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public CreateTransactions putAdditional(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /**
   * Pre-serialization wire view: raw date objects stay wrapped as POJONodes so
   * {@code TransactionSerialization.serializeCreateTransaction} can convert
   * them; {@code BlnkJson.toObjectNode} deliberately does not convert
   * temporals.
   */
  public ObjectNode toJson() {
    return toPayloadJson(fields);
  }

  private static List<Map<String, Object>> legsToMaps(List<MultipleSourcesT> legs) {
    List<Map<String, Object>> maps = new ArrayList<>();
    for (MultipleSourcesT leg : legs) {
      maps.add(leg.toMap());
    }
    return maps;
  }

  /**
   * Package-shared field-map to ObjectNode conversion that preserves raw date
   * objects as POJONodes (recursively — bulk transaction items carry their own
   * date fields). Everything else follows {@link BlnkJson#toJsonValue}.
   */
  static ObjectNode toPayloadJson(Map<String, ?> map) {
    ObjectNode node = BlnkJson.objectNode();
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      node.set(entry.getKey(), toPayloadValue(entry.getValue()));
    }
    return node;
  }

  private static JsonNode toPayloadValue(Object value) {
    if (DateSerialization.isDateObject(value)) {
      return JsonNodeFactory.instance.pojoNode(value);
    }
    if (value instanceof Map<?, ?> map) {
      ObjectNode node = BlnkJson.objectNode();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        node.set(String.valueOf(entry.getKey()), toPayloadValue(entry.getValue()));
      }
      return node;
    }
    if (value instanceof List<?> list) {
      ArrayNode node = BlnkJson.arrayNode();
      for (Object item : list) {
        node.add(toPayloadValue(item));
      }
      return node;
    }
    return BlnkJson.toJsonValue(value);
  }
}
