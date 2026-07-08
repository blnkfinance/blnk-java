package com.blnkfinance.blnk.types;

/**
 * The seven Core tokenization field names.
 *
 * <p>These are PascalCase Core field names, NOT the snake_case
 * {@code IdentityData} JSON keys ({@code first_name}, …) — Core rejects
 * snake_case with "field is not tokenizable". They are plain String constants
 * (no enum): the validators deliberately do NOT restrict field names to this
 * list, so invalid/empty strings stay representable and flow through to the
 * API unchanged.
 */
public final class TokenizableIdentityFields {

  private TokenizableIdentityFields() {}

  public static final String FIRST_NAME = "FirstName";
  public static final String LAST_NAME = "LastName";
  public static final String OTHER_NAMES = "OtherNames";
  public static final String EMAIL_ADDRESS = "EmailAddress";
  public static final String PHONE_NUMBER = "PhoneNumber";
  public static final String STREET = "Street";
  public static final String POST_CODE = "PostCode";
}
