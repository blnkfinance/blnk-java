package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.types.BlnkJson;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The sample {@code POST /transactions} response, with values taken verbatim
 * from the Blnk Core API reference. Note: it deliberately carries no
 * {@code rate} or {@code effective_date} field, and {@code created_at} keeps
 * its nanosecond precision.
 */
public final class CoreCreateTransactionResponse {

  private CoreCreateTransactionResponse() {}

  /** Fresh node per call so tests can override fields safely. */
  public static ObjectNode coreCreateTransactionReferenceResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("amount", 1250.34);
    node.put("precision", 100);
    node.put("precise_amount", 125034);
    node.put("transaction_id", "txn_c4e70eb8-e4d6-4e04-a2e2-92a43b969e0c");
    node.put("parent_transaction", "");
    node.put("source", "bln_f344b673-e855-4bda-b769-3e94a02c1941");
    node.put("destination", "bln_d5cbde84-d20a-485b-8ce8-6677d782c3a1");
    node.put("reference", "ref_2ye281ewiu-1e17-dh17-eh18728hd245");
    node.put("currency", "USD");
    node.put("description", "Card payment on Stripe");
    node.put("status", "QUEUED");
    node.put("hash", "0b9c25fb5b00d6c71cb4ca87026bf6dc316e63353d3330deb588bd0b3d74dcc0");
    node.put("allow_overdraft", false);
    node.put("inflight", false);
    node.put("created_at", "2024-11-26T09:33:35.265582042Z");
    node.put("scheduled_for", "0001-01-01T00:00:00Z");
    node.put("inflight_expiry_date", "0001-01-01T00:00:00Z");
    node.put("inflight_commit_date", "0001-01-01T00:00:00Z");
    return node;
  }
}
