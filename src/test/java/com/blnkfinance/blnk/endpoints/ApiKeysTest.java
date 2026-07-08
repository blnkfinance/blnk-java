package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.BlnkTransport;
import com.blnkfinance.blnk.ServiceFactory;
import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.CreateApiKeyData;
import com.blnkfinance.blnk.types.DeleteApiKeyOptions;
import com.blnkfinance.blnk.types.ListApiKeysOptions;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for the ApiKeys service: request shapes, validation failures, and error forwarding. */
class ApiKeysTest {

  /** Returns a valid {@link CreateApiKeyData} payload shared across tests. */
  private static CreateApiKeyData validData() {
    return CreateApiKeyData.create()
        .name("Service Account")
        .owner("merchant_a")
        .scopes(List.of("ledgers:read", "balances:write"))
        .expiresAt("2026-03-11T00:00:00Z");
  }

  /** Returns a representative API-key response body shared across tests. */
  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("api_key_id", "api_key_test_123");
    node.put("key", "YVLIhuIplUzLRCcT9r7DQ_jsGKCXAn39JQ3n_o-Ll2Q=");
    node.put("name", "Service Account");
    node.put("owner_id", "merchant_a");
    node.set("scopes", BlnkJson.arrayNode().add("ledgers:read").add("balances:write"));
    node.put("expires_at", "2026-03-11T00:00:00Z");
    node.put("created_at", "2026-06-12T05:50:00.000Z");
    node.put("last_used_at", "0001-01-01T00:00:00Z");
    node.put("is_revoked", false);
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  private static ApiKeys apiKeys(CapturingRequest capturedRequest) {
    return new ApiKeys(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("ApiKeys.create")
  class CreateApiKey {

    @Test
    @DisplayName("create POSTs api-keys")
    void createPostsApiKeys() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(201, "Success", mockResponse()));
      CreateApiKeyData data = validData();

      ApiResponse<JsonNode> response = apiKeys(capturedRequest).create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("api-keys", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, response.status());
      assertEquals("api_key_test_123", response.data().get("api_key_id").asText());
      assertEquals(
          "YVLIhuIplUzLRCcT9r7DQ_jsGKCXAn39JQ3n_o-Ll2Q=",
          response.data().get("key").asText());
      assertEquals("merchant_a", response.data().get("owner_id").asText());
    }

    @Test
    @DisplayName("create returns 400 for invalid payload")
    void createReturns400ForInvalidPayload() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(201, "Success", mockResponse()));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest).create(validData().scopes(List.of()));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("at least one scope must be specified", response.message());
    }

    @Test
    @DisplayName("create forwards API errors")
    void createForwardsApiErrors() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(403, "forbidden", null));
      CreateApiKeyData data = validData();

      ApiResponse<JsonNode> response = apiKeys(capturedRequest).create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("api-keys", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(403, response.status());
    }
  }

  @Nested
  @DisplayName("ApiKeys.list")
  class ListApiKeys {

    /** Returns a list entry whose key field is a hash rather than the raw secret. */
    private ObjectNode listItem() {
      ObjectNode node = mockResponse();
      node.put("key", "$2a$10$hashedkeyvalue");
      node.set("scopes", BlnkJson.arrayNode().add("ledgers:read"));
      return node;
    }

    @Test
    @DisplayName("list GETs api-keys without owner filter")
    void listGetsApiKeysWithoutOwnerFilter() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.arrayNode().add(listItem())));

      ApiResponse<JsonNode> response = apiKeys(capturedRequest).list();

      assertEquals(
          List.of(new CapturingRequest.Call("api-keys", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals(1, response.data().size());
      assertEquals("api_key_test_123", response.data().get(0).get("api_key_id").asText());
    }

    @Test
    @DisplayName("list GETs api-keys?owner= when owner provided")
    void listGetsApiKeysOwnerWhenOwnerProvided() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.arrayNode()));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest).list(ListApiKeysOptions.create().owner("merchant_a"));

      assertEquals(
          List.of(new CapturingRequest.Call("api-keys?owner=merchant_a", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("list URL-encodes owner query param")
    void listUrlEncodesOwnerQueryParam() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.arrayNode()));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest)
              .list(ListApiKeysOptions.create().owner("merchant a&role=admin"));

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "api-keys?owner=merchant%20a%26role%3Dadmin", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("list returns 400 for empty owner")
    void listReturns400ForEmptyOwner() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.arrayNode()));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest).list(ListApiKeysOptions.create().owner(""));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("owner must be a non-empty string", response.message());
    }

    @Test
    @DisplayName("list forwards API errors")
    void listForwardsApiErrors() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(403, "forbidden", null));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest).list(ListApiKeysOptions.create().owner("merchant_a"));

      assertEquals(
          List.of(new CapturingRequest.Call("api-keys?owner=merchant_a", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(403, response.status());
    }
  }

  @Nested
  @DisplayName("ApiKeys.delete")
  class DeleteApiKey {

    @Test
    @DisplayName("delete DELETEs api-keys/{id}")
    void deleteDeletesApiKeysId() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(204, "Success", null));

      ApiResponse<JsonNode> response = apiKeys(capturedRequest).delete("api_key_test_123");

      assertEquals(
          List.of(
              new CapturingRequest.Call("api-keys/api_key_test_123", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(204, response.status());
      assertNull(response.data());
    }

    @Test
    @DisplayName("delete DELETEs api-keys/{id}?owner= when owner provided")
    void deleteDeletesApiKeysIdOwnerWhenOwnerProvided() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(204, "Success", null));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest)
              .delete("api_key_test_123", DeleteApiKeyOptions.create().owner("merchant_a"));

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "api-keys/api_key_test_123?owner=merchant_a", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(204, response.status());
    }

    @Test
    @DisplayName("delete URL-encodes id and owner")
    void deleteUrlEncodesIdAndOwner() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(204, "Success", null));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest)
              .delete("api/key?id=1", DeleteApiKeyOptions.create().owner("merchant a&role=admin"));

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "api-keys/api%2Fkey%3Fid%3D1?owner=merchant%20a%26role%3Dadmin",
                  null,
                  "DELETE",
                  null)),
          capturedRequest.calls);
      assertEquals(204, response.status());
    }

    @Test
    @DisplayName("delete returns 400 for empty id")
    void deleteReturns400ForEmptyId() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(204, "Success", null));

      ApiResponse<JsonNode> response = apiKeys(capturedRequest).delete("");

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("api key id is required", response.message());
    }

    @Test
    @DisplayName("delete returns 400 for empty owner")
    void deleteReturns400ForEmptyOwner() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(204, "Success", null));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest)
              .delete("api_key_test_123", DeleteApiKeyOptions.create().owner(""));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("owner must be a non-empty string", response.message());
    }

    @Test
    @DisplayName("delete forwards API errors")
    void deleteForwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(404, "API key not found", null));

      ApiResponse<JsonNode> response =
          apiKeys(capturedRequest)
              .delete("api_key_missing", DeleteApiKeyOptions.create().owner("merchant_a"));

      assertEquals(
          List.of(
              new CapturingRequest.Call(
                  "api-keys/api_key_missing?owner=merchant_a", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
    }

    @Test
    @DisplayName("Delete succeeds through request layer on 204 No Content")
    void deleteSucceedsThroughRequestLayerOn204NoContent() {
      BlnkTransport noContentTransport = (url, transportRequest) -> TransportResponse.builder()
          .ok(true)
          .status(204)
          .statusText("No Content")
          .json(() -> {
            throw new IllegalStateException("Unexpected end of JSON input");
          })
          .text(() -> "")
          .build();

      Blnk blnk = new Blnk(
          "test-key",
          TestMocks.createMockBlnkClientOptions(),
          Map.<String, ServiceFactory>of("ApiKeys", ApiKeys::new),
          HttpClientUtils.FORMAT_RESPONSE,
          noContentTransport);

      ApiResponse<JsonNode> response = blnk.apiKeys().delete("api_key_test_123");

      assertEquals(204, response.status());
      assertEquals("Success", response.message());
      assertNull(response.data());
    }

    @Test
    @DisplayName("Delete succeeds on 200 OK with empty body")
    void deleteSucceedsOn200OkWithEmptyBody() {
      BlnkTransport emptyBodyTransport = (url, transportRequest) -> TransportResponse.builder()
          .ok(true)
          .status(200)
          .statusText("OK")
          .json(() -> {
            throw new IllegalStateException("Unexpected end of JSON input");
          })
          .text(() -> "")
          .build();

      Blnk blnk = new Blnk(
          "test-key",
          TestMocks.createMockBlnkClientOptions(),
          Map.<String, ServiceFactory>of("ApiKeys", ApiKeys::new),
          HttpClientUtils.FORMAT_RESPONSE,
          emptyBodyTransport);

      ApiResponse<JsonNode> response = blnk.apiKeys().delete("api_key_test_123");

      assertEquals(200, response.status());
      assertEquals("Success", response.message());
      assertNull(response.data());
    }
  }
}
