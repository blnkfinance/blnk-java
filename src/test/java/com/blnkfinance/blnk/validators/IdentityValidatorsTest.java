package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for identity payload validation in {@link IdentityValidators}. */
class IdentityValidatorsTest {

  /** Builds a fully populated individual identity payload shared across cases. */
  private static Map<String, Object> baseIndividual() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("identity_type", "individual");
    data.put("first_name", "Jane");
    data.put("last_name", "Doe");
    data.put("gender", "female");
    data.put("dob", "1990-01-15T00:00:00Z");
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

  @Nested
  @DisplayName("ValidateIdentity identity_id and dob")
  class ValidateIdentityIdentityIdAndDob {

    @Test
    @DisplayName("accepts caller-supplied identity_id")
    void acceptsCallerSuppliedIdentityId() {
      Map<String, Object> data = baseIndividual();
      data.put("identity_id", "idt_11111111-1111-4111-8111-111111111111");
      assertNull(IdentityValidators.validateIdentity(data));
    }

    @Test
    @DisplayName("accepts ISO dob string")
    void acceptsIsoDobString() {
      assertNull(IdentityValidators.validateIdentity(baseIndividual()));
    }

    @Test
    @DisplayName("accepts Date dob")
    void acceptsDateDob() {
      Map<String, Object> data = baseIndividual();
      data.put("dob", Date.from(Instant.parse("1990-01-15T00:00:00Z")));
      assertNull(IdentityValidators.validateIdentity(data));
    }

    @Test
    @DisplayName("rejects invalid identity_id")
    void rejectsInvalidIdentityId() {
      Map<String, Object> data = baseIndividual();
      data.put("identity_id", "user_123");
      assertEquals(
          "identity_id must start with idt_ followed by a valid UUID",
          IdentityValidators.validateIdentity(data));
    }

    @Test
    @DisplayName("rejects invalid dob string")
    void rejectsInvalidDobString() {
      Map<String, Object> data = baseIndividual();
      data.put("dob", "not-a-date");
      assertEquals(
          "dob must be a valid ISO 8601 date string or Date",
          IdentityValidators.validateIdentity(data));
    }
  }

  @Nested
  @DisplayName("ValidateIdentity optional fields")
  class ValidateIdentityOptionalFields {

    @Test
    @DisplayName("accepts minimal individual payload")
    void acceptsMinimalIndividualPayload() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("identity_type", "individual");
      assertNull(IdentityValidators.validateIdentity(data));
    }

    @Test
    @DisplayName("accepts minimal organization payload")
    void acceptsMinimalOrganizationPayload() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("identity_type", "organization");
      assertNull(IdentityValidators.validateIdentity(data));
    }

    @Test
    @DisplayName("rejects invalid identity_type")
    void rejectsInvalidIdentityType() {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("identity_type", "business");
      assertEquals(
          "identity_type must be individual or organization",
          IdentityValidators.validateIdentity(data));
    }

    @Test
    @DisplayName("rejects invalid gender when provided")
    void rejectsInvalidGenderWhenProvided() {
      Map<String, Object> data = baseIndividual();
      data.put("gender", "unknown");
      assertEquals(
          "gender must be male, female, or other if provided",
          IdentityValidators.validateIdentity(data));
    }
  }
}
