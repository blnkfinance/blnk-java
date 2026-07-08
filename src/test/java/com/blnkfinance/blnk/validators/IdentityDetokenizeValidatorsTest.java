package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for detokenize-identity payload validation in {@link IdentityValidators}. */
class IdentityDetokenizeValidatorsTest {

  @Test
  @DisplayName("ValidateDetokenizeIdentityData")
  void validateDetokenizeIdentityData() {
    Map<String, Object> validData = new LinkedHashMap<>();
    validData.put("fields", List.of("FirstName", "EmailAddress"));

    assertNull(IdentityValidators.validateDetokenizeIdentityData("idt_test_123", validData));

    // Empty fields array is allowed (detokenize-all semantics).
    Map<String, Object> emptyFields = new LinkedHashMap<>();
    emptyFields.put("fields", List.of());
    assertNull(IdentityValidators.validateDetokenizeIdentityData("idt_test_123", emptyFields));

    assertEquals(
        "identity id is required",
        IdentityValidators.validateDetokenizeIdentityData("", validData));

    Map<String, Object> blankField = new LinkedHashMap<>();
    blankField.put("fields", List.of("FirstName", ""));
    assertEquals(
        "each field must be a non-empty string",
        IdentityValidators.validateDetokenizeIdentityData("idt_test_123", blankField));
  }
}
