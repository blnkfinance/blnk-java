package com.blnkfinance.blnk;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The subset of an HTTP response the SDK reads. Mock transports can provide a
 * text body, a JSON-only body (which exercises the {@code json()} fallback in
 * {@code readResponseJsonBody}), both, or neither. Suppliers may throw
 * unchecked exceptions (e.g. a malformed-body parse failure).
 */
public final class TransportResponse {

  private final boolean ok;
  private final int status;
  private final String statusText;
  private final Map<String, String> headers;
  private final Supplier<String> textSupplier;
  private final Supplier<JsonNode> jsonSupplier;

  private TransportResponse(Builder builder) {
    this.ok = builder.ok;
    this.status = builder.status;
    this.statusText = builder.statusText;
    this.headers = builder.headers;
    this.textSupplier = builder.textSupplier;
    this.jsonSupplier = builder.jsonSupplier;
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean ok() {
    return ok;
  }

  public int status() {
    return status;
  }

  public String statusText() {
    return statusText;
  }

  public Map<String, String> headers() {
    return headers;
  }

  /** True when the response exposes a {@code text()} body (checked FIRST). */
  public boolean hasText() {
    return textSupplier != null;
  }

  public String text() {
    return textSupplier.get();
  }

  /** True when the response exposes a {@code json()} body (fallback). */
  public boolean hasJson() {
    return jsonSupplier != null;
  }

  public JsonNode json() {
    return jsonSupplier.get();
  }

  public static final class Builder {
    private boolean ok;
    private int status;
    private String statusText = "";
    private Map<String, String> headers = new LinkedHashMap<>();
    private Supplier<String> textSupplier;
    private Supplier<JsonNode> jsonSupplier;

    public Builder ok(boolean ok) {
      this.ok = ok;
      return this;
    }

    public Builder status(int status) {
      this.status = status;
      return this;
    }

    public Builder statusText(String statusText) {
      this.statusText = statusText;
      return this;
    }

    public Builder headers(Map<String, String> headers) {
      this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
      return this;
    }

    public Builder text(Supplier<String> textSupplier) {
      this.textSupplier = textSupplier;
      return this;
    }

    public Builder json(Supplier<JsonNode> jsonSupplier) {
      this.jsonSupplier = jsonSupplier;
      return this;
    }

    public TransportResponse build() {
      return new TransportResponse(this);
    }
  }
}
