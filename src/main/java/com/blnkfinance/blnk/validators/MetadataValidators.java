package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.StringUtils;

import java.util.Map;

/**
 * Validators for the metadata endpoints. Each validator returns {@code null}
 * when the input is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class MetadataValidators {

  private MetadataValidators() {}

  /**
   * Validates the id and payload for {@code Metadata.update}.
   *
   * <p>The {@code meta_data} check delegates to the shared
   * {@link LedgerBalanceValidators#isValidMetaData}: lists pass, while an
   * explicit null fails.
   */
  public static String validateUpdateMetadataData(String id, Map<String, Object> data) {
    // Only a null id or the exact empty string fails — whitespace-only ids
    // pass.
    if (!StringUtils.isValidString(id) || id.isEmpty()) {
      return "id is required";
    }

    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type UpdateMetadataData";
    }

    // meta_data is required here: an absent key fails, as does a present
    // null/string/number/boolean value.
    if (!data.containsKey("meta_data")
        || !LedgerBalanceValidators.isValidMetaData(data.get("meta_data"))) {
      return "meta_data must be a valid object";
    }

    return null;
  }
}
