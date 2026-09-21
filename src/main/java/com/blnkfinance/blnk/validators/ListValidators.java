package com.blnkfinance.blnk.validators;

import java.util.Map;

/**
 * Validates {@code ListOptions}. Same rules Core applies ({@code limit >= 1},
 * {@code offset >= 0}), checked before the request is sent.
 */
public final class ListValidators {

  private ListValidators() {}

  /** Returns {@code null} when valid, otherwise the failure message. */
  public static String validateListOptions(Map<String, Object> data) {
    if (data == null) {
      return "Data must be a valid object of type ListOptions";
    }

    if (data.containsKey("limit")) {
      if (!(data.get("limit") instanceof Integer limit)) {
        return "limit must be an integer if provided";
      }
      if (limit < 1) {
        return "limit must be at least 1";
      }
    }

    if (data.containsKey("offset")) {
      if (!(data.get("offset") instanceof Integer offset)) {
        return "offset must be an integer if provided";
      }
      if (offset < 0) {
        return "offset must be at least 0";
      }
    }

    return null;
  }
}
