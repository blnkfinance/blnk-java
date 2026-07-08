package com.blnkfinance.blnk;

/**
 * Client configuration: a required {@code baseUrl} plus optional
 * {@code timeout}, {@code retryCount}, {@code retryDelayMs}, and
 * {@code logger}. Immutable — normalization (the trailing slash on baseUrl,
 * logger injection by {@code Blnk.init}, retry defaults) is never applied to
 * this object; the normalized values live on the constructed {@link Blnk}.
 *
 * <p>{@code retryCount}/{@code retryDelayMs}/{@code timeout} are boxed doubles
 * so out-of-range inputs (NaN, ±Infinity, fractional counts such as 2.9) stay
 * representable and are normalized by a single well-defined policy.
 */
public final class BlnkClientOptions {

  private final String baseUrl;
  private final Double timeout;
  private final Double retryCount;
  private final Double retryDelayMs;
  private final BlnkLogger logger;

  private BlnkClientOptions(Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.timeout = builder.timeout;
    this.retryCount = builder.retryCount;
    this.retryDelayMs = builder.retryDelayMs;
    this.logger = builder.logger;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String baseUrl() {
    return baseUrl;
  }

  public Double timeout() {
    return timeout;
  }

  public Double retryCount() {
    return retryCount;
  }

  public Double retryDelayMs() {
    return retryDelayMs;
  }

  public BlnkLogger logger() {
    return logger;
  }

  /** Copy with a logger set (used by {@code Blnk.init} when logger is absent). */
  public BlnkClientOptions withLogger(BlnkLogger logger) {
    Builder builder = new Builder();
    builder.baseUrl = this.baseUrl;
    builder.timeout = this.timeout;
    builder.retryCount = this.retryCount;
    builder.retryDelayMs = this.retryDelayMs;
    builder.logger = logger;
    return new BlnkClientOptions(builder);
  }

  public static final class Builder {
    private String baseUrl;
    private Double timeout;
    private Double retryCount;
    private Double retryDelayMs;
    private BlnkLogger logger;

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder timeout(double timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder retryCount(double retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    public Builder retryDelayMs(double retryDelayMs) {
      this.retryDelayMs = retryDelayMs;
      return this;
    }

    public Builder logger(BlnkLogger logger) {
      this.logger = logger;
      return this;
    }

    public BlnkClientOptions build() {
      return new BlnkClientOptions(this);
    }
  }
}
