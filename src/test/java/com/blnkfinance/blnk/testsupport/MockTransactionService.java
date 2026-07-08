package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.FormatResponseFn;
import com.blnkfinance.blnk.types.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Stub transaction service used by client-level tests. Registered (via
 * {@link TestMocks#createMockServices()}) under the names Ledgers,
 * LedgerBalances, and Transactions — all three intentionally map to this one
 * class.
 */
public class MockTransactionService {

  @SuppressWarnings("unused")
  private final BlnkRequest request;
  @SuppressWarnings("unused")
  private final BlnkLogger logger;
  @SuppressWarnings("unused")
  private final FormatResponseFn formatResponse;

  public MockTransactionService(
      BlnkRequest request, BlnkLogger logger, FormatResponseFn formatResponse) {
    this.request = request;
    this.logger = logger;
    this.formatResponse = formatResponse;
  }

  /**
   * Echoes the caller's data merged with the dummy transaction fields; on key
   * collisions the dummy fields win over the caller's data.
   */
  public ApiResponse<JsonNode> create(ObjectNode data) {
    ObjectNode merged = data.deepCopy();
    merged.setAll(TestMocks.createDummyTransactionResponse());
    return new ApiResponse<>(200, "Success", merged);
  }

  /**
   * Returns the dummy transaction stamped with the given id and the caller's
   * update merged in; {@code status} is always overwritten with
   * {@code "COMMIT"} regardless of the status the caller requested.
   */
  public ApiResponse<JsonNode> updateStatus(String id, ObjectNode update) {
    ObjectNode merged = TestMocks.createDummyTransactionResponse();
    merged.put("transaction_id", id);
    merged.setAll(update);
    merged.put("status", "COMMIT");
    return new ApiResponse<>(200, "Success", merged);
  }

  /** Returns the dummy transaction stamped with the given id. */
  public ApiResponse<JsonNode> refund(String id) {
    ObjectNode merged = TestMocks.createDummyTransactionResponse();
    merged.put("transaction_id", id);
    return new ApiResponse<>(200, "Success", merged);
  }
}
