package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Balance document shape returned by {@code POST search/balances}. Monetary
 * fields ({@code balance}, {@code credit_balance}, …) are STRINGS (minor-unit
 * values); timestamps are Unix-seconds NUMBERS. Optional response convenience.
 */
public final class SearchBalanceDocument {

  private String id;
  private String balanceId;
  private String balance;
  private String creditBalance;
  private String debitBalance;
  private String inflightBalance;
  private String inflightCreditBalance;
  private String inflightDebitBalance;
  private String currency;
  private Number precision;
  private String ledgerId;
  private String identityId;
  private String indicator;
  private Number version;
  private String allocationStrategy;
  private Boolean trackFundLineage;
  private Long inflightExpiresAt;
  private Long createdAt;
  private Map<String, Object> metaData;

  private SearchBalanceDocument() {}

  public static SearchBalanceDocument create() {
    return new SearchBalanceDocument();
  }

  public SearchBalanceDocument id(String id) {
    this.id = id;
    return this;
  }

  public SearchBalanceDocument balanceId(String balanceId) {
    this.balanceId = balanceId;
    return this;
  }

  public SearchBalanceDocument balance(String balance) {
    this.balance = balance;
    return this;
  }

  public SearchBalanceDocument creditBalance(String creditBalance) {
    this.creditBalance = creditBalance;
    return this;
  }

  public SearchBalanceDocument debitBalance(String debitBalance) {
    this.debitBalance = debitBalance;
    return this;
  }

  public SearchBalanceDocument inflightBalance(String inflightBalance) {
    this.inflightBalance = inflightBalance;
    return this;
  }

  public SearchBalanceDocument inflightCreditBalance(String inflightCreditBalance) {
    this.inflightCreditBalance = inflightCreditBalance;
    return this;
  }

  public SearchBalanceDocument inflightDebitBalance(String inflightDebitBalance) {
    this.inflightDebitBalance = inflightDebitBalance;
    return this;
  }

  public SearchBalanceDocument currency(String currency) {
    this.currency = currency;
    return this;
  }

  public SearchBalanceDocument precision(Number precision) {
    this.precision = precision;
    return this;
  }

  public SearchBalanceDocument ledgerId(String ledgerId) {
    this.ledgerId = ledgerId;
    return this;
  }

  public SearchBalanceDocument identityId(String identityId) {
    this.identityId = identityId;
    return this;
  }

  public SearchBalanceDocument indicator(String indicator) {
    this.indicator = indicator;
    return this;
  }

  public SearchBalanceDocument version(Number version) {
    this.version = version;
    return this;
  }

  public SearchBalanceDocument allocationStrategy(String allocationStrategy) {
    this.allocationStrategy = allocationStrategy;
    return this;
  }

  public SearchBalanceDocument trackFundLineage(Boolean trackFundLineage) {
    this.trackFundLineage = trackFundLineage;
    return this;
  }

  public SearchBalanceDocument inflightExpiresAt(Long inflightExpiresAt) {
    this.inflightExpiresAt = inflightExpiresAt;
    return this;
  }

  /** Typesense-indexed creation time (Unix timestamp seconds). */
  public SearchBalanceDocument createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public SearchBalanceDocument metaData(Map<String, Object> metaData) {
    this.metaData = metaData;
    return this;
  }

  public String id() {
    return id;
  }

  public String balanceId() {
    return balanceId;
  }

  public String balance() {
    return balance;
  }

  public String creditBalance() {
    return creditBalance;
  }

  public String debitBalance() {
    return debitBalance;
  }

  public String inflightBalance() {
    return inflightBalance;
  }

  public String inflightCreditBalance() {
    return inflightCreditBalance;
  }

  public String inflightDebitBalance() {
    return inflightDebitBalance;
  }

  public String currency() {
    return currency;
  }

  public Number precision() {
    return precision;
  }

  public String ledgerId() {
    return ledgerId;
  }

  public String identityId() {
    return identityId;
  }

  public String indicator() {
    return indicator;
  }

  public Number version() {
    return version;
  }

  public String allocationStrategy() {
    return allocationStrategy;
  }

  public Boolean trackFundLineage() {
    return trackFundLineage;
  }

  public Long inflightExpiresAt() {
    return inflightExpiresAt;
  }

  public Long createdAt() {
    return createdAt;
  }

  public Map<String, Object> metaData() {
    return metaData;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static SearchBalanceDocument fromJson(JsonNode node) {
    SearchBalanceDocument document = new SearchBalanceDocument();
    if (node == null || !node.isObject()) {
      return document;
    }
    document.id = text(node.get("id"));
    document.balanceId = text(node.get("balance_id"));
    document.balance = text(node.get("balance"));
    document.creditBalance = text(node.get("credit_balance"));
    document.debitBalance = text(node.get("debit_balance"));
    document.inflightBalance = text(node.get("inflight_balance"));
    document.inflightCreditBalance = text(node.get("inflight_credit_balance"));
    document.inflightDebitBalance = text(node.get("inflight_debit_balance"));
    document.currency = text(node.get("currency"));
    document.precision = number(node.get("precision"));
    document.ledgerId = text(node.get("ledger_id"));
    document.identityId = text(node.get("identity_id"));
    document.indicator = text(node.get("indicator"));
    document.version = number(node.get("version"));
    document.allocationStrategy = text(node.get("allocation_strategy"));
    document.trackFundLineage = bool(node.get("track_fund_lineage"));
    document.inflightExpiresAt = longValue(node.get("inflight_expires_at"));
    document.createdAt = longValue(node.get("created_at"));
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
