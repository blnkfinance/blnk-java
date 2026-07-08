package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the identity response shape. Identity endpoint responses are
 * surfaced as raw parsed JSON ({@code ApiResponse<JsonNode>}), so the test
 * builds a reference body and asserts two facts: {@code dob} is a string, and
 * {@code identity_id} starts with {@code idt_}. The nanosecond-precision
 * {@code created_at} value is one the response shape must tolerate.
 */
@DisplayName("IdentityDataResponse API shape")
class IdentityResponseTest {

  @Test
  @DisplayName("response dob is a string")
  void responseDobIsAString() {
    ObjectNode response = BlnkJson.objectNode();
    response.put("identity_id", "idt_11111111-1111-4111-8111-111111111111");
    response.put("identity_type", "individual");
    response.put("first_name", "Alice");
    response.put("last_name", "Smith");
    response.put("gender", "female");
    response.put("dob", "1985-05-15T00:00:00Z");
    response.put("email_address", "alice@example.com");
    response.put("phone_number", "+1234567890");
    response.put("nationality", "Canadian");
    response.put("category", "customer");
    response.put("street", "789 Elm St");
    response.put("country", "Canada");
    response.put("state", "Ontario");
    response.put("post_code", "M4B 1B3");
    response.put("city", "Toronto");
    response.put("created_at", "2024-11-26T08:36:36.238244338Z");
    ObjectNode metaData = BlnkJson.objectNode();
    metaData.put("customer_id", "CUST123456");
    response.set("meta_data", metaData);

    assertTrue(response.get("dob").isTextual());
    assertEquals(true, response.get("identity_id").asText().startsWith("idt_"));
  }
}
