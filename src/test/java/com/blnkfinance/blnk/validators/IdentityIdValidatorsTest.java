package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for identity id validation in {@link IdentityValidators}. */
class IdentityIdValidatorsTest {

  @Test
  @DisplayName("ValidateIdentityId")
  void validateIdentityId() {
    // The id is only checked for presence — any non-empty string is accepted.
    assertNull(IdentityValidators.validateIdentityId("idt_test_123"));
    assertEquals("identity id is required", IdentityValidators.validateIdentityId(""));
  }
}
