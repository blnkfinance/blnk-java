package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.Blnk;
import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.BlnkTransport;
import com.blnkfinance.blnk.ServiceFactory;
import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.CreateHookData;
import com.blnkfinance.blnk.types.ListHooksOptions;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for the Hooks service: request shapes, validation failures, and error forwarding. */
class HooksTest {

  /** Returns a valid {@link CreateHookData} payload shared across tests. */
  private static CreateHookData validData() {
    return CreateHookData.create()
        .name("Pre-transaction validation")
        .url("https://api.example.com/validate")
        .type("PRE_TRANSACTION")
        .active(true)
        .timeout(30)
        .retryCount(3);
  }

  /** Returns a representative hook response body shared across tests. */
  private static ObjectNode mockResponse() {
    ObjectNode node = BlnkJson.objectNode();
    node.put("id", "hk_test_123");
    node.put("name", "Pre-transaction validation");
    node.put("url", "https://api.example.com/validate");
    node.put("type", "PRE_TRANSACTION");
    node.put("active", true);
    node.put("timeout", 30);
    node.put("retry_count", 3);
    node.put("created_at", "2026-06-12T04:50:00.000Z");
    node.put("last_run", "0001-01-01T00:00:00Z");
    node.put("last_success", false);
    return node;
  }

  /** Builds a request stub that always returns the given status, message, and data. */
  private static BlnkRequest respond(int status, String message, JsonNode data) {
    return (endpoint, d, method, headers) -> new ApiResponse<>(status, message, data);
  }

  private static Hooks hooks(CapturingRequest capturedRequest) {
    return new Hooks(
        capturedRequest, TestMocks.createMockLogger(), HttpClientUtils.FORMAT_RESPONSE);
  }

  @Nested
  @DisplayName("Hooks.create")
  class CreateHooks {

    @Test
    @DisplayName("create POSTs hooks")
    void createPostsHooks() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(201, "Success", mockResponse()));
      CreateHookData data = validData();

      ApiResponse<JsonNode> response = hooks(capturedRequest).create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("hooks", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(201, response.status());
      assertEquals("hk_test_123", response.data().get("id").asText());
      assertEquals("PRE_TRANSACTION", response.data().get("type").asText());
    }

    @Test
    @DisplayName("create returns 400 for invalid payload")
    void createReturns400ForInvalidPayload() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(201, "Success", mockResponse()));

      ApiResponse<JsonNode> response =
          hooks(capturedRequest).create(validData().type("INVALID"));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("type must be PRE_TRANSACTION or POST_TRANSACTION", response.message());
    }

    @Test
    @DisplayName("create forwards API errors")
    void createForwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(403, "hook management requires master key", null));
      CreateHookData data = validData();

      ApiResponse<JsonNode> response = hooks(capturedRequest).create(data);

      assertEquals(
          List.of(new CapturingRequest.Call("hooks", data.toJson(), "POST", null)),
          capturedRequest.calls);
      assertEquals(403, response.status());
    }
  }

  @Nested
  @DisplayName("Hooks.list")
  class ListHooks {

    @Test
    @DisplayName("list GETs hooks without type filter")
    void listGetsHooksWithoutTypeFilter() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(
              respond(200, "Success", BlnkJson.arrayNode().add(mockResponse())));

      ApiResponse<JsonNode> response = hooks(capturedRequest).list();

      assertEquals(
          List.of(new CapturingRequest.Call("hooks", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals(1, response.data().size());
      assertEquals("hk_test_123", response.data().get(0).get("id").asText());
    }

    @Test
    @DisplayName("list GETs hooks?type= when type provided")
    void listGetsHooksTypeWhenTypeProvided() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.arrayNode()));

      ApiResponse<JsonNode> response =
          hooks(capturedRequest).list(ListHooksOptions.create().type("POST_TRANSACTION"));

      assertEquals(
          List.of(new CapturingRequest.Call("hooks?type=POST_TRANSACTION", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
    }

    @Test
    @DisplayName("list returns 400 for invalid type")
    void listReturns400ForInvalidType() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", BlnkJson.arrayNode()));

      ApiResponse<JsonNode> response =
          hooks(capturedRequest).list(ListHooksOptions.create().type("INVALID"));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("type must be PRE_TRANSACTION or POST_TRANSACTION", response.message());
    }

    @Test
    @DisplayName("list forwards API errors")
    void listForwardsApiErrors() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(403, "hook management requires master key", null));

      ApiResponse<JsonNode> response = hooks(capturedRequest).list();

      assertEquals(
          List.of(new CapturingRequest.Call("hooks", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(403, response.status());
    }
  }

  @Nested
  @DisplayName("Hooks.get")
  class GetHook {

    @Test
    @DisplayName("get GETs hooks/{id}")
    void getGetsHooksId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response = hooks(capturedRequest).get("hk_test_123");

      assertEquals(
          List.of(new CapturingRequest.Call("hooks/hk_test_123", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals("hk_test_123", response.data().get("id").asText());
      assertEquals("PRE_TRANSACTION", response.data().get("type").asText());
    }

    @Test
    @DisplayName("get returns 400 for empty id")
    void getReturns400ForEmptyId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response = hooks(capturedRequest).get("");

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("hook id is required", response.message());
    }

    @Test
    @DisplayName("get forwards API errors")
    void getForwardsApiErrors() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(404, "hook not found", null));

      ApiResponse<JsonNode> response = hooks(capturedRequest).get("hk_missing");

      assertEquals(
          List.of(new CapturingRequest.Call("hooks/hk_missing", null, "GET", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
    }
  }

  @Nested
  @DisplayName("Hooks.update")
  class UpdateHook {

    /** Returns the update payload used by the tests in this group. */
    private CreateHookData updateData() {
      return CreateHookData.create()
          .name("Pre-transaction validation (updated)")
          .url("https://api.example.com/validate-v2")
          .type("PRE_TRANSACTION")
          .active(false)
          .timeout(45)
          .retryCount(5);
    }

    @Test
    @DisplayName("update PUTs hooks/{id}")
    void updatePutsHooksId() {
      CreateHookData updateData = updateData();
      ObjectNode merged = mockResponse();
      merged.setAll(updateData.toJson()); // base response overlaid with the update payload
      CapturingRequest capturedRequest = CapturingRequest.of(respond(200, "Success", merged));

      ApiResponse<JsonNode> response =
          hooks(capturedRequest).update("hk_test_123", updateData);

      assertEquals(
          List.of(
              new CapturingRequest.Call("hooks/hk_test_123", updateData.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertFalse(response.data().get("active").asBoolean());
      assertEquals(45, response.data().get("timeout").asInt());
    }

    @Test
    @DisplayName("update returns 400 for empty id")
    void updateReturns400ForEmptyId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response = hooks(capturedRequest).update("", updateData());

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("hook id is required", response.message());
    }

    @Test
    @DisplayName("update returns 400 for invalid payload")
    void updateReturns400ForInvalidPayload() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", mockResponse()));

      ApiResponse<JsonNode> response =
          hooks(capturedRequest).update("hk_test_123", updateData().timeout(0));

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("timeout must be a positive number", response.message());
    }

    @Test
    @DisplayName("update forwards API errors")
    void updateForwardsApiErrors() {
      CreateHookData updateData = updateData();
      CapturingRequest capturedRequest = CapturingRequest.of(respond(404, "hook not found", null));

      ApiResponse<JsonNode> response = hooks(capturedRequest).update("hk_missing", updateData);

      assertEquals(
          List.of(
              new CapturingRequest.Call("hooks/hk_missing", updateData.toJson(), "PUT", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
    }
  }

  @Nested
  @DisplayName("Hooks.delete")
  class DeleteHook {

    /** Returns the delete-confirmation body used by the tests in this group. */
    private ObjectNode deleteResponse() {
      ObjectNode node = BlnkJson.objectNode();
      node.put("message", "hook deleted successfully");
      return node;
    }

    @Test
    @DisplayName("delete DELETEs hooks/{id}")
    void deleteDeletesHooksId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", deleteResponse()));

      ApiResponse<JsonNode> response = hooks(capturedRequest).delete("hk_test_123");

      assertEquals(
          List.of(new CapturingRequest.Call("hooks/hk_test_123", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(200, response.status());
      assertEquals("hook deleted successfully", response.data().get("message").asText());
    }

    @Test
    @DisplayName("delete returns 400 for empty id")
    void deleteReturns400ForEmptyId() {
      CapturingRequest capturedRequest =
          CapturingRequest.of(respond(200, "Success", deleteResponse()));

      ApiResponse<JsonNode> response = hooks(capturedRequest).delete("");

      assertEquals(0, capturedRequest.calls.size());
      assertEquals(400, response.status());
      assertEquals("hook id is required", response.message());
    }

    @Test
    @DisplayName("delete forwards API errors")
    void deleteForwardsApiErrors() {
      CapturingRequest capturedRequest = CapturingRequest.of(respond(404, "hook not found", null));

      ApiResponse<JsonNode> response = hooks(capturedRequest).delete("hk_missing");

      assertEquals(
          List.of(new CapturingRequest.Call("hooks/hk_missing", null, "DELETE", null)),
          capturedRequest.calls);
      assertEquals(404, response.status());
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
          Map.<String, ServiceFactory>of("Hooks", Hooks::new),
          HttpClientUtils.FORMAT_RESPONSE,
          emptyBodyTransport);

      ApiResponse<JsonNode> response = blnk.hooks().delete("hk_test_123");

      assertEquals(200, response.status());
      assertEquals("Success", response.message());
      assertNull(response.data());
    }
  }
}
