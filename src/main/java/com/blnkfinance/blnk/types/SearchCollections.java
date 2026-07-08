package com.blnkfinance.blnk.types;

/**
 * Collection names supported by {@code Search.search} / {@code Search.filter}.
 *
 * <p>Deliberately String constants and NOT an enum, so invalid collection
 * names can still be constructed and exercised against the validator.
 */
public final class SearchCollections {

  private SearchCollections() {}

  public static final String LEDGERS = "ledgers";
  public static final String TRANSACTIONS = "transactions";
  public static final String BALANCES = "balances";
  public static final String IDENTITIES = "identities";
}
