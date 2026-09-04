package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST transactions/bulk}: optional {@code atomic},
 * {@code inflight}, {@code run_async}, {@code skip_queue}, and {@code dry_run}
 * flags plus the list of {@code transactions}.
 *
 * <p>{@code transactions} items keep their raw date inputs; each item is run
 * through {@code TransactionSerialization.serializeCreateTransaction} by
 * {@code Transactions.createBulk} AFTER validation.
 */
public final class BulkTransactions {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private BulkTransactions() {}

  public static BulkTransactions create() {
    return new BulkTransactions();
  }

  public BulkTransactions atomic(boolean atomic) {
    fields.put("atomic", atomic);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public BulkTransactions atomic(Object atomic) {
    fields.put("atomic", atomic);
    return this;
  }

  public BulkTransactions inflight(boolean inflight) {
    fields.put("inflight", inflight);
    return this;
  }

  public BulkTransactions runAsync(boolean runAsync) {
    fields.put("run_async", runAsync);
    return this;
  }

  public BulkTransactions skipQueue(boolean skipQueue) {
    fields.put("skip_queue", skipQueue);
    return this;
  }

  /**
   * Preview the batch without writing anything. {@code run_async} is ignored;
   * {@code skip_queue} still selects cumulative vs independent projection.
   */
  public BulkTransactions dryRun(boolean dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  /** Untyped overload: accepts any value, letting non-boolean input reach the validator. */
  public BulkTransactions dryRun(Object dryRun) {
    fields.put("dry_run", dryRun);
    return this;
  }

  public BulkTransactions transactions(List<CreateTransactions> transactions) {
    List<Map<String, Object>> items = new ArrayList<>();
    for (CreateTransactions transaction : transactions) {
      items.add(transaction.toMap());
    }
    fields.put("transactions", items);
    return this;
  }

  /** Sets an arbitrary extra field; unknown keys pass validation and are sent to the API. */
  public BulkTransactions putAdditional(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /**
   * Pre-serialization wire view: raw date objects inside transaction items stay
   * wrapped as POJONodes (see {@link CreateTransactions#toJson()}).
   */
  public ObjectNode toJson() {
    return CreateTransactions.toPayloadJson(fields);
  }
}
