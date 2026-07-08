package com.blnkfinance.blnk.types;

/**
 * Standard envelope returned by every endpoint method: the HTTP status, a
 * message, the parsed payload, and an optional structured error detail.
 *
 * <p>The {@code error} field is {@code null} whenever no structured error
 * detail accompanied the response; Jackson's NON_NULL inclusion keeps it out
 * of any serialized form.
 */
public record ApiResponse<T>(int status, String message, T data, BlnkApiErrorDetail error) {

  public ApiResponse(int status, String message, T data) {
    this(status, message, data, null);
  }
}
