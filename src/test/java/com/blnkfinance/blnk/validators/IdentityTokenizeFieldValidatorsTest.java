package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for single-field tokenize validation in {@link IdentityValidators}. */
class IdentityTokenizeFieldValidatorsTest {

  @Test
  @DisplayName("ValidateTokenizeIdentityField")
  void validateTokenizeIdentityField() {
    assertNull(IdentityValidators.validateTokenizeIdentityField("idt_test_123", "FirstName"));
    assertEquals(
        "identity id is required",
        IdentityValidators.validateTokenizeIdentityField("", "FirstName"));
    assertEquals(
        "field name is required",
        IdentityValidators.validateTokenizeIdentityField("idt_test_123", ""));
  }
}
