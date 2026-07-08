package com.blnkfinance.blnk.testsupport;

import com.blnkfinance.blnk.BlnkTransport;
import com.blnkfinance.blnk.TransportRequest;
import com.blnkfinance.blnk.TransportResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link BlnkTransport} and records every {@code (url, request)} pair
 * before delegating to the wrapped transport.
 */
public final class CapturingTransport implements BlnkTransport {

  public record Call(String url, TransportRequest request) {}

  public final List<Call> calls = new ArrayList<>();
  private final BlnkTransport delegate;

  private CapturingTransport(BlnkTransport delegate) {
    this.delegate = delegate;
  }

  public static CapturingTransport of(BlnkTransport delegate) {
    return new CapturingTransport(delegate);
  }

  @Override
  public TransportResponse execute(String url, TransportRequest request) {
    calls.add(new Call(url, request));
    return delegate.execute(url, request);
  }
}
