package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.BlnkClientOptions;
import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.ServiceFactory;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The standard mock kit shared by the service test suites: client options,
 * logger, service registry, request stubs, and a dummy transaction fixture.
 */
public final class TestMocks {

  private TestMocks() {}

  /** The id stamped by the success mock request. */
  public static final String LEDGER_ID = "123456";

  /**
   * Mock logger with prefixes {@code Mock Info: } / {@code Mock Error: } /
   * {@code Mock Debug: }. All levels — including error — print to stdout;
   * nothing goes to stderr.
   */
  public static BlnkLogger createMockLogger() {
    return new BlnkLogger() {
      @Override
      public void info(String message, Object... meta) {
        System.out.println("Mock Info: " + message + render(meta));
      }

      @Override
      public void error(String message, Object... meta) {
        System.out.println("Mock Error: " + message + render(meta));
      }

      @Override
      public void debug(String message, Object... meta) {
        System.out.println("Mock Debug: " + message + render(meta));
      }

      private String render(Object... meta) {
        StringBuilder sb = new StringBuilder();
        for (Object m : meta) {
          sb.append(' ').append(m);
        }
        return sb.toString();
      }
    };
  }

  /**
   * Options with base URL {@code http://mock-api.com}, a 5000ms timeout, and
   * the mock logger.
   */
  public static BlnkClientOptions createMockBlnkClientOptions() {
    return BlnkClientOptions.builder()
        .baseUrl("http://mock-api.com")
        .timeout(5000)
        .logger(createMockLogger())
        .build();
  }

  /**
   * Ledgers, LedgerBalances, and Transactions all map to the same mock class;
   * no other service name is registered.
   */
  public static Map<String, ServiceFactory> createMockServices() {
    Map<String, ServiceFactory> services = new LinkedHashMap<>();
    services.put("Ledgers", MockTransactionService::new);
    services.put("LedgerBalances", MockTransactionService::new);
    services.put("Transactions", MockTransactionService::new);
    return services;
  }

  public static BlnkRequest createMockBlnkRequest(boolean success) {
    return createMockBlnkRequest(success, null, 200);
  }

  public static BlnkRequest createMockBlnkRequest(boolean success, String throwError) {
    return createMockBlnkRequest(success, throwError, 200);
  }

  /**
   * Builds a stub {@link BlnkRequest}:
   * <ul>
   *   <li>{@code throwError} set → every call throws a
   *       {@code RuntimeException} with that message.</li>
   *   <li>success → returns {@code (status, "Success", data)} with
   *       {@code ledger_id: "123456"} stamped last, overwriting any caller
   *       value. Non-object data (null / multipart bodies) contributes no
   *       fields.</li>
   *   <li>otherwise → returns {@code (500, "Internal Server Error", null)}.</li>
   * </ul>
   */
  public static BlnkRequest createMockBlnkRequest(boolean success, String throwError, int status) {
    return (endpoint, data, method, headerOptions) -> {
      if (throwError != null) {
        throw new RuntimeException(throwError);
      }
      if (success) {
        ObjectNode mockData =
            data instanceof ObjectNode objectNode ? objectNode.deepCopy() : BlnkJson.objectNode();
        mockData.put("ledger_id", LEDGER_ID);
        return new ApiResponse<>(status, "Success", mockData);
      }
      return new ApiResponse<>(500, "Internal Server Error", null);
    };
  }

  /**
   * Dummy transaction fixture. Values are fixed except:
   * {@code transaction_id} carries a random 9-character base-36 suffix, and
   * {@code created_at} holds a live {@link Date} object rather than a string
   * (tests never serialize it).
   */
  public static ObjectNode createDummyTransactionResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("transaction_id", "txn_" + randomBase36(9));
    node.put("amount", 1000);
    node.put("precision", 2);
    node.put("precise_amount", 100000);
    node.put("reference", "REF12345");
    node.put("description", "Sample transaction description");
    node.put("currency", "USD");
    node.put("status", "INFLIGHT");
    node.put("hash", "0b9c25fb5b00d6c71cb4ca87026bf6dc316e63353d3330deb588bd0b3d74dcc0");
    node.put("parent_transaction", "");
    node.put("source", "source_12345");
    node.put("destination", "destination_67890");
    ArrayNode sources = BlnkJson.arrayNode();
    ObjectNode sourceLeg = BlnkJson.objectNode();
    sourceLeg.put("identifier", "account1");
    sourceLeg.put("distribution", "left");
    sources.add(sourceLeg);
    node.set("sources", sources);
    node.put("allow_overdraft", false);
    node.put("inflight", true);
    node.put("skip_queue", false);
    node.put("atomic", false);
    node.set("created_at", JsonNodeFactory.instance.pojoNode(new Date())); // a Date object, not a string
    node.put("scheduled_for", "0001-01-01T00:00:00Z");
    node.put("inflight_expiry_date", "0001-01-01T00:00:00Z");
    node.put("inflight_commit_date", "0001-01-01T00:00:00Z");
    node.set("meta_data", BlnkJson.objectNode());
    return node;
  }

  private static String randomBase36(int length) {
    String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
    }
    return sb.toString();
  }
}
