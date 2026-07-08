package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Transaction document shape returned by {@code POST search/transactions}.
 * {@code precise_amount} is a STRING, {@code amount} a number; all timestamps
 * ({@code created_at}, {@code scheduled_for}, {@code inflight_expiry_date},
 * {@code effective_date}) are Unix-seconds NUMBERS. Optional response
 * convenience.
 */
public final class SearchTransactionDocument {

  private String id;
  private String transactionId;
  private Number amount;
  private String amountString;
  private String preciseAmount;
  private Number precision;
  private String source;
  private String destination;
  private String reference;
  private String description;
  private String currency;
  private String status;
  private String hash;
  private String parentTransaction;
  private Boolean atomic;
  private Boolean inflight;
  private Boolean allowOverdraft;
  private Number overdraftLimit;
  private Boolean skipQueue;
  private Long createdAt;
  private Long scheduledFor;
  private Long inflightExpiryDate;
  private Long effectiveDate;
  private Map<String, Object> metaData;

  private SearchTransactionDocument() {}

  public static SearchTransactionDocument create() {
    return new SearchTransactionDocument();
  }

  public SearchTransactionDocument id(String id) {
    this.id = id;
    return this;
  }

  public SearchTransactionDocument transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public SearchTransactionDocument amount(Number amount) {
    this.amount = amount;
    return this;
  }

  public SearchTransactionDocument amountString(String amountString) {
    this.amountString = amountString;
    return this;
  }

  public SearchTransactionDocument preciseAmount(String preciseAmount) {
    this.preciseAmount = preciseAmount;
    return this;
  }

  public SearchTransactionDocument precision(Number precision) {
    this.precision = precision;
    return this;
  }

  public SearchTransactionDocument source(String source) {
    this.source = source;
    return this;
  }

  public SearchTransactionDocument destination(String destination) {
    this.destination = destination;
    return this;
  }

  public SearchTransactionDocument reference(String reference) {
    this.reference = reference;
    return this;
  }

  public SearchTransactionDocument description(String description) {
    this.description = description;
    return this;
  }

  public SearchTransactionDocument currency(String currency) {
    this.currency = currency;
    return this;
  }

  public SearchTransactionDocument status(String status) {
    this.status = status;
    return this;
  }

  public SearchTransactionDocument hash(String hash) {
    this.hash = hash;
    return this;
  }

  public SearchTransactionDocument parentTransaction(String parentTransaction) {
    this.parentTransaction = parentTransaction;
    return this;
  }

  public SearchTransactionDocument atomic(Boolean atomic) {
    this.atomic = atomic;
    return this;
  }

  public SearchTransactionDocument inflight(Boolean inflight) {
    this.inflight = inflight;
    return this;
  }

  public SearchTransactionDocument allowOverdraft(Boolean allowOverdraft) {
    this.allowOverdraft = allowOverdraft;
    return this;
  }

  public SearchTransactionDocument overdraftLimit(Number overdraftLimit) {
    this.overdraftLimit = overdraftLimit;
    return this;
  }

  public SearchTransactionDocument skipQueue(Boolean skipQueue) {
    this.skipQueue = skipQueue;
    return this;
  }

  /** Typesense-indexed creation time (Unix timestamp seconds). */
  public SearchTransactionDocument createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public SearchTransactionDocument scheduledFor(Long scheduledFor) {
    this.scheduledFor = scheduledFor;
    return this;
  }

  public SearchTransactionDocument inflightExpiryDate(Long inflightExpiryDate) {
    this.inflightExpiryDate = inflightExpiryDate;
    return this;
  }

  public SearchTransactionDocument effectiveDate(Long effectiveDate) {
    this.effectiveDate = effectiveDate;
    return this;
  }

  public SearchTransactionDocument metaData(Map<String, Object> metaData) {
    this.metaData = metaData;
    return this;
  }

  public String id() {
    return id;
  }

  public String transactionId() {
    return transactionId;
  }

  public Number amount() {
    return amount;
  }

  public String amountString() {
    return amountString;
  }

  public String preciseAmount() {
    return preciseAmount;
  }

  public Number precision() {
    return precision;
  }

  public String source() {
    return source;
  }

  public String destination() {
    return destination;
  }

  public String reference() {
    return reference;
  }

  public String description() {
    return description;
  }

  public String currency() {
    return currency;
  }

  public String status() {
    return status;
  }

  public String hash() {
    return hash;
  }

  public String parentTransaction() {
    return parentTransaction;
  }

  public Boolean atomic() {
    return atomic;
  }

  public Boolean inflight() {
    return inflight;
  }

  public Boolean allowOverdraft() {
    return allowOverdraft;
  }

  public Number overdraftLimit() {
    return overdraftLimit;
  }

  public Boolean skipQueue() {
    return skipQueue;
  }

  public Long createdAt() {
    return createdAt;
  }

  public Long scheduledFor() {
    return scheduledFor;
  }

  public Long inflightExpiryDate() {
    return inflightExpiryDate;
  }

  public Long effectiveDate() {
    return effectiveDate;
  }

  public Map<String, Object> metaData() {
    return metaData;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static SearchTransactionDocument fromJson(JsonNode node) {
    SearchTransactionDocument document = new SearchTransactionDocument();
    if (node == null || !node.isObject()) {
      return document;
    }
    document.id = text(node.get("id"));
    document.transactionId = text(node.get("transaction_id"));
    document.amount = number(node.get("amount"));
    document.amountString = text(node.get("amount_string"));
    document.preciseAmount = text(node.get("precise_amount"));
    document.precision = number(node.get("precision"));
    document.source = text(node.get("source"));
    document.destination = text(node.get("destination"));
    document.reference = text(node.get("reference"));
    document.description = text(node.get("description"));
    document.currency = text(node.get("currency"));
    document.status = text(node.get("status"));
    document.hash = text(node.get("hash"));
    document.parentTransaction = text(node.get("parent_transaction"));
    document.atomic = bool(node.get("atomic"));
    document.inflight = bool(node.get("inflight"));
    document.allowOverdraft = bool(node.get("allow_overdraft"));
    document.overdraftLimit = number(node.get("overdraft_limit"));
    document.skipQueue = bool(node.get("skip_queue"));
    document.createdAt = longValue(node.get("created_at"));
    document.scheduledFor = longValue(node.get("scheduled_for"));
    document.inflightExpiryDate = longValue(node.get("inflight_expiry_date"));
    document.effectiveDate = longValue(node.get("effective_date"));
    document.metaData = map(node.get("meta_data"));
    return document;
  }

  private static String text(JsonNode node) {
    return node == null || !node.isTextual() ? null : node.asText();
  }

  private static Number number(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.numberValue();
  }

  private static Boolean bool(JsonNode node) {
    return node == null || !node.isBoolean() ? null : node.booleanValue();
  }

  private static Long longValue(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.longValue();
  }

  private static Map<String, Object> map(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }
    return BlnkJson.mapper().convertValue(node, new TypeReference<Map<String, Object>>() {});
  }
}
