package com.blnkfinance.blnk.endpoints;

import com.blnkfinance.blnk.BlnkLogger;
import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.testsupport.CapturingRequest;
import com.blnkfinance.blnk.testsupport.TestMocks;
import com.blnkfinance.blnk.types.ApiResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.blnkfinance.blnk.types.IdentityData;
import com.blnkfinance.blnk.util.HttpClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for the Identity service: create flows, dob serialization, and validation. */
@DisplayName("Identity")
class IdentityTest {

  /** Returns the full organization payload shared by several tests. */
  private static IdentityData fullOrganizationData() {
    return IdentityData.create()
        .category("test")
        .identityType("organization")
        .city("test")
        .country("test")
        .emailAddress("test@test.com")
        .organizationName("test org")
        .state("test")
        .postCode("test")
        .street("test")
        .phoneNumber("1234567890");
  }

  @Test
  @DisplayName("Create Organization Identity")
  void createOrganizationIdentity() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);

    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    IdentityData data = fullOrganizationData();

    ApiResponse<JsonNode> response = identity.create(data);

    assertEquals(
        List.of(new CapturingRequest.Call("identities", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
    assertEquals(
        data.toJson().get("identity_type").asText(),
        response.data().get("identity_type").asText());
  }

  @Test
  @DisplayName("Create accepts minimal organization payload")
  void createAcceptsMinimalOrganizationPayload() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    IdentityData data = IdentityData.create().identityType("organization");

    ApiResponse<JsonNode> response = identity.create(data);

    assertEquals(
        List.of(new CapturingRequest.Call("identities", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
  }

  @Test
  @DisplayName("Create accepts minimal individual payload")
  void createAcceptsMinimalIndividualPayload() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);
    IdentityData data = IdentityData.create().identityType("individual");

    ApiResponse<JsonNode> response = identity.create(data);

    assertEquals(
        List.of(new CapturingRequest.Call("identities", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(201, response.status());
  }

  @Test
  @DisplayName("it should handle errors thrown during creation")
  void itShouldHandleErrorsThrownDuringCreation() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest =
        TestMocks.createMockBlnkRequest(false, "Error creating identity", 500);

    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    IdentityData data = fullOrganizationData();

    ApiResponse<JsonNode> response = identity.create(data);

    // The request WAS invoked (validation passed), then threw → HandleError.
    assertEquals(
        List.of(new CapturingRequest.Call("identities", data.toJson(), "POST", null)),
        capturedRequest.calls);
    assertEquals(500, response.status());
    assertNull(response.data());
  }

  @Test
  @DisplayName("Create forwards identity_id and ISO dob")
  void createForwardsIdentityIdAndIsoDob() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    IdentityData data =
        IdentityData.create()
            .identityId("idt_11111111-1111-4111-8111-111111111111")
            .category("customer")
            .identityType("individual")
            .firstName("Jane")
            .lastName("Doe")
            .gender("female")
            .dob("1990-01-15T00:00:00Z")
            .nationality("US")
            .city("New York")
            .country("USA")
            .emailAddress("jane@example.com")
            .state("NY")
            .postCode("10001")
            .street("123 Main St")
            .phoneNumber("1234567890");

    identity.create(data);

    // A dob already given as an ISO-8601 string is forwarded unchanged.
    Map<String, Object> expected = data.toMap();
    expected.put("dob", "1990-01-15T00:00:00Z");
    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities", BlnkJson.toObjectNode(expected), "POST", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("Create serializes Date dob")
  void createSerializesDateDob() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true, null, 201);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    IdentityData data =
        IdentityData.create()
            .category("customer")
            .identityType("individual")
            .firstName("Jane")
            .lastName("Doe")
            .gender("female")
            .dob(Date.from(Instant.parse("1990-01-15T00:00:00.000Z")))
            .nationality("US")
            .city("New York")
            .country("USA")
            .emailAddress("jane@example.com")
            .state("NY")
            .postCode("10001")
            .street("123 Main St")
            .phoneNumber("1234567890");

    identity.create(data);

    // A Date dob is serialized as UTC ISO-8601 without fractional seconds —
    // the timestamp format Blnk Core expects.
    Map<String, Object> expected = data.toMap();
    expected.put("dob", "1990-01-15T00:00:00Z");
    assertEquals(
        List.of(
            new CapturingRequest.Call(
                "identities", BlnkJson.toObjectNode(expected), "POST", null)),
        capturedRequest.calls);
  }

  @Test
  @DisplayName("Create rejects invalid identity_id")
  void createRejectsInvalidIdentityId() {
    BlnkLogger mockLogger = TestMocks.createMockLogger();
    BlnkRequest thirdPartyRequest = TestMocks.createMockBlnkRequest(true);
    CapturingRequest capturedRequest = CapturingRequest.of(thirdPartyRequest);
    Identity identity =
        new Identity(capturedRequest, mockLogger, HttpClientUtils.FORMAT_RESPONSE);

    IdentityData data =
        IdentityData.create()
            .identityId("user_123")
            .category("customer")
            .identityType("individual")
            .firstName("Jane")
            .lastName("Doe")
            .gender("female")
            .dob("1990-01-15T00:00:00Z")
            .nationality("US")
            .city("New York")
            .country("USA")
            .emailAddress("jane@example.com")
            .state("NY")
            .postCode("10001")
            .street("123 Main St")
            .phoneNumber("1234567890");

    ApiResponse<JsonNode> response = identity.create(data);

    assertEquals(List.of(), capturedRequest.calls);
    assertEquals(400, response.status());
    assertEquals(
        "identity_id must start with idt_ followed by a valid UUID", response.message());
  }
}
