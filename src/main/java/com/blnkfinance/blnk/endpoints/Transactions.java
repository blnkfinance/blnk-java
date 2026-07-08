package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BulkCommitInflightRequest;
import com.blnkfinance.blnk.types.BulkTransactions;
import com.blnkfinance.blnk.types.BulkVoidInflightRequest;
import com.blnkfinance.blnk.types.CreateTransactions;
import com.blnkfinance.blnk.types.RecoverQueueRequest;
import com.blnkfinance.blnk.types.RefundTransactionRequest;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.blnkfinance.blnk.util.UriEncoding;
import com.blnkfinance.blnk.util.ValueFormat;
import com.blnkfinance.blnk.util.Loggers;
import com.blnkfinance.blnk.util.TransactionSerialization;
import com.blnkfinance.blnk.validators.TransactionValidators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Endpoint service for transactions: creation (single and bulk), inflight
 * status updates, refunds, lookups, queue recovery, and lineage.
 *
 * <p>Every method validates first (a validator message yields
 * {@code formatResponse(400, msg, null)} with no HTTP call), then issues the
 * request; every thrown error is routed through {@code Loggers.handleError}
 * under the calling method's own name — methods never throw. No method passes
 * header options.
 */
public class Transactions {

  protected final BlnkRequest request;
  protected final BlnkLogger logger;
  protected final FormatResponseFn formatResponse;

  public Transactions(BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Records a transaction — {@code POST transactions}. Validates, serializes
   * the date fields to the timestamp format Blnk Core expects, then sends the
   * serialized payload (not the original).
   */
  public ApiResponse<JsonNode> create(CreateTransactions data) {
    try {
      String validatorResponse =
          TransactionValidators.validateCreateTransactions(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      ObjectNode payload = TransactionSerialization.serializeCreateTransaction(data.toJson());

      return request.call("transactions", payload, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "create");
    }
  }

  /**
   * Updates an inflight transaction — {@code PUT transactions/inflight/{id}}.
   * The body is forwarded as-is, never serialized; the id is neither
   * validated nor URL-encoded.
   */
  public ApiResponse<JsonNode> updateStatus(String id, UpdateTransactionStatus update) {
    try {
      String validatorResponse =
          TransactionValidators.validateUpdateTransactions(update == null ? null : update.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call("transactions/inflight/" + id, update.toJson(), "PUT", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "updateStatus");
    }
  }

  /** Refunds a transaction — {@code POST refund-transaction/{id}} with no options and no body. */
  public ApiResponse<JsonNode> refund(String id) {
    return refund(id, null);
  }

  /**
   * Refunds a transaction — {@code POST refund-transaction/{id}}. The
   * validator runs only when options are provided; passing {@code null} means
   * "no options" and sends no body. The body, when present, is forwarded
   * as-is; the id is neither validated nor URL-encoded.
   */
  public ApiResponse<JsonNode> refund(String id, RefundTransactionRequest options) {
    try {
      if (options != null) {
        String validatorResponse = TransactionValidators.validateRefundTransaction(options.toMap());
        if (validatorResponse != null) {
          return formatResponse.format(400, validatorResponse, null, null);
        }
      }
      return request.call(
          "refund-transaction/" + id, options == null ? null : options.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "refund");
    }
  }

  /**
   * Retrieves a transaction — {@code GET transactions/{transactionId}}. A null
   * or empty id is rejected; the id is interpolated raw, without
   * URL-encoding. No body.
   */
  public ApiResponse<JsonNode> get(String transactionId) {
    try {
      if (transactionId == null || transactionId.isEmpty()) {
        return formatResponse.format(400, "transaction id is required", null, null);
      }
      return request.call("transactions/" + transactionId, null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "get");
    }
  }

  /**
   * Retrieves a transaction by reference —
   * {@code GET transactions/reference/{reference}}. A null or empty reference
   * is rejected; the reference IS URL-encoded — the only URL-encoded path
   * segment in this class.
   */
  public ApiResponse<JsonNode> getByReference(String reference) {
    try {
      if (reference == null || reference.isEmpty()) {
        return formatResponse.format(400, "reference is required", null, null);
      }
      return request.call(
          "transactions/reference/" + UriEncoding.encodePathSegment(reference), null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getByReference");
    }
  }

  /**
   * Retrieves a transaction's lineage —
   * {@code GET transactions/{transactionId}/lineage}. A null or empty id is
   * rejected; the id is interpolated raw, without URL-encoding. No body.
   */
  public ApiResponse<JsonNode> getLineage(String transactionId) {
    try {
      if (transactionId == null || transactionId.isEmpty()) {
        return formatResponse.format(400, "transaction id is required", null, null);
      }
      return request.call("transactions/" + transactionId + "/lineage", null, "GET", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "getLineage");
    }
  }

  /** Recovers the transaction queue — {@code POST transactions/recover} with no options. */
  public ApiResponse<JsonNode> recoverQueue() {
    return recoverQueue(null);
  }

  /**
   * Recovers the transaction queue —
   * {@code POST transactions/recover[?threshold=…]}. The validator runs only
   * when options are provided; a set threshold goes into the query string,
   * URL-encoded. Note: validation trims the threshold before checking it, but
   * the query string is built from the original UNtrimmed value. No body.
   */
  public ApiResponse<JsonNode> recoverQueue(RecoverQueueRequest options) {
    try {
      if (options != null) {
        String validatorResponse = TransactionValidators.validateRecoverQueue(options.toMap());
        if (validatorResponse != null) {
          return formatResponse.format(400, validatorResponse, null, null);
        }
      }

      Object threshold = options == null ? null : options.toMap().get("threshold");
      String endpoint =
          ValueFormat.isTruthy(threshold)
              ? "transactions/recover?threshold=" + UriEncoding.encodePathSegment(String.valueOf(threshold))
              : "transactions/recover";

      return request.call(endpoint, null, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "recoverQueue");
    }
  }

  /**
   * Commits inflight transactions in bulk —
   * {@code POST transactions/inflight/bulk/commit}. The body is forwarded
   * as-is, never serialized.
   */
  public ApiResponse<JsonNode> bulkCommitInflight(BulkCommitInflightRequest data) {
    try {
      String validatorResponse =
          TransactionValidators.validateBulkCommitInflight(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call("transactions/inflight/bulk/commit", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "bulkCommitInflight");
    }
  }

  /**
   * Voids inflight transactions in bulk —
   * {@code POST transactions/inflight/bulk/void}. The body is forwarded
   * as-is, never serialized.
   */
  public ApiResponse<JsonNode> bulkVoidInflight(BulkVoidInflightRequest data) {
    try {
      String validatorResponse =
          TransactionValidators.validateBulkVoidInflight(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }
      return request.call("transactions/inflight/bulk/void", data.toJson(), "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "bulkVoidInflight");
    }
  }

  /**
   * Records transactions in bulk — {@code POST transactions/bulk}. Validates,
   * then sends the payload with each entry of {@code transactions} passed
   * through the same date serialization as {@link #create}; top-level flags
   * pass through untouched.
   */
  public ApiResponse<JsonNode> createBulk(BulkTransactions data) {
    try {
      String validatorResponse =
          TransactionValidators.validateBulkTransactions(data == null ? null : data.toMap());
      if (validatorResponse != null) {
        return formatResponse.format(400, validatorResponse, null, null);
      }

      ObjectNode payload = data.toJson();
      if (payload.get("transactions") instanceof ArrayNode transactionsArray) {
        for (int i = 0; i < transactionsArray.size(); i++) {
          if (transactionsArray.get(i) instanceof ObjectNode item) {
            transactionsArray.set(i, TransactionSerialization.serializeCreateTransaction(item));
          }
        }
      }

      return request.call("transactions/bulk", payload, "POST", null);
    } catch (RuntimeException error) {
      return Loggers.handleError(error, logger, formatResponse, "createBulk");
    }
  }
}
