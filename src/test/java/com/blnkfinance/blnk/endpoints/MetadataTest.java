package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.UpdateMetadataData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@code Metadata.update}: request shape, validation, and error forwarding. */
@DisplayName("Metadata.update")
class MetadataTest {

  /** Valid update payload carrying a two-entry {@code meta_data} object. */
  private static final UpdateMetadataData validData =
      UpdateMetadataData.create().metaData(buildMetaDataMap());

  /** Response body with the same shape as {@link #validData}. */
  private static final ObjectNode mockResponse = buildMockResponse();

  private static Map<String, Object> buildMetaDataMap() {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("project_owner", "Acme LLC");
    meta.put("update_status", "Approved");
    return meta;
  }

  private static ObjectNode buildMockResponse() {
    ObjectNode meta = BlnkJson.objectNode();
    meta.put("project_owner", "Acme LLC");
    meta.put("update_status", "Approved");
    ObjectNode node = BlnkJson.objectNode();
    node.set("meta_data", meta);
    return node;
  }

  @Test
  @DisplayName("update POSTs {id}/metadata")
  void updatePostsIdMetadata() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", mockResponse);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Metadata metadata =
        new Metadata(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = metadata.update("ldg_test_123", validData);

    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "ldg_test_123/metadata", validData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(200, response.status());
    assertEquals(validData.toJson().get("meta_data"), response.data().get("meta_data"));
  }

  @Test
  @DisplayName("update rejects empty id")
  void updateRejectsEmptyId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", mockResponse);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Metadata metadata =
        new Metadata(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = metadata.update("", validData);

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("id is required", response.message());
  }

  @Test
  @DisplayName("update rejects invalid meta_data")
  void updateRejectsInvalidMetaData() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(200, "Success", mockResponse);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Metadata metadata =
        new Metadata(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response =
        metadata.update("ldg_test_123", UpdateMetadataData.create().metaData(null));

    assertEquals(0, capturedRequest.calls.size());
    assertEquals(400, response.status());
    assertEquals("meta_data must be a valid object", response.message());
  }

  @Test
  @DisplayName("update forwards API errors")
  void updateForwardsApiErrors() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        (endpoint, data, method, headers) -> new ApiResponse<>(404, "Entity not found", null);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Metadata metadata =
        new Metadata(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    ApiResponse<JsonNode> response = metadata.update("ldg_missing", validData);

    assertEquals(
        List.of(
            new CapturingRequest.Call("ldg_missing/metadata", validData.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(404, response.status());
  }
}
