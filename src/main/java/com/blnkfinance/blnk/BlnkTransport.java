package com.blnkfinance.blnk;

/**
 * The HTTP transport seam. The default implementation is
 * {@link HttpTransport} (java.net.http); tests inject lambdas.
 *
 * <p>Failure contract (all unchecked):
 * <ul>
 *   <li>throw {@link BlnkAbortException} for timeouts/aborts (→ synthetic 408,
 *       never retried);</li>
 *   <li>throw {@link BlnkTransportException} (or any other RuntimeException)
 *       for network-level failures (→ retryable for GET, otherwise reported as
 *       a 500);</li>
 *   <li>return a {@link TransportResponse} for any HTTP response, ok or not.</li>
 * </ul>
 */
@FunctionalInterface
public interface BlnkTransport {

  TransportResponse execute(String url, TransportRequest request);
}
