package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for tokenize-identity payload validation in {@link IdentityValidators}. */
class IdentityTokenizeValidatorsTest {

  @Test
  @DisplayName("ValidateTokenizeIdentityData")
  void validateTokenizeIdentityData() {
    Map<String, Object> validData = new LinkedHashMap<>();
    validData.put("fields", List.of("FirstName", "EmailAddress"));

    assertNull(IdentityValidators.validateTokenizeIdentityData("idt_test_123", validData));
    assertEquals(
        "identity id is required",
        IdentityValidators.validateTokenizeIdentityData("", validData));

    Map<String, Object> emptyFields = new LinkedHashMap<>();
    emptyFields.put("fields", List.of());
    assertEquals(
        "at least one field must be specified",
        IdentityValidators.validateTokenizeIdentityData("idt_test_123", emptyFields));

    Map<String, Object> blankField = new LinkedHashMap<>();
    blankField.put("fields", List.of("FirstName", ""));
    assertEquals(
        "each field must be a non-empty string",
        IdentityValidators.validateTokenizeIdentityData("idt_test_123", blankField));
  }
}
