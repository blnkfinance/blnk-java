package com.blnkfinance.blnk.types;

/**
 * Hook type values, exposed as plain String constants (NOT an enum) so
 * invalid values can still be constructed and exercised against the
 * validator.
 */
public final class HookType {

  private HookType() {}

  public static final String PRE_TRANSACTION = "PRE_TRANSACTION";
  public static final String POST_TRANSACTION = "POST_TRANSACTION";
}
