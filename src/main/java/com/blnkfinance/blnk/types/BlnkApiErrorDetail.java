package com.blnkfinance.blnk.types;

/**
 * Structured error detail extracted from a Blnk API error body: an error
 * {@code code}, a human-readable {@code message}, and optional extra
 * {@code details}.
 *
 * <p>{@code details} is {@code null} when the error body carried none.
 */
public record BlnkApiErrorDetail(String code, String message, Object details) {

  public BlnkApiErrorDetail(String code, String message) {
    this(code, message, null);
  }
}
