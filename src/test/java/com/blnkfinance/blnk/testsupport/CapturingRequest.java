package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.BlnkRequest;
import com.blnkfinance.blnk.types.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wraps a {@link BlnkRequest} and records every
 * {@code (endpoint, data, method, headers)} tuple before delegating. Service
 * test suites assert on the recorded {@code calls}.
 */
public final class CapturingRequest implements BlnkRequest {

  public record Call(String endpoint, Object data, String method, Map<String, String> headers) {}

  public final List<Call> calls = new ArrayList<>();
  private final BlnkRequest delegate;

  private CapturingRequest(BlnkRequest delegate) {
    this.delegate = delegate;
  }

  public static CapturingRequest of(BlnkRequest delegate) {
    return new CapturingRequest(delegate);
  }

  @Override
  public ApiResponse<JsonNode> call(
      String endpoint, Object data, String method, Map<String, String> headerOptions) {
    calls.add(new Call(endpoint, data, method, headerOptions));
    return delegate.call(endpoint, data, method, headerOptions);
  }
}
