package com.blnkfinance.blnk.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link IdentitySerialization#serializeIdentityData}: dob
 * normalization and optional identity_id forwarding.
 */
@DisplayName("serializeIdentityData")
class IdentitySerializationTest {

  /** Builds an individual identity payload without dob or identity_id. */
  private static Map<String, Object> baseIndividual() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("identity_type", "individual");
    data.put("first_name", "Jane");
    data.put("last_name", "Doe");
    data.put("gender", "female");
    data.put("nationality", "US");
    data.put("email_address", "jane@example.com");
    data.put("phone_number", "+1234567890");
    data.put("category", "customer");
    data.put("street", "123 Main St");
    data.put("country", "USA");
    data.put("state", "NY");
    data.put("post_code", "10001");
    data.put("city", "New York");
    return data;
  }

  @Test
  @DisplayName("passes through ISO dob string")
  void passesThroughIsoDobString() {
    Map<String, Object> data = baseIndividual();
    data.put("dob", "1990-01-15T00:00:00Z");

    Map<String, Object> payload = IdentitySerialization.serializeIdentityData(data);

    assertEquals("1990-01-15T00:00:00Z", payload.get("dob"));
  }

  @Test
  @DisplayName("serializes Date dob to ISO string without milliseconds")
  void serializesDateDobToIso8601WithoutMilliseconds() {
    Map<String, Object> data = baseIndividual();
    data.put("dob", Date.from(Instant.parse("1990-01-15T00:00:00.000Z")));

    Map<String, Object> payload = IdentitySerialization.serializeIdentityData(data);

    assertEquals("1990-01-15T00:00:00Z", payload.get("dob"));
  }

  @Test
  @DisplayName("forwards optional identity_id")
  void forwardsOptionalIdentityId() {
    Map<String, Object> data = baseIndividual();
    data.put("identity_id", "idt_11111111-1111-4111-8111-111111111111");
    data.put("dob", "1990-01-15T00:00:00Z");

    Map<String, Object> payload = IdentitySerialization.serializeIdentityData(data);

    assertEquals("idt_11111111-1111-4111-8111-111111111111", payload.get("identity_id"));
  }
}
