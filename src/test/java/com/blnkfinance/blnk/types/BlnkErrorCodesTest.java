package com.blnkfinance.blnk.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BlnkErrorCodes}: the constant set must mirror Core's
 * {@code internal/apierror/codes.go} (0.15.4) and stay internally consistent.
 */
class BlnkErrorCodesTest {

  private static final Set<String> KNOWN_PREFIXES = Set.of(
      "GEN", "AUTH", "APIKEY", "TXN", "BAL", "LGR", "ACC", "IDT", "RECON",
      "META", "HOOK", "QUEUE", "SRCH", "ADMIN");

  @Test
  @DisplayName("every constant's value equals its name, is unique, and uses a Core prefix")
  void constantsAreSelfNamedUniqueAndPrefixed() throws IllegalAccessException {
    Set<String> seen = new HashSet<>();
    int count = 0;
    for (Field field : BlnkErrorCodes.class.getDeclaredFields()) {
      int mods = field.getModifiers();
      if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods) || field.getType() != String.class) {
        continue;
      }
      count++;
      String value = (String) field.get(null);
      assertEquals(field.getName(), value, "constant name and value must match");
      assertTrue(seen.add(value), "duplicate error code " + value);
      String prefix = value.substring(0, value.indexOf('_'));
      assertTrue(KNOWN_PREFIXES.contains(prefix), "unknown Core prefix on " + value);
    }
    assertEquals(79, count, "expected the full Core 0.15.4 catalogue");
  }

  @Test
  @DisplayName("Core 0.15.4 codes are present")
  void core0154CodesArePresent() {
    assertEquals("TXN_ALREADY_REFUNDED", BlnkErrorCodes.TXN_ALREADY_REFUNDED);
    assertEquals("BAL_NOT_FOUND", BlnkErrorCodes.BAL_NOT_FOUND);
    assertEquals("TXN_VALIDATION_ERROR", BlnkErrorCodes.TXN_VALIDATION_ERROR);
    assertEquals("GEN_CONFLICT", BlnkErrorCodes.GEN_CONFLICT);
  }

  @Test
  @DisplayName("pre-1.5.0 constants are unchanged")
  void legacyConstantsUnchanged() {
    assertEquals("TXN_INVALID_AMOUNT", BlnkErrorCodes.TXN_INVALID_AMOUNT);
    assertEquals("GEN_CONFLICT", BlnkErrorCodes.GEN_CONFLICT);
    assertEquals("TXN_VALIDATION_ERROR", BlnkErrorCodes.TXN_VALIDATION_ERROR);
  }
}
