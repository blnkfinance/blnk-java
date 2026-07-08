package com.blnkfinance.blnk.validators;

import com.blnkfinance.blnk.util.StringUtils;

import java.util.Map;

/**
 * Validators for the ledger endpoints. Each validator receives the request
 * payload as a raw {@code Map<String,Object>} and returns {@code null} when
 * the payload is valid, otherwise the exact failure message surfaced to
 * callers. The first failing check wins, and validators never throw.
 */
public final class LedgerValidators {

  private LedgerValidators() {}

  /**
   * Validates the payload for creating a ledger. Note: the null-payload
   * message names {@code CreateLedgerBalance}; callers depend on this exact
   * string.
   */
  public static String validateCreateLedger(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type CreateLedgerBalance";
    }

    // Type check only — an empty string passes.
    if (!StringUtils.isValidString(data.get("name"))) {
      return "name field must be a valid string";
    }

    // An absent key skips the check; a key present with a null value fails it,
    // while lists pass.
    if (data.containsKey("meta_data")
        && !LedgerBalanceValidators.isValidMetaData(data.get("meta_data"))) {
      return "meta_data must be a valid object if provided";
    }

    return null;
  }

  /** Validates the payload for renaming a ledger. */
  public static String validateUpdateLedger(Map<String, Object> data) {
    // A null payload is rejected before any field checks.
    if (data == null) {
      return "Data must be a valid object of type UpdateLedger";
    }

    if (!StringUtils.isValidString(data.get("name"))) {
      return "name field must be a valid string";
    }

    // Unicode-aware trim: a whitespace-only name is rejected.
    if (((String) data.get("name")).strip().isEmpty()) {
      return "name field is required";
    }

    return null;
  }
}
