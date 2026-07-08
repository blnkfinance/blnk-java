package com.blnkfinance.blnk.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for metadata-update payload validation in {@link MetadataValidators}. */
@DisplayName("ValidateUpdateMetadataData")
class MetadataValidatorsTest {

  /** A valid payload: {@code meta_data} containing a single entry. */
  private static final Map<String, Object> validData = buildValidData();

  private static Map<String, Object> buildValidData() {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("project_owner", "Acme LLC");
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("meta_data", meta);
    return data;
  }

  @Test
  @DisplayName("accepts valid payload")
  void acceptsValidPayload() {
    assertNull(MetadataValidators.validateUpdateMetadataData("ldg_123", validData));
  }

  @Test
  @DisplayName("rejects empty id")
  void rejectsEmptyId() {
    assertEquals(
        "id is required", MetadataValidators.validateUpdateMetadataData("", validData));
  }

  @Test
  @DisplayName("rejects missing meta_data")
  void rejectsMissingMetaData() {
    assertEquals(
        "meta_data must be a valid object",
        MetadataValidators.validateUpdateMetadataData("ldg_123", new LinkedHashMap<>()));
  }

  @Test
  @DisplayName("rejects non-object meta_data")
  void rejectsNonObjectMetaData() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("meta_data", "invalid");
    assertEquals(
        "meta_data must be a valid object",
        MetadataValidators.validateUpdateMetadataData("ldg_123", data));
  }
}
