package com.blnkfinance.blnk;

import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.CreateHookData;
import com.blnkfinance.blnk.types.Criteria;
import com.blnkfinance.blnk.types.DeleteApiKeyOptions;
import com.blnkfinance.blnk.types.DetokenizeIdentityData;
import com.blnkfinance.blnk.types.FilterCondition;
import com.blnkfinance.blnk.types.FilterParams;
import com.blnkfinance.blnk.types.GetBalanceAtRequest;
import com.blnkfinance.blnk.types.GetBalanceRequest;
import com.blnkfinance.blnk.types.IdentityData;
import com.blnkfinance.blnk.types.Matcher;
import com.blnkfinance.blnk.types.MonitorCondition;
import com.blnkfinance.blnk.types.MonitorData;
import com.blnkfinance.blnk.types.RefundTransactionRequest;
import com.blnkfinance.blnk.types.SearchParams;
import com.blnkfinance.blnk.types.TokenizeIdentityData;
import com.blnkfinance.blnk.types.UpdateBalanceIdentity;
import com.blnkfinance.blnk.types.UpdateLedger;
import com.blnkfinance.blnk.types.UpdateMetadataData;
import com.blnkfinance.blnk.types.UpdateTransactionStatus;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.blnkfinance.blnk.util.PathSegments;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Path-confinement tests against a real local HTTP server. The server records
 * the raw path of every request that reaches it, so these tests check what
 * actually goes over the wire, not what the SDK meant to build.
 *
 * <p>A caller-supplied value that is not a single path segment must produce an
 * error response and send nothing. Otherwise an app that forwards a
 * user-controlled ID (for example {@code transactions().refund(id)}) could
 * have its {@code X-Blnk-Key} sent to a different Core route.
 */
class PathSecurityTest {

  /** Values that must never reach the server as part of a path. */
  private static final List<String> HOSTILE = List.of(
      "../backup",
      "../search/reindex",
      "../api-keys/x",
      "%2e%2e/%2e%2e/secret",
      "%2E%2E",
      "..%2Fbackup",
      "ldg_1/../../backup",
      "..",
      ".",
      "a/b",
      "?",
      "#",
      "\\",
      "a\r\nb",
      "");

  private HttpServer server;
  private final List<String> received = Collections.synchronizedList(new ArrayList<>());

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", exchange -> {
      received.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath());
      byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("content-type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private String origin() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private Blnk coreClient() {
    return client(origin() + "/", null);
  }

  private Blnk proxyClient() {
    return client(origin() + "/proxy/", "inst_1");
  }

  private Blnk client(String baseUrl, String instanceId) {
    BlnkClientOptions.Builder builder =
        BlnkClientOptions.builder().baseUrl(baseUrl).retryCount(1).timeout(5000);
    if (instanceId != null) {
      builder.instanceId(instanceId);
    }
    return new Blnk(
        "test-key",
        builder.build().withLogger(TestMocks.createMockLogger()),
        ServiceRegistry.defaultServices(),
        HttpClientUtils.FORMAT_RESPONSE,
        new HttpTransport());
  }

  /** One ID-taking entry point: how to call it, and the path a valid id produces. */
  private record Endpoint(
      String name, String method, String validValue, String expectedPath,
      Function<Blnk, Function<String, ApiResponse<JsonNode>>> call) {}

  private static final String TS = "2025-02-24T08:55:26Z";

  private static List<Endpoint> endpoints() {
    List<Endpoint> list = new ArrayList<>();
    // Ledgers
    list.add(new Endpoint("ledgers.get", "GET", "ldg_1", "ledgers/ldg_1",
        b -> id -> b.ledgers().get(id)));
    list.add(new Endpoint("ledgers.update", "PUT", "ldg_1", "ledgers/ldg_1",
        b -> id -> b.ledgers().update(id, UpdateLedger.create().name("n"))));
    // Balances
    list.add(new Endpoint("ledgerBalances.get", "GET", "bln_1", "balances/bln_1",
        b -> id -> b.ledgerBalances().get(id, null)));
    list.add(new Endpoint("ledgerBalances.get(options)", "GET", "bln_1", "balances/bln_1",
        b -> id -> b.ledgerBalances().get(id, GetBalanceRequest.create().fromSource(true))));
    list.add(new Endpoint("ledgerBalances.getByIndicator(indicator)", "GET", "@World",
        "balances/indicator/%40World/currency/USD",
        b -> v -> b.ledgerBalances().getByIndicator(v, "USD")));
    list.add(new Endpoint("ledgerBalances.getByIndicator(currency)", "GET", "USD",
        "balances/indicator/%40World/currency/USD",
        b -> v -> b.ledgerBalances().getByIndicator("@World", v)));
    list.add(new Endpoint("ledgerBalances.updateIdentity", "PUT", "bln_1",
        "balances/bln_1/identity",
        b -> id -> b.ledgerBalances().updateIdentity(
            id, UpdateBalanceIdentity.create().identityId("idt_1"))));
    list.add(new Endpoint("ledgerBalances.getAt", "GET", "bln_1", "balances/bln_1/at",
        b -> id -> b.ledgerBalances().getAt(id, GetBalanceAtRequest.create().timestamp(TS))));
    list.add(new Endpoint("ledgerBalances.getLineage", "GET", "bln_1", "balances/bln_1/lineage",
        b -> id -> b.ledgerBalances().getLineage(id)));
    // Transactions
    list.add(new Endpoint("transactions.get", "GET", "txn_1", "transactions/txn_1",
        b -> id -> b.transactions().get(id)));
    list.add(new Endpoint("transactions.getLineage", "GET", "txn_1",
        "transactions/txn_1/lineage", b -> id -> b.transactions().getLineage(id)));
    list.add(new Endpoint("transactions.updateStatus", "PUT", "txn_1",
        "transactions/inflight/txn_1",
        b -> id -> b.transactions().updateStatus(
            id, UpdateTransactionStatus.create().status("commit"))));
    list.add(new Endpoint("transactions.refund", "POST", "txn_1", "refund-transaction/txn_1",
        b -> id -> b.transactions().refund(id)));
    list.add(new Endpoint("transactions.refund(options)", "POST", "txn_1",
        "refund-transaction/txn_1",
        b -> id -> b.transactions().refund(id, RefundTransactionRequest.create().skipQueue(true))));
    // Balance monitors
    list.add(new Endpoint("balanceMonitor.get", "GET", "mon_1", "balance-monitors/mon_1",
        b -> id -> b.balanceMonitor().get(id)));
    list.add(new Endpoint("balanceMonitor.update", "PUT", "mon_1", "balance-monitors/mon_1",
        b -> id -> b.balanceMonitor().update(id, monitorData())));
    list.add(new Endpoint("balanceMonitor.delete", "DELETE", "mon_1", "balance-monitors/mon_1",
        b -> id -> b.balanceMonitor().delete(id)));
    list.add(new Endpoint("balanceMonitor.listByBalanceId", "GET", "bln_1",
        "balance-monitors/balances/bln_1", b -> id -> b.balanceMonitor().listByBalanceId(id)));
    // Reconciliation
    list.add(new Endpoint("reconciliation.get", "GET", "recon_1", "reconciliation/recon_1",
        b -> id -> b.reconciliation().get(id)));
    list.add(new Endpoint("reconciliation.updateMatchingRule", "PUT", "rule_1",
        "reconciliation/matching-rules/rule_1",
        b -> id -> b.reconciliation().updateMatchingRule(id, matcher())));
    list.add(new Endpoint("reconciliation.deleteMatchingRule", "DELETE", "rule_1",
        "reconciliation/matching-rules/rule_1",
        b -> id -> b.reconciliation().deleteMatchingRule(id)));
    // Identities
    list.add(new Endpoint("identity.get", "GET", "idt_1", "identities/idt_1",
        b -> id -> b.identity().get(id)));
    list.add(new Endpoint("identity.update", "PUT", "idt_1", "identities/idt_1",
        b -> id -> b.identity().update(id, identityData())));
    list.add(new Endpoint("identity.delete", "DELETE", "idt_1", "identities/idt_1",
        b -> id -> b.identity().delete(id)));
    list.add(new Endpoint("identity.getTokenizedFields", "GET", "idt_1",
        "identities/idt_1/tokenized-fields", b -> id -> b.identity().getTokenizedFields(id)));
    list.add(new Endpoint("identity.tokenizeField(id)", "POST", "idt_1",
        "identities/idt_1/tokenize/FirstName",
        b -> id -> b.identity().tokenizeField(id, "FirstName")));
    list.add(new Endpoint("identity.tokenizeField(field)", "POST", "FirstName",
        "identities/idt_1/tokenize/FirstName",
        b -> f -> b.identity().tokenizeField("idt_1", f)));
    list.add(new Endpoint("identity.tokenize", "POST", "idt_1", "identities/idt_1/tokenize",
        b -> id -> b.identity().tokenize(
            id, TokenizeIdentityData.create().fields(List.of("FirstName")))));
    list.add(new Endpoint("identity.detokenizeField(id)", "GET", "idt_1",
        "identities/idt_1/detokenize/FirstName",
        b -> id -> b.identity().detokenizeField(id, "FirstName")));
    list.add(new Endpoint("identity.detokenizeField(field)", "GET", "FirstName",
        "identities/idt_1/detokenize/FirstName",
        b -> f -> b.identity().detokenizeField("idt_1", f)));
    list.add(new Endpoint("identity.detokenize", "POST", "idt_1", "identities/idt_1/detokenize",
        b -> id -> b.identity().detokenize(
            id, DetokenizeIdentityData.create().fields(List.of("FirstName")))));
    // Metadata, search, hooks, API keys
    list.add(new Endpoint("metadata.update", "POST", "ldg_1", "ldg_1/metadata",
        b -> id -> b.metadata().update(
            id, UpdateMetadataData.create().metaData(Map.of("k", "v")))));
    list.add(new Endpoint("search.search", "POST", "ledgers", "search/ledgers",
        b -> c -> b.search().search(SearchParams.create().q("x"), c)));
    list.add(new Endpoint("search.filter", "POST", "ledgers", "ledgers/filter",
        b -> c -> b.search().filter(filterParams(), c)));
    list.add(new Endpoint("hooks.get", "GET", "hk_1", "hooks/hk_1",
        b -> id -> b.hooks().get(id)));
    list.add(new Endpoint("hooks.update", "PUT", "hk_1", "hooks/hk_1",
        b -> id -> b.hooks().update(id, hookData())));
    list.add(new Endpoint("hooks.delete", "DELETE", "hk_1", "hooks/hk_1",
        b -> id -> b.hooks().delete(id)));
    list.add(new Endpoint("apiKeys.delete", "DELETE", "api_key_1", "api-keys/api_key_1",
        b -> id -> b.apiKeys().delete(id)));
    list.add(new Endpoint("apiKeys.delete(options)", "DELETE", "api_key_1",
        "api-keys/api_key_1",
        b -> id -> b.apiKeys().delete(id, DeleteApiKeyOptions.create().owner("owner_1"))));
    return list;
  }

  private static MonitorData monitorData() {
    return MonitorData.create()
        .balanceId("bln_1")
        .condition(MonitorCondition.create().field("debit_balance").operator("<").value(500)
            .precision(100));
  }

  private static Matcher matcher() {
    return Matcher.create().name("m").description("d")
        .criteria(List.of(Criteria.create().field("amount").operator("equals")));
  }

  private static IdentityData identityData() {
    return IdentityData.create().category("test").identityType("organization").city("test")
        .country("test").emailAddress("test@test.com").organizationName("test org")
        .state("test").postCode("test").street("test").phoneNumber("1234567890");
  }

  private static FilterParams filterParams() {
    return FilterParams.create().filters(
        List.of(FilterCondition.create().field("status").operator("eq").value("APPLIED")));
  }

  private static CreateHookData hookData() {
    return CreateHookData.create().name("h").url("https://example.com/h")
        .type("PRE_TRANSACTION").active(false).timeout(45).retryCount(5);
  }

  private static String show(String value) {
    return value.replace("\r", "\\r").replace("\n", "\\n");
  }

  @Test
  @DisplayName("Every ID-taking method rejects hostile values and sends nothing (Core and Cloud Proxy)")
  void everyIdTakingMethodRejectsHostileValues() {
    List<String> failures = new ArrayList<>();
    int checked = 0;
    for (String base : List.of("core", "proxy")) {
      Blnk blnk = base.equals("core") ? coreClient() : proxyClient();
      for (Endpoint endpoint : endpoints()) {
        for (String value : HOSTILE) {
          received.clear();
          ApiResponse<JsonNode> response;
          try {
            response = endpoint.call().apply(blnk).apply(value);
          } catch (RuntimeException thrown) {
            response = null;
          }
          checked++;
          if (!received.isEmpty()) {
            failures.add(base + " " + endpoint.name() + "(\"" + show(value) + "\") sent "
                + received);
          } else if (response != null && response.status() >= 200 && response.status() < 300) {
            failures.add(base + " " + endpoint.name() + "(\"" + show(value) + "\") returned "
                + response.status());
          }
        }
      }
    }
    assertTrue(failures.isEmpty(),
        failures.size() + " of " + checked + " hostile calls were not refused:\n  "
            + String.join("\n  ", failures));
  }

  @Test
  @DisplayName("The scanner's proof-of-concept calls are refused")
  void scannerProofOfConceptCallsAreRefused() {
    Blnk core = coreClient();
    Blnk proxy = proxyClient();

    ApiResponse<JsonNode> refund = core.transactions().refund("../search/reindex");
    assertEquals(List.of(), received, "refund(\"../search/reindex\") reached the server");
    assertEquals(400, refund.status());

    ApiResponse<JsonNode> identity = core.identity().get("../backup");
    assertEquals(List.of(), received, "identity.get(\"../backup\") reached the server");
    assertEquals(400, identity.status());

    ApiResponse<JsonNode> hook = core.hooks().delete("../api-keys/api_key_victim");
    assertEquals(List.of(), received, "hooks.delete(\"../api-keys/...\") reached the server");
    assertEquals(400, hook.status());

    ApiResponse<JsonNode> ledger = proxy.ledgers().get("%2e%2e/%2e%2e/secret");
    assertEquals(List.of(), received, "proxy ledgers.get(\"%2e%2e/%2e%2e/secret\") reached the server");
    assertEquals(400, ledger.status());
  }

  @Test
  @DisplayName("Valid IDs are unchanged on the wire (Core and Cloud Proxy)")
  void validIdsAreUnchangedOnTheWire() {
    for (String base : List.of("core", "proxy")) {
      Blnk blnk = base.equals("core") ? coreClient() : proxyClient();
      String prefix = base.equals("core") ? "/" : "/proxy/";
      for (Endpoint endpoint : endpoints()) {
        received.clear();
        ApiResponse<JsonNode> response = endpoint.call().apply(blnk).apply(endpoint.validValue());
        assertEquals(200, response.status(), base + " " + endpoint.name() + ": "
            + response.message());
        assertEquals(List.of(endpoint.method() + " " + prefix + endpoint.expectedPath()),
            received, base + " " + endpoint.name());
      }
    }
  }

  @Test
  @DisplayName("A space in an ID is sent as %20, not +")
  void spaceIsPercentEncoded() {
    coreClient().ledgers().get("ldg 1");
    assertEquals(List.of("GET /ledgers/ldg%201"), received);
  }

  @Test
  @DisplayName("getByReference escapes / and % but rejects . and ..")
  void getByReferenceEscapesSlashAndPercent() {
    Blnk blnk = coreClient();
    assertEquals(200, blnk.transactions().getByReference("order/2025/%41").status());
    assertEquals(List.of("GET /transactions/reference/order%2F2025%2F%2541"), received);

    received.clear();
    assertEquals(400, blnk.transactions().getByReference(".").status());
    assertEquals(400, blnk.transactions().getByReference("..").status());
    assertEquals(List.of(), received);
  }

  /**
   * References keep {@code %} (they are free-form), so a reference can carry
   * an encoding that only becomes {@code ..} after a second or third decode by
   * a proxy chain. None of these may reach the server.
   */
  private static final List<String> MULTI_ENCODED_REFERENCES = List.of(
      "%2e%2e/%2e%2e/search/reindex",
      "%2E%2e/%2e%2E/search/reindex",
      "%252e%252e/%252e%252e/search/reindex",
      "%252E%252e%252Fsearch%252Freindex",
      "%25252e%25252e/secret",
      "%2525252e%2525252E/secret",
      "..%2fsearch%2freindex",
      "a/%2e%2e/b",
      "%2e%2e",
      "%2e",
      ".%2e",
      "%2e%2e%3fx",
      "%2e%2e%23x",
      "%2e%2e;x");

  @Test
  @DisplayName("getByReference refuses references that decode to dot segments at any layer (Core and Cloud Proxy)")
  void getByReferenceRefusesMultiEncodedDotSegments() {
    List<String> failures = new ArrayList<>();
    for (String base : List.of("core", "proxy")) {
      Blnk blnk = base.equals("core") ? coreClient() : proxyClient();
      for (String reference : MULTI_ENCODED_REFERENCES) {
        received.clear();
        ApiResponse<JsonNode> response = blnk.transactions().getByReference(reference);
        if (!received.isEmpty() || response.status() != 400) {
          failures.add(base + " getByReference(\"" + reference + "\") -> " + response.status()
              + " sent " + received);
        }
      }
    }
    assertTrue(failures.isEmpty(), String.join("\n  ", failures));
  }

  @Test
  @DisplayName("getByReference still sends references whose % never decodes to a dot segment")
  void getByReferenceKeepsHarmlessPercent() {
    Blnk blnk = proxyClient();
    assertEquals(200, blnk.transactions().getByReference("100%").status());
    assertEquals(200, blnk.transactions().getByReference("a%2Fb").status());
    assertEquals(200, blnk.transactions().getByReference("v1.2..3").status());
    assertEquals(List.of(
        "GET /proxy/transactions/reference/100%25",
        "GET /proxy/transactions/reference/a%252Fb",
        "GET /proxy/transactions/reference/v1.2..3"), received);
  }

  @Test
  @DisplayName("The request layer refuses double- and triple-encoded dot segments")
  void requestLayerRefusesMultiEncodedDotSegments() {
    for (Blnk blnk : List.of(coreClient(), proxyClient())) {
      for (String endpoint : List.of(
          "transactions/reference/%252e%252e%252Fsearch%252Freindex",
          "ledgers/%252E%252e/x", "ledgers/%25252e%25252e/x")) {
        ApiResponse<JsonNode> response = blnk.request(endpoint, null, "GET", null);
        assertEquals(400, response.status(), endpoint);
      }
    }
    assertEquals(List.of(), received);
  }

  @Test
  @DisplayName("The request layer refuses encoded and plain dot segments on both bases")
  void requestLayerRefusesDotSegments() {
    for (Blnk blnk : List.of(coreClient(), proxyClient())) {
      for (String endpoint : List.of(
          "ledgers/%2e%2e/%2e%2e/secret", "ledgers/%2E%2E", "ledgers/../backup",
          "ledgers/./x", "ledgers/..%2fbackup", "..", "%2e%2e")) {
        ApiResponse<JsonNode> response = blnk.request(endpoint, null, "GET", null);
        assertEquals(400, response.status(), endpoint);
        assertEquals("endpoint escapes the configured base URL", response.message(), endpoint);
      }
    }
    assertEquals(List.of(), received);
  }

  @Test
  @DisplayName("PathSegments.segment rejects separators, dot segments and control characters")
  void pathSegmentHelper() {
    for (String value : HOSTILE) {
      assertThrows(IllegalArgumentException.class, () -> PathSegments.segment(value),
          show(value));
    }
    assertThrows(IllegalArgumentException.class, () -> PathSegments.segment(null));
    assertThrows(IllegalArgumentException.class, () -> PathSegments.segment("a\u007fb"));
    assertEquals("ldg_1", PathSegments.segment("ldg_1"));
    assertEquals("a%20b", PathSegments.segment("a b"));
    assertEquals("%40World", PathSegments.segment("@World"));
    assertEquals("..a", PathSegments.segment("..a"));
    assertEquals("a%2Fb%25", PathSegments.reference("a/b%"));
    assertFalse(PathSegments.hasDotSegment("ledgers/..a/x?q=../y"));
    assertTrue(PathSegments.hasDotSegment("ledgers/%2e%2e"));
    assertTrue(PathSegments.hasDotSegment("ledgers/%252e%252E"));
    assertFalse(PathSegments.hasDotSegment("transactions/reference/100%25"));
    // A value still changing after the decode bound is treated as unsafe.
    String deep = "x";
    for (int i = 0; i < 12; i++) {
      deep = deep.replace("%", "%25");
      deep = i == 0 ? "%78" : deep;
    }
    assertTrue(PathSegments.hasDotSegment("ledgers/" + deep));
    assertThrows(IllegalArgumentException.class, () -> PathSegments.reference("%252e%252e"));
  }
}
